package com.gdupt.volunteer.admin;

import com.gdupt.volunteer.admin.dto.AnnouncementRequest;
import com.gdupt.volunteer.admin.dto.TypeRequest;
import com.gdupt.volunteer.admin.dto.UpdateUserRolesRequest;
import com.gdupt.volunteer.admin.dto.UpdateUserStatusRequest;
import com.gdupt.volunteer.admin.dto.UserAdminResponse;
import com.gdupt.volunteer.common.ApiResponse;
import com.gdupt.volunteer.common.CurrentUser;
import com.gdupt.volunteer.common.PageResult;
import com.gdupt.volunteer.entity.ActivityType;
import com.gdupt.volunteer.entity.Announcement;
import com.gdupt.volunteer.entity.OperationLog;
import com.gdupt.volunteer.entity.Role;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // ---------- 用户管理 ----------

    @GetMapping("/users")
    public ApiResponse<PageResult<UserAdminResponse>> users(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(adminService.userList(keyword, page, size));
    }

    @PutMapping("/users/{id}/status")
    public ApiResponse<Void> userStatus(@PathVariable Long id,
                                        @Valid @RequestBody UpdateUserStatusRequest req,
                                        HttpServletRequest request) {
        adminService.updateStatus(id, req, CurrentUser.getUserId(), getIp(request));
        return ApiResponse.ok();
    }

    @PutMapping("/users/{id}/roles")
    public ApiResponse<Void> userRoles(@PathVariable Long id,
                                       @Valid @RequestBody UpdateUserRolesRequest req,
                                       HttpServletRequest request) {
        adminService.updateRoles(id, req, CurrentUser.getUserId(), getIp(request));
        return ApiResponse.ok();
    }

    // ---------- 公告管理 ----------

    @GetMapping("/announcements")
    public ApiResponse<PageResult<Announcement>> announcements(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(adminService.announcementList(status, page, size));
    }

    @PostMapping("/announcements")
    public ApiResponse<Long> createAnnouncement(@Valid @RequestBody AnnouncementRequest req) {
        return ApiResponse.ok(adminService.createAnnouncement(req, CurrentUser.getUserId()));
    }

    @PutMapping("/announcements/{id}")
    public ApiResponse<Void> updateAnnouncement(@PathVariable Long id,
                                                @Valid @RequestBody AnnouncementRequest req) {
        adminService.updateAnnouncement(id, req);
        return ApiResponse.ok();
    }

    @PostMapping("/announcements/{id}/publish")
    public ApiResponse<Void> publishAnnouncement(@PathVariable Long id) {
        adminService.publishAnnouncement(id, true);
        return ApiResponse.ok();
    }

    @PostMapping("/announcements/{id}/unpublish")
    public ApiResponse<Void> unpublishAnnouncement(@PathVariable Long id) {
        adminService.publishAnnouncement(id, false);
        return ApiResponse.ok();
    }

    @DeleteMapping("/announcements/{id}")
    public ApiResponse<Void> deleteAnnouncement(@PathVariable Long id) {
        adminService.deleteAnnouncement(id);
        return ApiResponse.ok();
    }

    // ---------- 活动类型管理 ----------

    @GetMapping("/types")
    public ApiResponse<List<ActivityType>> types() {
        return ApiResponse.ok(adminService.typeList());
    }

    @PostMapping("/types")
    public ApiResponse<Long> createType(@Valid @RequestBody TypeRequest req) {
        return ApiResponse.ok(adminService.createType(req));
    }

    @PutMapping("/types/{id}")
    public ApiResponse<Void> updateType(@PathVariable Long id, @Valid @RequestBody TypeRequest req) {
        adminService.updateType(id, req);
        return ApiResponse.ok();
    }

    @DeleteMapping("/types/{id}")
    public ApiResponse<Void> deleteType(@PathVariable Long id) {
        adminService.deleteType(id);
        return ApiResponse.ok();
    }

    // ---------- 角色 ----------

    @GetMapping("/roles")
    public ApiResponse<List<Role>> roles() {
        return ApiResponse.ok(adminService.roleList());
    }

    // ---------- 操作日志 ----------

    @GetMapping("/logs")
    public ApiResponse<PageResult<OperationLog>> logs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(adminService.logList(userId, module, page, size));
    }

    private String getIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
