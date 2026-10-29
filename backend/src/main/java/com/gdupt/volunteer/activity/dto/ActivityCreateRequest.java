package com.gdupt.volunteer.activity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityCreateRequest {

    @NotBlank(message = "活动标题不能为空")
    @Size(max = 100, message = "活动标题最长 100 字")
    private String title;

    @Size(max = 5000, message = "活动描述过长")
    private String description;

    private Long typeId;

    @Size(max = 255, message = "地点过长")
    private String location;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    /** 签到开放时间 */
    private LocalDateTime signinStart;

    /** 签到截止时间 */
    private LocalDateTime signinEnd;

    /** 签退截止时间 */
    private LocalDateTime signoutEnd;

    /** 名额，0=不限 */
    private Integer capacity;

    private String coverUrl;
}
