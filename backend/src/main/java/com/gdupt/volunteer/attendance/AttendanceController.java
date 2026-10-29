package com.gdupt.volunteer.attendance;

import com.gdupt.volunteer.attendance.dto.AttendanceAdminResponse;
import com.gdupt.volunteer.attendance.dto.AttendanceResponse;
import com.gdupt.volunteer.attendance.dto.HoursResponse;
import com.gdupt.volunteer.attendance.dto.SigninRequest;
import com.gdupt.volunteer.common.ApiResponse;
import com.gdupt.volunteer.common.CurrentUser;
import com.gdupt.volunteer.common.PageResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/attendance/{activityId}/signin")
    public ApiResponse<Long> signin(@PathVariable Long activityId,
                                    @Valid @RequestBody(required = false) SigninRequest req,
                                    HttpServletRequest request) {
        String method = req == null ? null : req.getMethod();
        Long targetUserId = req == null ? null : req.getUserId();
        return ApiResponse.ok(attendanceService.signin(activityId, CurrentUser.getUserId(), method, targetUserId, getIp(request)));
    }

    @PostMapping("/attendance/{activityId}/signout")
    public ApiResponse<Void> signout(@PathVariable Long activityId, HttpServletRequest request) {
        attendanceService.signout(activityId, CurrentUser.getUserId(), getIp(request));
        return ApiResponse.ok();
    }

    @GetMapping("/attendance/mine")
    public ApiResponse<PageResult<AttendanceResponse>> myRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(attendanceService.myRecords(CurrentUser.getUserId(), page, size));
    }

    @GetMapping("/activities/{id}/attendance")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ApiResponse<PageResult<AttendanceAdminResponse>> activityRecords(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(attendanceService.activityRecords(id, CurrentUser.getUserId(), page, size));
    }

    @GetMapping("/hours/mine")
    public ApiResponse<PageResult<HoursResponse>> myHours(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(attendanceService.myHours(CurrentUser.getUserId(), page, size));
    }

    private String getIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
