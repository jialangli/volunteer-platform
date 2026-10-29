package com.gdupt.volunteer.profile.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 志愿者档案列表项 */
@Data
@Builder
public class VolunteerListItem {

    private Long id;

    private String username;

    private String realName;

    private String studentNo;

    private String phone;

    /** 1 正常 / 0 禁用 */
    private Integer status;

    private BigDecimal totalHours;

    private Long serviceCount;

    private Integer level;

    private String levelName;

    private LocalDateTime createdAt;
}
