package com.gdupt.volunteer.activity.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** 志愿者视角：我的报名记录 */
@Data
@Builder
public class RegistrationResponse {

    private Long id;

    private Long activityId;

    private String activityTitle;

    private LocalDateTime activityStartTime;

    private LocalDateTime activityEndTime;

    /** PENDING/APPROVED/REJECTED/CANCELLED/WAIVED */
    private String status;

    private LocalDateTime applyTime;

    private LocalDateTime auditTime;

    private String auditReason;
}
