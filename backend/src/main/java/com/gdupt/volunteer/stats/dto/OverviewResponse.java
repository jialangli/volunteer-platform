package com.gdupt.volunteer.stats.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** 平台总览 */
@Data
@Builder
public class OverviewResponse {

    private Long activityCount;

    private Long volunteerCount;

    private Long totalServiceCount;

    private BigDecimal totalHours;

    /** 本月新增活动数 */
    private Long monthActivityCount;
}
