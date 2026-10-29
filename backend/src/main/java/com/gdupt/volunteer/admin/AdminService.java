package com.gdupt.volunteer.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gdupt.volunteer.admin.dto.AnnouncementRequest;
import com.gdupt.volunteer.admin.dto.TypeRequest;
import com.gdupt.volunteer.admin.dto.UpdateUserRolesRequest;
import com.gdupt.volunteer.admin.dto.UpdateUserStatusRequest;
import com.gdupt.volunteer.admin.dto.UserAdminResponse;
import com.gdupt.volunteer.common.BusinessException;
import com.gdupt.volunteer.common.ErrorCode;
import com.gdupt.volunteer.common.OperationLogService;
import com.gdupt.volunteer.common.PageResult;
import com.gdupt.volunteer.entity.Activity;
import com.gdupt.volunteer.entity.ActivityType;
import com.gdupt.volunteer.entity.Announcement;
import com.gdupt.volunteer.entity.OperationLog;
import com.gdupt.volunteer.entity.Role;
import com.gdupt.volunteer.entity.User;
import com.gdupt.volunteer.entity.UserRole;
import com.gdupt.volunteer.mapper.ActivityMapper;
import com.gdupt.volunteer.mapper.ActivityTypeMapper;
import com.gdupt.volunteer.mapper.AnnouncementMapper;
import com.gdupt.volunteer.mapper.OperationLogMapper;
import com.gdupt.volunteer.mapper.RoleMapper;
import com.gdupt.volunteer.mapper.UserMapper;
import com.gdupt.volunteer.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统管理（仅 ADMIN）：用户管理（列表/启停/角色）、公告管理、活动类型管理、操作日志查询。
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final AnnouncementMapper announcementMapper;
    private final ActivityTypeMapper activityTypeMapper;
    private final ActivityMapper activityMapper;
    private final OperationLogMapper operationLogMapper;
    private final OperationLogService operationLogService;

    // ---------- 用户管理 ----------

    public PageResult<UserAdminResponse> userList(String keyword, int page, int size) {
        page = Math.max(1, page);
        size = Math.min(100, Math.max(1, size));
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getId);
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getRealName, keyword)
                    .or().like(User::getStudentNo, keyword));
        }
        Page<User> p = userMapper.selectPage(new Page<>(page, size), qw);
        Map<Long, List<String>> rolesByUser = new HashMap<>();
        Map<Long, List<Long>> roleIdsByUser = new HashMap<>();
        for (User u : p.getRecords()) {
            rolesByUser.put(u.getId(), rolesOf(u.getId()));
            roleIdsByUser.put(u.getId(), roleIdsOf(u.getId()));
        }
        List<UserAdminResponse> items = new ArrayList<>(p.getRecords().size());
        for (User u : p.getRecords()) {
            items.add(UserAdminResponse.builder()
                    .id(u.getId())
                    .username(u.getUsername())
                    .realName(u.getRealName())
                    .studentNo(u.getStudentNo())
                    .phone(u.getPhone())
                    .status(u.getStatus())
                    .roles(rolesByUser.getOrDefault(u.getId(), Collections.emptyList()))
                    .roleIds(roleIdsByUser.getOrDefault(u.getId(), Collections.emptyList()))
                    .createdAt(u.getCreatedAt())
                    .build());
        }
        return PageResult.of(items, p.getTotal(), page, size);
    }

    @Transactional
    public void updateStatus(Long userId, UpdateUserStatusRequest req, Long operatorId, String ip) {
        User u = requireUser(userId);
        if (req.getStatus() == null || (req.getStatus() != 0 && req.getStatus() != 1)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }
        if (userId.equals(operatorId)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "不能停用当前登录的管理员账号");
        }
        User patch = new User();
        patch.setId(userId);
        patch.setStatus(req.getStatus());
        patch.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(patch);
        operationLogService.record(operatorId, "user", "UPDATE_STATUS", "USER", userId, ip,
                "status=" + req.getStatus() + ",user=" + u.getUsername());
    }

    @Transactional
    public void updateRoles(Long userId, UpdateUserRolesRequest req, Long operatorId, String ip) {
        requireUser(userId);
        List<Long> roleIds = req.getRoleIds();
        List<Role> roles = roleMapper.selectBatchIds(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        for (Long rid : roleIds) {
            UserRole link = new UserRole();
            link.setUserId(userId);
            link.setRoleId(rid);
            userRoleMapper.insert(link);
        }
        operationLogService.record(operatorId, "user", "UPDATE_ROLES", "USER", userId, ip,
                "roles=" + roles.stream().map(Role::getCode).toList());
    }

    // ---------- 公告管理 ----------

    public PageResult<Announcement> announcementList(Integer status, int page, int size) {
        page = Math.max(1, page);
        size = Math.min(100, Math.max(1, size));
        LambdaQueryWrapper<Announcement> qw = new LambdaQueryWrapper<Announcement>()
                .orderByDesc(Announcement::getIsTop)
                .orderByDesc(Announcement::getId);
        if (status != null) {
            qw.eq(Announcement::getStatus, status);
        }
        Page<Announcement> p = announcementMapper.selectPage(new Page<>(page, size), qw);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    @Transactional
    public Long createAnnouncement(AnnouncementRequest req, Long operatorId) {
        Announcement a = new Announcement();
        a.setTitle(req.getTitle());
        a.setContent(req.getContent());
        a.setIsTop(0);
        a.setStatus(0);
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        announcementMapper.insert(a);
        return a.getId();
    }

    @Transactional
    public void updateAnnouncement(Long id, AnnouncementRequest req) {
        requireAnnouncement(id);
        Announcement patch = new Announcement();
        patch.setId(id);
        patch.setTitle(req.getTitle());
        patch.setContent(req.getContent());
        patch.setUpdatedAt(LocalDateTime.now());
        announcementMapper.updateById(patch);
    }

    @Transactional
    public void publishAnnouncement(Long id, boolean publish) {
        requireAnnouncement(id);
        Announcement patch = new Announcement();
        patch.setId(id);
        patch.setStatus(publish ? 1 : 0);
        patch.setPublishTime(publish ? LocalDateTime.now() : null);
        patch.setUpdatedAt(LocalDateTime.now());
        announcementMapper.updateById(patch);
    }

    @Transactional
    public void deleteAnnouncement(Long id) {
        requireAnnouncement(id);
        announcementMapper.deleteById(id);
    }

    // ---------- 活动类型管理 ----------

    public List<ActivityType> typeList() {
        return activityTypeMapper.selectList(new LambdaQueryWrapper<ActivityType>()
                .orderByAsc(ActivityType::getSort).orderByAsc(ActivityType::getId));
    }

    @Transactional
    public Long createType(TypeRequest req) {
        ActivityType t = new ActivityType();
        t.setName(req.getName());
        t.setStatus(1);
        t.setSort(req.getSortOrder() == null ? 999 : req.getSortOrder());
        activityTypeMapper.insert(t);
        return t.getId();
    }

    @Transactional
    public void updateType(Long id, TypeRequest req) {
        if (activityTypeMapper.selectById(id) == null) {
            throw new BusinessException(ErrorCode.TYPE_IN_USE, "活动类型不存在");
        }
        ActivityType patch = new ActivityType();
        patch.setId(id);
        patch.setName(req.getName());
        if (req.getSortOrder() != null) {
            patch.setSort(req.getSortOrder());
        }
        activityTypeMapper.updateById(patch);
    }

    @Transactional
    public void deleteType(Long id) {
        if (activityTypeMapper.selectById(id) == null) {
            throw new BusinessException(ErrorCode.TYPE_IN_USE, "活动类型不存在");
        }
        Long used = activityMapper.selectCount(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getTypeId, id));
        if (used != null && used > 0) {
            throw new BusinessException(ErrorCode.TYPE_IN_USE);
        }
        activityTypeMapper.deleteById(id);
    }

    // ---------- 操作日志 ----------

    /** 全部角色列表（管理端角色下拉） */
    public List<Role> roleList() {
        return roleMapper.selectList(new LambdaQueryWrapper<Role>().orderByAsc(Role::getId));
    }

    public PageResult<OperationLog> logList(Long userId, String module, int page, int size) {        page = Math.max(1, page);
        size = Math.min(100, Math.max(1, size));
        LambdaQueryWrapper<OperationLog> qw = new LambdaQueryWrapper<OperationLog>()
                .orderByDesc(OperationLog::getId);
        if (userId != null) {
            qw.eq(OperationLog::getUserId, userId);
        }
        if (StringUtils.hasText(module)) {
            qw.eq(OperationLog::getModule, module);
        }
        Page<OperationLog> p = operationLogMapper.selectPage(new Page<>(page, size), qw);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    // ---------- 私有 ----------

    private User requireUser(Long id) {
        User u = userMapper.selectById(id);
        if (u == null || u.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return u;
    }

    private Announcement requireAnnouncement(Long id) {
        Announcement a = announcementMapper.selectById(id);
        if (a == null) {
            throw new BusinessException(ErrorCode.ANNOUNCEMENT_NOT_FOUND);
        }
        return a;
    }

    private List<String> rolesOf(Long userId) {
        List<UserRole> links = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (links.isEmpty()) {
            return Collections.emptyList();
        }
        return roleMapper.selectBatchIds(links.stream().map(UserRole::getRoleId).toList())
                .stream().map(Role::getCode).toList();
    }

    private List<Long> roleIdsOf(Long userId) {
        List<UserRole> links = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (links.isEmpty()) {
            return Collections.emptyList();
        }
        return links.stream().map(UserRole::getRoleId).toList();
    }
}
