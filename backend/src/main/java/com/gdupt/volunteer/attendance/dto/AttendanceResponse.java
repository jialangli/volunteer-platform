package com.gdupt.volunteer.attendance.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 志愿者视角：我的签到记录（含核算工时） */
@Data
@Builder
public class AttendanceResponse {

    private Long attendanceId;

    private Long activityId;

    private String activityTitle;

    private LocalDateTime signinTime;

    private LocalDateTime signoutTime;

    /** 有效服务时长（分钟） */
    private Integer durationMinutes;

    /** QR/GEO/MANUAL */
    private String signinMethod;

    /** VALID/PENDING/INVALID */
    private String auditStatus;

    /** 核算工时（小时），未签退为 null */
    private BigDecimal hours;

    /** 工时核算规则版本 */
    private String calcRuleVersion;

    /** 工时状态 CONFIRMED/PENDING/CANCELLED，未签退为 null */
    private String hoursStatus;
}
