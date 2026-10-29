package com.gdupt.volunteer.stats.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopActivityItem {

    private Long activityId;

    private String title;

    private Long regCount;
}
