package com.gdupt.volunteer.activity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gdupt.volunteer.activity.dto.ActivityCreateRequest;
import com.gdupt.volunteer.activity.dto.ActivityDetailResponse;
import com.gdupt.volunteer.activity.dto.ActivityListQuery;
import com.gdupt.volunteer.activity.dto.ActivityResponse;
import com.gdupt.volunteer.activity.dto.ActivityUpdateRequest;
import com.gdupt.volunteer.activity.dto.RegistrationAdminResponse;
import com.gdupt.volunteer.activity.dto.RegistrationResponse;
import com.gdupt.volunteer.common.BusinessException;
import com.gdupt.volunteer.common.ErrorCode;
import com.gdupt.volunteer.common.OperationLogService;
import com.gdupt.volunteer.common.PageResult;
import com.gdupt.volunteer.entity.Activity;
import com.gdupt.volunteer.entity.Registration;
import com.gdupt.volunteer.entity.Role;
import com.gdupt.volunteer.entity.User;
import com.gdupt.volunteer.entity.UserRole;
import com.gdupt.volunteer.mapper.ActivityMapper;
import com.gdupt.volunteer.mapper.ActivityTypeMapper;
import com.gdupt.volunteer.mapper.RegistrationMapper;
import com.gdupt.volunteer.mapper.RoleMapper;
import com.gdupt.volunteer.mapper.UserMapper;
import com.gdupt.volunteer.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 活动与报名模块：
 * - 活动 CRUD 与状态流转（DRAFT→RECRUITING→ONGOING→FINISHED→ARCHIVED，任意→CANCELLED）
 * - 报名：capacity>0 时原子扣减剩余名额防超卖 + (activity_id,user_id) 唯一约束防重复报名
 * - 取消/审核：取消与审核拒绝时原子回补名额
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    /** 活动状态迁移白名单 */
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "DRAFT", Set.of("RECRUITING", "CANCELLED", "ARCHIVED"),
            "RECRUITING", Set.of("ONGOING", "FINISHED", "CANCELLED"),
            "ONGOING", Set.of("FINISHED", "CANCELLED"),
            "FINISHED", Set.of("ARCHIVED"),
            "CANCELLED", Set.of("ARCHIVED"),
            "ARCHIVED", Set.of());

    private final ActivityMapper activityMapper;
    private final ActivityTypeMapper activityTypeMapper;
    private final RegistrationMapper registrationMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final OperationLogService operationLogService;

    public Long create(ActivityCreateRequest req, Long userId, String ip) {
        validateTimes(req.getStartTime(), req.getEndTime(), req.getSigninStart(), req.getSigninEnd(), req.getSignoutEnd());
        int capacity = req.getCapacity() == null ? 0 : Math.max(0, req.getCapacity());
        Activity activity = new Activity();
        activity.setTitle(req.getTitle());
        activity.setDescription(req.getDescription());
        activity.setTypeId(req.getTypeId());
        activity.setOrganizerId(userId);
        activity.setLocation(req.getLocation());
        activity.setStartTime(req.getStartTime());
        activity.setEndTime(req.getEndTime());
        activity.setSigninStart(req.getSigninStart());
        activity.setSigninEnd(req.getSigninEnd());
        activity.setSignoutEnd(req.getSignoutEnd());
        activity.setCapacity(capacity);
        activity.setCapacityRemaining(capacity);
        activity.setStatus("DRAFT");
        activity.setCoverUrl(req.getCoverUrl());
        activity.setVersion(0);
        activityMapper.insert(activity);
        operationLogService.record(userId, "activity", "CREATE", "ACTIVITY", activity.getId(), ip, null);
        return activity.getId();
    }

    public void update(Long id, ActivityUpdateRequest req, Long operatorId, String ip) {
        Activity activity = requireActivity(id);
        checkOwnerOrAdmin(activity, operatorId);
        if (!Set.of("DRAFT", "RECRUITING").contains(activity.getStatus())) {
            throw new BusinessException(ErrorCode.ACTIVITY_STATUS_INVALID, "仅草稿或招募中的活动可编辑");
        }
        validateTimes(req.getStartTime(), req.getEndTime(), req.getSigninStart(), req.getSigninEnd(), req.getSignoutEnd());
        activity.setTitle(req.getTitle());
        activity.setDescription(req.getDescription());
        activity.setTypeId(req.getTypeId());
        activity.setLocation(req.getLocation());
        activity.setStartTime(req.getStartTime());
        activity.setEndTime(req.getEndTime());
        activity.setSigninStart(req.getSigninStart());
        activity.setSigninEnd(req.getSigninEnd());
        activity.setSignoutEnd(req.getSignoutEnd());
        activity.setCoverUrl(req.getCoverUrl());
        // 名额调整：剩余名额 = max(0, 新名额 - 已占名额)
        int newCapacity = req.getCapacity() == null ? 0 : Math.max(0, req.getCapacity());
        long used = countUsed(id);
        activity.setCapacity(newCapacity);
        activity.setCapacityRemaining(newCapacity == 0 ? 0 : (int) Math.max(0, newCapacity - used));
        activityMapper.updateById(activity);
        operationLogService.record(operatorId, "activity", "UPDATE", "ACTIVITY", id, ip, null);
    }

    public void changeStatus(Long id, String action, Long operatorId, String ip) {
        Activity activity = requireActivity(id);
        checkOwnerOrAdmin(activity, operatorId);
        String target = switch (action == null ? "" : action) {
            case "publish" -> "RECRUITING";
            case "finish" -> "FINISHED";
            case "cancel" -> "CANCELLED";
            case "archive" -> "ARCHIVED";
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的 action：" + action);
        };
        if (!TRANSITIONS.getOrDefault(activity.getStatus(), Set.of()).contains(target)) {
            throw new BusinessException(ErrorCode.ACTIVITY_STATUS_INVALID,
                    "活动状态 " + activity.getStatus() + " 不允许流转到 " + target);
        }
        Activity patch = new Activity();
        patch.setId(id);
        patch.setStatus(target);
        activityMapper.updateById(patch);
        operationLogService.record(operatorId, "activity", "STATUS_" + target, "ACTIVITY", id, ip, null);
    }

    public void delete(Long id, Long operatorId, String ip) {
        Activity activity = requireActivity(id);
        checkOwnerOrAdmin(activity, operatorId);
        if (!"DRAFT".equals(activity.getStatus())) {
            throw new BusinessException(ErrorCode.ACTIVITY_STATUS_INVALID, "仅草稿状态的活动可删除");
        }
        Activity patch = new Activity();
        patch.setId(id);
        patch.setDeletedAt(LocalDateTime.now());
        activityMapper.updateById(patch);
        operationLogService.record(operatorId, "activity", "DELETE", "ACTIVITY", id, ip, null);
    }

    public PageResult<ActivityResponse> list(ActivityListQuery query, Long currentUserId) {
        int page = Math.max(1, query.getPage());
        int size = Math.min(100, Math.max(1, query.getSize()));
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(Activity::getDeletedAt);
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            wrapper.eq(Activity::getStatus, query.getStatus());
        }
        if (query.getTypeId() != null) {
            wrapper.eq(Activity::getTypeId, query.getTypeId());
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.like(Activity::getTitle, query.getKeyword());
        }
        if (Boolean.TRUE.equals(query.getOnlyMine())) {
            wrapper.eq(Activity::getOrganizerId, currentUserId);
        }
        wrapper.orderByAsc(Activity::getStartTime);
        Page<Activity> p = activityMapper.selectPage(new Page<>(page, size), wrapper);
        List<Activity> records = p.getRecords();

        Map<Long, String> typeNames = new HashMap<>();
        Map<Long, String> orgNames = new HashMap<>();
        if (!records.isEmpty()) {
            typeNames = activityTypeMapper.selectBatchIds(records.stream()
                            .map(Activity::getTypeId).filter(java.util.Objects::nonNull).toList())
                    .stream().collect(Collectors.toMap(t -> t.getId(), t -> t.getName(), (a, b) -> a));
            orgNames = userMapper.selectBatchIds(records.stream()
                            .map(Activity::getOrganizerId).filter(java.util.Objects::nonNull).toList())
                    .stream().collect(Collectors.toMap(User::getId, User::getRealName, (a, b) -> a));
        }
        List<ActivityResponse> items = new ArrayList<>(records.size());
        Map<Long, String> myRegStatus = new HashMap<>();
        if (currentUserId != null && !records.isEmpty()) {
            registrationMapper.selectList(new LambdaQueryWrapper<Registration>()
                            .eq(Registration::getUserId, currentUserId)
                            .in(Registration::getActivityId, records.stream().map(Activity::getId).toList()))
                    .forEach(r -> myRegStatus.put(r.getActivityId(), r.getStatus()));
        }
        for (Activity a : records) {
            items.add(ActivityResponse.builder()
                    .id(a.getId())
                    .title(a.getTitle())
                    .typeId(a.getTypeId())
                    .typeName(a.getTypeId() == null ? null : typeNames.get(a.getTypeId()))
                    .organizerName(orgNames.get(a.getOrganizerId()))
                    .location(a.getLocation())
                    .startTime(a.getStartTime())
                    .endTime(a.getEndTime())
                    .signinStart(a.getSigninStart())
                    .signinEnd(a.getSigninEnd())
                    .signoutEnd(a.getSignoutEnd())
                    .capacity(a.getCapacity())
                    .capacityRemaining(a.getCapacityRemaining())
                    .status(a.getStatus())
                    .coverUrl(a.getCoverUrl())
                    .registeredCount(countUsed(a.getId()))
                    .myRegistrationStatus(myRegStatus.get(a.getId()))
                    .build());
        }
        return PageResult.of(items, p.getTotal(), page, size);
    }

    public ActivityDetailResponse detail(Long id, Long currentUserId) {
        Activity a = requireActivity(id);
        String typeName = a.getTypeId() == null ? null
                : activityTypeMapper.selectById(a.getTypeId()) == null ? null
                : activityTypeMapper.selectById(a.getTypeId()).getName();
        User organizer = userMapper.selectById(a.getOrganizerId());
        String myStatus = null;
        Registration mine = registrationMapper.selectOne(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, id)
                .eq(Registration::getUserId, currentUserId));
        if (mine != null) {
            myStatus = mine.getStatus();
        }
        return ActivityDetailResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .description(a.getDescription())
                .typeId(a.getTypeId())
                .typeName(typeName)
                .organizerId(a.getOrganizerId())
                .organizerName(organizer == null ? null : organizer.getRealName())
                .location(a.getLocation())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .signinStart(a.getSigninStart())
                .signinEnd(a.getSigninEnd())
                .signoutEnd(a.getSignoutEnd())
                .capacity(a.getCapacity())
                .capacityRemaining(a.getCapacityRemaining())
                .status(a.getStatus())
                .coverUrl(a.getCoverUrl())
                .createdAt(a.getCreatedAt())
                .registeredCount(countUsed(id))
                .myRegistrationStatus(myStatus)
                .build();
    }

    /** 报名：仅 RECRUITING 状态可报名；capacity>0 时原子扣减名额，防止超卖 */
    @Transactional
    public Long enroll(Long activityId, Long userId, String ip) {
        Activity activity = requireActivity(activityId);
        if (!"RECRUITING".equals(activity.getStatus())) {
            throw new BusinessException(ErrorCode.ACTIVITY_NOT_RECRUITING);
        }
        // 快速路径：已报名则提示重复（并发场景仍由唯一约束 (activity_id,user_id) 兜底）
        Long exists = registrationMapper.selectCount(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, activityId)
                .eq(Registration::getUserId, userId));
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.REGISTRATION_DUPLICATE);
        }
        if (activity.getCapacity() != null && activity.getCapacity() > 0) {
            int rows = activityMapper.updateRemaining(activityId, 1);
            if (rows == 0) {
                throw new BusinessException(ErrorCode.ACTIVITY_FULL);
            }
        }
        Registration reg = new Registration();
        reg.setActivityId(activityId);
        reg.setUserId(userId);
        reg.setStatus("PENDING");
        try {
            registrationMapper.insert(reg);
        } catch (DuplicateKeyException e) {
            // 唯一约束 (activity_id, user_id)：同事务回滚会一并撤销上面的名额扣减
            throw new BusinessException(ErrorCode.REGISTRATION_DUPLICATE);
        }
        operationLogService.record(userId, "registration", "ENROLL", "ACTIVITY", activityId, ip, null);
        return reg.getId();
    }

    /** 取消报名：释放占用的名额 */
    @Transactional
    public void cancel(Long activityId, Long userId, String ip) {
        Activity activity = requireActivity(activityId);
        Registration reg = registrationMapper.selectOne(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, activityId)
                .eq(Registration::getUserId, userId));
        if (reg == null) {
            throw new BusinessException(ErrorCode.NOT_REGISTERED);
        }
        if (!Set.of("PENDING", "APPROVED").contains(reg.getStatus())) {
            throw new BusinessException(ErrorCode.REGISTRATION_STATUS_INVALID);
        }
        if (!"RECRUITING".equals(activity.getStatus())) {
            throw new BusinessException(ErrorCode.ACTIVITY_STATUS_INVALID, "活动已开始或已结束，无法取消报名");
        }
        if (activity.getCapacity() != null && activity.getCapacity() > 0) {
            activityMapper.updateRemaining(activityId, -1);
        }
        Registration patch = new Registration();
        patch.setId(reg.getId());
        patch.setStatus("CANCELLED");
        registrationMapper.updateById(patch);
        operationLogService.record(userId, "registration", "CANCEL", "ACTIVITY", activityId, ip, null);
    }

    /** 审核报名：通过/拒绝；拒绝时回补名额 */
    @Transactional
    public void audit(Long registrationId, boolean approve, String reason, Long operatorId, String ip) {
        Registration reg = registrationMapper.selectById(registrationId);
        if (reg == null) {
            throw new BusinessException(ErrorCode.NOT_REGISTERED, "报名记录不存在");
        }
        Activity activity = requireActivity(reg.getActivityId());
        checkOwnerOrAdmin(activity, operatorId);
        if (!"PENDING".equals(reg.getStatus())) {
            throw new BusinessException(ErrorCode.REGISTRATION_STATUS_INVALID);
        }
        Registration patch = new Registration();
        patch.setId(reg.getId());
        patch.setStatus(approve ? "APPROVED" : "REJECTED");
        patch.setAuditTime(LocalDateTime.now());
        patch.setAuditBy(operatorId);
        patch.setAuditReason(reason);
        registrationMapper.updateById(patch);
        if (!approve && activity.getCapacity() != null && activity.getCapacity() > 0) {
            activityMapper.updateRemaining(reg.getActivityId(), -1);
        }
        operationLogService.record(operatorId, "registration",
                approve ? "AUDIT_APPROVE" : "AUDIT_REJECT", "REGISTRATION", registrationId, ip, reason);
    }

    /** 我的报名记录 */
    public PageResult<RegistrationResponse> myRegistrations(Long userId, int page, int size) {
        page = Math.max(1, page);
        size = Math.min(100, Math.max(1, size));
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<Registration>()
                .eq(Registration::getUserId, userId)
                .orderByDesc(Registration::getApplyTime)
                .orderByDesc(Registration::getId);
        Page<Registration> p = registrationMapper.selectPage(new Page<>(page, size), wrapper);
        List<RegistrationResponse> items = new ArrayList<>(p.getRecords().size());
        for (Registration r : p.getRecords()) {
            Activity a = activityMapper.selectById(r.getActivityId());
            items.add(RegistrationResponse.builder()
                    .id(r.getId())
                    .activityId(r.getActivityId())
                    .activityTitle(a == null ? "活动已删除" : a.getTitle())
                    .activityStartTime(a == null ? null : a.getStartTime())
                    .activityEndTime(a == null ? null : a.getEndTime())
                    .status(r.getStatus())
                    .applyTime(r.getApplyTime())
                    .auditTime(r.getAuditTime())
                    .auditReason(r.getAuditReason())
                    .build());
        }
        return PageResult.of(items, p.getTotal(), page, size);
    }

    /** 组织者查看某活动的报名列表（含报名人信息） */
    public PageResult<RegistrationAdminResponse> listRegistrations(Long activityId, String status, int page, int size, Long operatorId) {
        Activity activity = requireActivity(activityId);
        checkOwnerOrAdmin(activity, operatorId);
        page = Math.max(1, page);
        size = Math.min(100, Math.max(1, size));
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, activityId)
                .orderByDesc(Registration::getApplyTime)
                .orderByDesc(Registration::getId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(Registration::getStatus, status);
        }
        Page<Registration> p = registrationMapper.selectPage(new Page<>(page, size), wrapper);
        List<RegistrationAdminResponse> items = new ArrayList<>(p.getRecords().size());
        for (Registration r : p.getRecords()) {
            User u = userMapper.selectById(r.getUserId());
            items.add(RegistrationAdminResponse.builder()
                    .id(r.getId())
                    .userId(r.getUserId())
                    .realName(u == null ? null : u.getRealName())
                    .studentNo(u == null ? null : u.getStudentNo())
                    .phone(u == null ? null : u.getPhone())
                    .status(r.getStatus())
                    .applyTime(r.getApplyTime())
                    .auditTime(r.getAuditTime())
                    .auditReason(r.getAuditReason())
                    .build());
        }
        return PageResult.of(items, p.getTotal(), page, size);
    }

    // ---------- 私有方法 ----------

    private Activity requireActivity(Long id) {
        Activity a = activityMapper.selectById(id);
        if (a == null || a.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);
        }
        return a;
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

    private long countUsed(Long activityId) {
        Long c = registrationMapper.selectCount(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, activityId)
                .in(Registration::getStatus, List.of("PENDING", "APPROVED")));
        return c == null ? 0 : c;
    }

    private void validateTimes(LocalDateTime start, LocalDateTime end,
                               LocalDateTime signinStart, LocalDateTime signinEnd, LocalDateTime signoutEnd) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "活动开始时间必须早于结束时间");
        }
        if (signinStart != null && signinEnd != null && !signinStart.isBefore(signinEnd)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "签到开放时间必须早于签到截止时间");
        }
        if (signinEnd != null && signoutEnd != null && !signinEnd.isBefore(signoutEnd)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "签到截止时间必须早于签退截止时间");
        }
    }
}
