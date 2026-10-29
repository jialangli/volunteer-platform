package com.gdupt.volunteer.stats.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TopVolunteerItem {

    private Long userId;

    private String realName;

    private String studentNo;

    private BigDecimal totalHours;

    private Long serviceCount;

    private Long activityCount;
}
