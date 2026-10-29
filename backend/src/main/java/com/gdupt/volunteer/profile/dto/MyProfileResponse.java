package com.gdupt.volunteer.profile.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 我的档案：基本信息 + 服务汇总 + 等级 */
@Data
@Builder
public class MyProfileResponse {

    private Long id;

    private String username;

    private String realName;

    private String studentNo;

    private String phone;

    private List<String> roles;

    /** 累计服务时长（小时） */
    private BigDecimal totalHours;

    /** 有效服务次数 */
    private Long serviceCount;

    /** 参与活动数 */
    private Long activityCount;

    /** 等级（1~4） */
    private Integer level;

    /** 等级名称（一星/二星/三星/四星志愿者） */
    private String levelName;

    private LocalDateTime createdAt;
}
