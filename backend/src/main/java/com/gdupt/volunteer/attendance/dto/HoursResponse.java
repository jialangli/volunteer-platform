package com.gdupt.volunteer.attendance.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 工时流水 */
@Data
@Builder
public class HoursResponse {

    private Long id;

    private Long activityId;

    private String activityTitle;

    private Long attendanceId;

    private BigDecimal hours;

    /** 核算规则版本号 */
    private String calcRuleVersion;

    /** CONFIRMED/PENDING/CANCELLED */
    private String status;

    private LocalDateTime createdAt;
}
