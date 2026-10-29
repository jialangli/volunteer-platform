package com.gdupt.volunteer.certification;

import com.gdupt.volunteer.certification.dto.CertResponse;
import com.gdupt.volunteer.certification.dto.IssueCertRequest;
import com.gdupt.volunteer.common.ApiResponse;
import com.gdupt.volunteer.common.CurrentUser;
import com.gdupt.volunteer.common.PageResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/certifications")
@RequiredArgsConstructor
public class CertificationController {

    private final CertificationService certificationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ApiResponse<Long> issue(@Valid @RequestBody IssueCertRequest req, HttpServletRequest request) {
        return ApiResponse.ok(certificationService.issue(req, CurrentUser.getUserId(), getIp(request)));
    }

    @GetMapping("/mine")
    public ApiResponse<PageResult<CertResponse>> mine(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(certificationService.listMine(CurrentUser.getUserId(), page, size));
    }

    @GetMapping("/volunteer/{userId}")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ApiResponse<PageResult<CertResponse>> byVolunteer(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(certificationService.listByVolunteer(userId, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<CertResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(certificationService.detail(id, CurrentUser.getUserId()));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        return certificationService.downloadPdf(id, CurrentUser.getUserId());
    }

    private String getIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
