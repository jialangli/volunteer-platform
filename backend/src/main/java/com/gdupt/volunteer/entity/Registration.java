package com.gdupt.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("registration")
public class Registration {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long activityId;

    private Long userId;

    /** PENDING/APPROVED/REJECTED/CANCELLED/WAIVED */
    private String status;

    private LocalDateTime applyTime;

    private LocalDateTime auditTime;

    private Long auditBy;

    private String auditReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
