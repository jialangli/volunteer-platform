package com.gdupt.volunteer.stats;

import com.gdupt.volunteer.common.ApiResponse;
import com.gdupt.volunteer.stats.dto.MonthlyTrendItem;
import com.gdupt.volunteer.stats.dto.OverviewResponse;
import com.gdupt.volunteer.stats.dto.TopActivityItem;
import com.gdupt.volunteer.stats.dto.TopVolunteerItem;
import com.gdupt.volunteer.stats.dto.TypeDistItem;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/overview")
    public ApiResponse<OverviewResponse> overview() {
        return ApiResponse.ok(statsService.overview());
    }

    @GetMapping("/type-distribution")
    public ApiResponse<List<TypeDistItem>> typeDistribution() {
        return ApiResponse.ok(statsService.typeDistribution());
    }

    @GetMapping("/monthly-trend")
    public ApiResponse<List<MonthlyTrendItem>> monthlyTrend(
            @RequestParam(defaultValue = "6") int months) {
        return ApiResponse.ok(statsService.monthlyTrend(months));
    }

    @GetMapping("/top-activities")
    public ApiResponse<List<TopActivityItem>> topActivities(
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.ok(statsService.topActivities(limit));
    }

    @GetMapping("/top-volunteers")
    public ApiResponse<List<TopVolunteerItem>> topVolunteers(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(statsService.topVolunteers(limit));
    }
}
