package com.gdupt.volunteer.activity;

import com.gdupt.volunteer.activity.dto.ActivityCreateRequest;
import com.gdupt.volunteer.activity.dto.ActivityDetailResponse;
import com.gdupt.volunteer.activity.dto.ActivityListQuery;
import com.gdupt.volunteer.activity.dto.ActivityResponse;
import com.gdupt.volunteer.activity.dto.ActivityUpdateRequest;
import com.gdupt.volunteer.activity.dto.AuditRequest;
import com.gdupt.volunteer.activity.dto.RegistrationAdminResponse;
import com.gdupt.volunteer.activity.dto.RegistrationRequest;
import com.gdupt.volunteer.activity.dto.RegistrationResponse;
import com.gdupt.volunteer.common.ApiResponse;
import com.gdupt.volunteer.common.CurrentUser;
import com.gdupt.volunteer.common.PageResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @PostMapping("/activities")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ApiResponse<Long> create(@Valid @RequestBody ActivityCreateRequest req, HttpServletRequest request) {
        return ApiResponse.ok(activityService.create(req, CurrentUser.getUserId(), getIp(request)));
    }

    @PutMapping("/activities/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody ActivityUpdateRequest req,
                                    HttpServletRequest request) {
        activityService.update(id, req, CurrentUser.getUserId(), getIp(request));
        return ApiResponse.ok();
    }

    @PostMapping("/activities/{id}/status")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ApiResponse<Void> changeStatus(@PathVariable Long id, @RequestParam String action,
                                          HttpServletRequest request) {
        activityService.changeStatus(id, action, CurrentUser.getUserId(), getIp(request));
        return ApiResponse.ok();
    }

    @DeleteMapping("/activities/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        activityService.delete(id, CurrentUser.getUserId(), getIp(request));
        return ApiResponse.ok();
    }

    @GetMapping("/activities")
    public ApiResponse<PageResult<ActivityResponse>> list(ActivityListQuery query) {
        return ApiResponse.ok(activityService.list(query, CurrentUser.getUserId()));
    }

    @GetMapping("/activities/{id}")
    public ApiResponse<ActivityDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(activityService.detail(id, CurrentUser.getUserId()));
    }

    @PostMapping("/registrations")
    public ApiResponse<Long> enroll(@Valid @RequestBody RegistrationRequest req, HttpServletRequest request) {
        return ApiResponse.ok(activityService.enroll(req.getActivityId(), CurrentUser.getUserId(), getIp(request)));
    }

    @DeleteMapping("/registrations/{activityId}")
    public ApiResponse<Void> cancel(@PathVariable Long activityId, HttpServletRequest request) {
        activityService.cancel(activityId, CurrentUser.getUserId(), getIp(request));
        return ApiResponse.ok();
    }

    @GetMapping("/registrations/mine")
    public ApiResponse<PageResult<RegistrationResponse>> myRegistrations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(activityService.myRegistrations(CurrentUser.getUserId(), page, size));
    }

    @GetMapping("/activities/{id}/registrations")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ApiResponse<PageResult<RegistrationAdminResponse>> listRegistrations(
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(activityService.listRegistrations(id, status, page, size, CurrentUser.getUserId()));
    }

    @PostMapping("/registrations/{id}/audit")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ApiResponse<Void> audit(@PathVariable Long id, @Valid @RequestBody AuditRequest req,
                                   HttpServletRequest request) {
        activityService.audit(id, req.getApprove(), req.getReason(), CurrentUser.getUserId(), getIp(request));
        return ApiResponse.ok();
    }

    private String getIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
