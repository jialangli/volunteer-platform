package com.gdupt.volunteer.stats.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MonthlyTrendItem {

    private String month;

    private Long activityCount;

    private BigDecimal totalHours;
}
