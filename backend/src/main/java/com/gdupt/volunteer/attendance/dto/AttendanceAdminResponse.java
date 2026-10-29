package com.gdupt.volunteer.attendance.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 组织者视角：某活动报名成员的签到情况 */
@Data
@Builder
public class AttendanceAdminResponse {

    private Long registrationId;

    private Long userId;

    private String realName;

    private String studentNo;

    /** APPROVED 报名者 */
    private String registrationStatus;

    private LocalDateTime signinTime;

    private LocalDateTime signoutTime;

    private Integer durationMinutes;

    private String signinMethod;

    /** 是否已签到 */
    private boolean checkedIn;

    /** 是否已签退 */
    private boolean checkedOut;

    /** 核算工时（小时） */
    private BigDecimal hours;
}
