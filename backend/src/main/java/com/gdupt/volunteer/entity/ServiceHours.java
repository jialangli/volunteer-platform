package com.gdupt.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("service_hours")
public class ServiceHours {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long activityId;

    private Long attendanceId;

    /** 核算时长（小时） */
    private BigDecimal hours;

    /** 核算规则版本号，保证口径可回溯 */
    private String calcRuleVersion;

    /** CONFIRMED/PENDING/CANCELLED */
    private String status;

    private LocalDateTime createdAt;
}
