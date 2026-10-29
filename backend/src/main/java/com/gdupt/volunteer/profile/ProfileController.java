package com.gdupt.volunteer.profile;

import com.gdupt.volunteer.common.ApiResponse;
import com.gdupt.volunteer.common.CurrentUser;
import com.gdupt.volunteer.common.PageResult;
import com.gdupt.volunteer.profile.dto.MyProfileResponse;
import com.gdupt.volunteer.profile.dto.UpdateProfileRequest;
import com.gdupt.volunteer.profile.dto.VolunteerListItem;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/mine")
    public ApiResponse<MyProfileResponse> myProfile() {
        return ApiResponse.ok(profileService.myProfile(CurrentUser.getUserId()));
    }

    @PutMapping("/mine")
    public ApiResponse<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest req) {
        profileService.updateProfile(CurrentUser.getUserId(), req);
        return ApiResponse.ok();
    }

    @GetMapping("/volunteers")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ApiResponse<PageResult<VolunteerListItem>> volunteers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(profileService.volunteerList(keyword, page, size));
    }
}
