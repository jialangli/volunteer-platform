package com.gdupt.volunteer.stats.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TypeDistItem {

    private Long typeId;

    private String typeName;

    private Long activityCount;

    private Long registrationCount;
}
