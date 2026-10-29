package com.gdupt.volunteer.activity.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** 活动列表项 */
@Data
@Builder
public class ActivityResponse {

    private Long id;

    private String title;

    private Long typeId;

    private String typeName;

    private String organizerName;

    private String location;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime signinStart;

    private LocalDateTime signinEnd;

    private LocalDateTime signoutEnd;

    private Integer capacity;

    private Integer capacityRemaining;

    private String status;

    private String coverUrl;

    /** 已报名人数（PENDING + APPROVED） */
    private Long registeredCount;

    /** 当前用户对该活动的报名状态（未报名为 null） */
    private String myRegistrationStatus;
}
