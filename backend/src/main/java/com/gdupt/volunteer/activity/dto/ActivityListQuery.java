package com.gdupt.volunteer.activity.dto;

import lombok.Data;

/** 活动分页查询参数 */
@Data
public class ActivityListQuery {

    private int page = 1;

    private int size = 10;

    /** DRAFT/RECRUITING/ONGOING/FINISHED/ARCHIVED/CANCELLED */
    private String status;

    private Long typeId;

    /** 标题关键字 */
    private String keyword;

    /** true=只看我（组织者）发布的活动 */
    private Boolean onlyMine;
}
