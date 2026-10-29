package com.gdupt.volunteer.attendance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gdupt.volunteer.attendance.dto.AttendanceAdminResponse;
import com.gdupt.volunteer.attendance.dto.AttendanceResponse;
import com.gdupt.volunteer.attendance.dto.HoursResponse;
import com.gdupt.volunteer.common.BusinessException;
import com.gdupt.volunteer.common.ErrorCode;
import com.gdupt.volunteer.common.OperationLogService;
import com.gdupt.volunteer.common.PageResult;
import com.gdupt.volunteer.entity.Activity;
import com.gdupt.volunteer.entity.Attendance;
import com.gdupt.volunteer.entity.Registration;
import com.gdupt.volunteer.entity.Role;
import com.gdupt.volunteer.entity.ServiceHours;
import com.gdupt.volunteer.entity.User;
import com.gdupt.volunteer.entity.UserRole;
import com.gdupt.volunteer.mapper.ActivityMapper;
import com.gdupt.volunteer.mapper.AttendanceMapper;
import com.gdupt.volunteer.mapper.RegistrationMapper;
import com.gdupt.volunteer.mapper.RoleMapper;
import com.gdupt.volunteer.mapper.ServiceHoursMapper;
import com.gdupt.volunteer.mapper.UserMapper;
import com.gdupt.volunteer.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 签到签退与工时核算模块：
 * - 签到窗口 [signin_start, signin_end]，签退窗口 [start_time, signout_end]，窗口外拒绝（防作弊）
 * - 一人一活动仅一条签到记录（uk_att_registration 唯一约束），重复签到/签退直接拒绝
 * - 签退后按工时规则核算并入账 service_hours（规则版本号 RULE_V1，口径可回溯）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    /** 工时核算规则版本号：门槛 15 分钟，按时长/60 精确到 2 位小数 */
    private static final String CALC_RULE_VERSION = "RULE_V1";
    private static final int MIN_SERVICE_MINUTES = 15;

    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final AttendanceMapper attendanceMapper;
    private final ServiceHoursMapper serviceHoursMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final OperationLogService operationLogService;

    /** 签到：本人签到或组织者代签（MANUAL）；窗口内且报名审核通过方可 */
    @Transactional
    public Long signin(Long activityId, Long operatorId, String method, Long targetUserId, String ip) {
        Activity activity = requireActivity(activityId);
        if ("CANCELLED".equals(activity.getStatus())) {
            throw new BusinessException(ErrorCode.ACTIVITY_STATUS_INVALID, "活动已取消，无法签到");
        }
        Long effectiveUser = targetUserId;
        String effectiveMethod = method == null ? "QR" : method;
        if (effectiveUser != null) {
            // 代签：仅组织者（该活动负责人）或管理员可为他人代签
            checkOwnerOrAdmin(activity, operatorId);
            effectiveMethod = "MANUAL";
        } else {
            effectiveUser = operatorId;
        }
        Registration reg = requireApprovedRegistration(activityId, effectiveUser);
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getSigninStart()) || now.isAfter(activity.getSigninEnd())) {
            throw new BusinessException(ErrorCode.OUTSIDE_SIGNIN_WINDOW,
                    "当前不在签到时间窗口内（" + activity.getSigninStart() + " ~ " + activity.getSigninEnd() + "）");
        }
        Attendance existing = attendanceMapper.selectOne(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getRegistrationId, reg.getId()));
        if (existing != null) {
            throw new BusinessException(ErrorCode.ALREADY_CHECKED_IN);
        }
        Attendance attendance = new Attendance();
        attendance.setRegistrationId(reg.getId());
        attendance.setSigninTime(now);
        attendance.setDurationMinutes(0);
        attendance.setSigninMethod(effectiveMethod);
        attendance.setAuditStatus("VALID");
        try {
            attendanceMapper.insert(attendance);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.ALREADY_CHECKED_IN);
        }
        operationLogService.record(operatorId, "attendance", "SIGNIN", "ACTIVITY", activityId, ip,
                "method=" + effectiveMethod + ",user=" + effectiveUser);
        return attendance.getId();
    }

    /** 签退：窗口内，已签到未签退，签退后立即核算工时入账 */
    @Transactional
    public void signout(Long activityId, Long userId, String ip) {
        Activity activity = requireActivity(activityId);
        Registration reg = requireApprovedRegistration(activityId, userId);
        Attendance attendance = attendanceMapper.selectOne(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getRegistrationId, reg.getId()));
        if (attendance == null) {
            throw new BusinessException(ErrorCode.NOT_CHECKED_IN);
        }
        if (attendance.getSignoutTime() != null) {
            throw new BusinessException(ErrorCode.ALREADY_CHECKED_OUT);
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime()) || now.isAfter(activity.getSignoutEnd())) {
            throw new BusinessException(ErrorCode.OUTSIDE_SIGNIN_WINDOW,
                    "当前不在签退时间窗口内（" + activity.getStartTime() + " ~ " + activity.getSignoutEnd() + "）");
        }
        int duration = (int) Math.max(0, Duration.between(attendance.getSigninTime(), now).toMinutes());
        Attendance patch = new Attendance();
        patch.setId(attendance.getId());
        patch.setSignoutTime(now);
        patch.setDurationMinutes(duration);
        attendanceMapper.updateById(patch);

        // 工时核算（规则 RULE_V1：门槛 15 分钟，四舍五入 2 位小数）
        BigDecimal hours = calcHours(duration);
        ServiceHours hoursRecord = new ServiceHours();
        hoursRecord.setUserId(userId);
        hoursRecord.setActivityId(activityId);
        hoursRecord.setAttendanceId(attendance.getId());
        hoursRecord.setHours(hours);
        hoursRecord.setCalcRuleVersion(CALC_RULE_VERSION);
        hoursRecord.setStatus("CONFIRMED");
        try {
            serviceHoursMapper.insert(hoursRecord);
        } catch (DuplicateKeyException e) {
            log.warn("活动 {} 报名者 {} 的工时流水已存在，跳过重复核算", activityId, userId);
        }
        operationLogService.record(userId, "hours", "CALC", "ACTIVITY", activityId, ip,
                "duration=" + duration + ",hours=" + hours + ",rule=" + CALC_RULE_VERSION);
    }

    /** 我的签到记录（含工时） */
    public PageResult<AttendanceResponse> myRecords(Long userId, int page, int size) {
        page = Math.max(1, page);
        size = Math.min(100, Math.max(1, size));
        Page<Attendance> p = attendanceMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Attendance>()
                        .inSql(Attendance::getRegistrationId,
                                "SELECT id FROM registration WHERE user_id = " + userId)
                        .orderByDesc(Attendance::getId));
        Map<Long, Activity> activities = new HashMap<>();
        Map<Long, ServiceHours> hoursByAttendance = new HashMap<>();
        List<Long> regIds = p.getRecords().stream().map(Attendance::getRegistrationId).toList();
        List<Long> attIds = p.getRecords().stream().map(Attendance::getId).toList();
        if (!regIds.isEmpty()) {
            for (Registration r : registrationMapper.selectBatchIds(regIds)) {
                Activity a = activityMapper.selectById(r.getActivityId());
                if (a != null) {
                    activities.put(r.getId(), a);
                }
            }
        }
        if (!attIds.isEmpty()) {
            for (ServiceHours h : serviceHoursMapper.selectList(new LambdaQueryWrapper<ServiceHours>()
                    .in(ServiceHours::getAttendanceId, attIds))) {
                hoursByAttendance.put(h.getAttendanceId(), h);
            }
        }
        List<AttendanceResponse> items = new ArrayList<>(p.getRecords().size());
        for (Attendance at : p.getRecords()) {
            Registration r = registrationMapper.selectById(at.getRegistrationId());
            Activity a = r == null ? null : activities.get(r.getId());
            ServiceHours h = hoursByAttendance.get(at.getId());
            items.add(AttendanceResponse.builder()
                    .attendanceId(at.getId())
                    .activityId(a == null ? null : a.getId())
                    .activityTitle(a == null ? "活动已删除" : a.getTitle())
                    .signinTime(at.getSigninTime())
                    .signoutTime(at.getSignoutTime())
                    .durationMinutes(at.getDurationMinutes())
                    .signinMethod(at.getSigninMethod())
                    .auditStatus(at.getAuditStatus())
                    .hours(h == null ? null : h.getHours())
                    .calcRuleVersion(h == null ? null : h.getCalcRuleVersion())
                    .hoursStatus(h == null ? null : h.getStatus())
                    .build());
        }
        return PageResult.of(items, p.getTotal(), page, size);
    }

    /** 组织者查看某活动的签到情况 */
    public PageResult<AttendanceAdminResponse> activityRecords(Long activityId, Long operatorId, int page, int size) {
        Activity activity = requireActivity(activityId);
        checkOwnerOrAdmin(activity, operatorId);
        page = Math.max(1, page);
        size = Math.min(100, Math.max(1, size));
        Page<Registration> p = registrationMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Registration>()
                        .eq(Registration::getActivityId, activityId)
                        .eq(Registration::getStatus, "APPROVED")
                        .orderByDesc(Registration::getId));
        List<AttendanceAdminResponse> items = new ArrayList<>(p.getRecords().size());
        for (Registration r : p.getRecords()) {
            Attendance at = attendanceMapper.selectOne(new LambdaQueryWrapper<Attendance>()
                    .eq(Attendance::getRegistrationId, r.getId()));
            ServiceHours h = at == null ? null : serviceHoursMapper.selectOne(
                    new LambdaQueryWrapper<ServiceHours>().eq(ServiceHours::getAttendanceId, at.getId()));
            User u = userMapper.selectById(r.getUserId());
            boolean checkedIn = at != null;
            boolean checkedOut = at != null && at.getSignoutTime() != null;
            items.add(AttendanceAdminResponse.builder()
                    .registrationId(r.getId())
                    .userId(r.getUserId())
                    .realName(u == null ? null : u.getRealName())
                    .studentNo(u == null ? null : u.getStudentNo())
                    .registrationStatus(r.getStatus())
                    .signinTime(at == null ? null : at.getSigninTime())
                    .signoutTime(at == null ? null : at.getSignoutTime())
                    .durationMinutes(at == null ? null : at.getDurationMinutes())
                    .signinMethod(at == null ? null : at.getSigninMethod())
                    .checkedIn(checkedIn)
                    .checkedOut(checkedOut)
                    .hours(h == null ? null : h.getHours())
                    .build());
        }
        return PageResult.of(items, p.getTotal(), page, size);
    }

    /** 我的工时流水 */
    public PageResult<HoursResponse> myHours(Long userId, int page, int size) {
        page = Math.max(1, page);
        size = Math.min(100, Math.max(1, size));
        Page<ServiceHours> p = serviceHoursMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<ServiceHours>()
                        .eq(ServiceHours::getUserId, userId)
                        .orderByDesc(ServiceHours::getId));
        List<HoursResponse> items = new ArrayList<>(p.getRecords().size());
        for (ServiceHours h : p.getRecords()) {
            Activity a = activityMapper.selectById(h.getActivityId());
            items.add(HoursResponse.builder()
                    .id(h.getId())
                    .activityId(h.getActivityId())
                    .activityTitle(a == null ? "活动已删除" : a.getTitle())
                    .attendanceId(h.getAttendanceId())
                    .hours(h.getHours())
                    .calcRuleVersion(h.getCalcRuleVersion())
                    .status(h.getStatus())
                    .createdAt(h.getCreatedAt())
                    .build());
        }
        return PageResult.of(items, p.getTotal(), page, size);
    }

    // ---------- 私有方法 ----------

    /** 工时规则 V1：满 15 分钟才计，时长/60 保留 2 位小数 */
    private BigDecimal calcHours(int durationMinutes) {
        if (durationMinutes < MIN_SERVICE_MINUTES) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(durationMinutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private Activity requireActivity(Long id) {
        Activity a = activityMapper.selectById(id);
        if (a == null || a.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);
        }
        return a;
    }

    private Registration requireApprovedRegistration(Long activityId, Long userId) {
        Registration reg = registrationMapper.selectOne(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, activityId)
                .eq(Registration::getUserId, userId));
        if (reg == null || !"APPROVED".equals(reg.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_REGISTERED);
        }
        return reg;
    }

    private void checkOwnerOrAdmin(Activity activity, Long operatorId) {
        if (activity.getOrganizerId() != null && activity.getOrganizerId().equals(operatorId)) {
            return;
        }
        if (isAdmin(operatorId)) {
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    private boolean isAdmin(Long userId) {
        List<UserRole> links = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (links.isEmpty()) {
            return false;
        }
        List<Long> roleIds = links.stream().map(UserRole::getRoleId).toList();
        return roleMapper.selectBatchIds(roleIds).stream().anyMatch(r -> "ADMIN".equals(r.getCode()));
    }
}
