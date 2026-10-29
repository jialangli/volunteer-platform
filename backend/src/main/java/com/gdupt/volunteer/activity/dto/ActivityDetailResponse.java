package com.gdupt.volunteer.activity.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** 活动详情：全字段 + 类型名/组织者 + 当前用户报名状态 */
@Data
@Builder
public class ActivityDetailResponse {

    private Long id;

    private String title;

    private String description;

    private Long typeId;

    private String typeName;

    private Long organizerId;

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

    private LocalDateTime createdAt;

    /** 已报名人数（PENDING + APPROVED） */
    private Long registeredCount;

    /** 当前登录用户对本活动的报名状态，未报名为 null */
    private String myRegistrationStatus;
}
