package com.gdupt.volunteer.activity.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** 组织者视角：某活动的报名审核列表 */
@Data
@Builder
public class RegistrationAdminResponse {

    private Long id;

    private Long userId;

    private String realName;

    private String studentNo;

    private String phone;

    /** PENDING/APPROVED/REJECTED/CANCELLED/WAIVED */
    private String status;

    private LocalDateTime applyTime;

    private LocalDateTime auditTime;

    private String auditReason;
}
