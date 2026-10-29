package com.gdupt.volunteer.profile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gdupt.volunteer.common.BusinessException;
import com.gdupt.volunteer.common.ErrorCode;
import com.gdupt.volunteer.common.PageResult;
import com.gdupt.volunteer.entity.Role;
import com.gdupt.volunteer.entity.ServiceHours;
import com.gdupt.volunteer.entity.User;
import com.gdupt.volunteer.entity.UserRole;
import com.gdupt.volunteer.mapper.RoleMapper;
import com.gdupt.volunteer.mapper.ServiceHoursMapper;
import com.gdupt.volunteer.mapper.UserMapper;
import com.gdupt.volunteer.mapper.UserRoleMapper;
import com.gdupt.volunteer.profile.dto.MyProfileResponse;
import com.gdupt.volunteer.profile.dto.UpdateProfileRequest;
import com.gdupt.volunteer.profile.dto.VolunteerListItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 志愿者档案：个人服务汇总（总工时/服务次数/参与活动数）、星级等级、资料维护。
 * 等级规则（总工时）：<10h 一星；<50h 二星；<100h 三星；>=100h 四星。
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final BigDecimal L1 = new BigDecimal("10");
    private static final BigDecimal L2 = new BigDecimal("50");
    private static final BigDecimal L3 = new BigDecimal("100");
    private static final String[] LEVEL_NAMES = {"一星志愿者", "二星志愿者", "三星志愿者", "四星志愿者"};

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final ServiceHoursMapper serviceHoursMapper;

    /** 我的档案 */
    public MyProfileResponse myProfile(Long userId) {
        return buildProfile(userMapper.selectById(userId));
    }

    /** 志愿者列表（含汇总），ADMIN/ORGANIZER 使用 */
    public PageResult<VolunteerListItem> volunteerList(String keyword, int page, int size) {
        page = Math.max(1, page);
        size = Math.min(100, Math.max(1, size));
        List<Long> volunteerIds = volunteerUserIds();
        if (volunteerIds.isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, page, size);
        }
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<User>()
                .in(User::getId, volunteerIds)
                .orderByDesc(User::getId);
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getRealName, keyword)
                    .or().like(User::getStudentNo, keyword));
        }
        Page<User> p = userMapper.selectPage(new Page<>(page, size), qw);
        List<VolunteerListItem> items = new ArrayList<>(p.getRecords().size());
        for (User u : p.getRecords()) {
            Summary s = summarize(u.getId());
            int level = levelOf(s.totalHours);
            items.add(VolunteerListItem.builder()
                    .id(u.getId())
                    .username(u.getUsername())
                    .realName(u.getRealName())
                    .studentNo(u.getStudentNo())
                    .phone(u.getPhone())
                    .status(u.getStatus())
                    .totalHours(s.totalHours)
                    .serviceCount(s.serviceCount)
                    .level(level)
                    .levelName(LEVEL_NAMES[level - 1])
                    .createdAt(u.getCreatedAt())
                    .build());
        }
        return PageResult.of(items, p.getTotal(), page, size);
    }

    /** 更新我的资料：仅更新非空字段 */
    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequest req) {
        User u = userMapper.selectById(userId);
        if (u == null || u.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        User patch = new User();
        patch.setId(userId);
        if (StringUtils.hasText(req.getRealName())) {
            patch.setRealName(req.getRealName());
        }
        if (StringUtils.hasText(req.getPhone())) {
            patch.setPhone(req.getPhone());
        }
        if (StringUtils.hasText(req.getStudentNo())) {
            patch.setStudentNo(req.getStudentNo());
        }
        patch.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(patch);
    }

    // ---------- 私有 ----------

    private MyProfileResponse buildProfile(User u) {
        if (u == null || u.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        Summary s = summarize(u.getId());
        int level = levelOf(s.totalHours);
        return MyProfileResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .realName(u.getRealName())
                .studentNo(u.getStudentNo())
                .phone(u.getPhone())
                .roles(rolesOf(u.getId()))
                .totalHours(s.totalHours)
                .serviceCount(s.serviceCount)
                .activityCount(s.activityCount)
                .level(level)
                .levelName(LEVEL_NAMES[level - 1])
                .createdAt(u.getCreatedAt())
                .build();
    }

    private Summary summarize(Long userId) {
        List<ServiceHours> hoursList = serviceHoursMapper.selectList(new LambdaQueryWrapper<ServiceHours>()
                .eq(ServiceHours::getUserId, userId)
                .eq(ServiceHours::getStatus, "CONFIRMED"));
        BigDecimal total = BigDecimal.ZERO;
        Set<Long> activities = new java.util.HashSet<>();
        for (ServiceHours h : hoursList) {
            total = total.add(h.getHours() == null ? BigDecimal.ZERO : h.getHours());
            activities.add(h.getActivityId());
        }
        return new Summary(total, (long) hoursList.size(), (long) activities.size());
    }

    private int levelOf(BigDecimal hours) {
        if (hours.compareTo(L1) < 0) {
            return 1;
        }
        if (hours.compareTo(L2) < 0) {
            return 2;
        }
        if (hours.compareTo(L3) < 0) {
            return 3;
        }
        return 4;
    }

    private List<Long> volunteerUserIds() {
        List<Role> roles = roleMapper.selectList(new LambdaQueryWrapper<Role>().eq(Role::getCode, "VOLUNTEER"));
        if (roles.isEmpty()) {
            return Collections.emptyList();
        }
        List<UserRole> links = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getRoleId, roles.get(0).getId()));
        return links.stream().map(UserRole::getUserId).toList();
    }

    private List<String> rolesOf(Long userId) {
        List<UserRole> links = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (links.isEmpty()) {
            return Collections.emptyList();
        }
        return roleMapper.selectBatchIds(links.stream().map(UserRole::getRoleId).toList())
                .stream().map(Role::getCode).collect(Collectors.toList());
    }

    private record Summary(BigDecimal totalHours, Long serviceCount, Long activityCount) {
    }
}
