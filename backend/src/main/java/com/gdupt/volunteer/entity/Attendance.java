package com.gdupt.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("attendance")
public class Attendance {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long registrationId;

    private LocalDateTime signinTime;

    private LocalDateTime signoutTime;

    /** 有效服务时长（分钟） */
    private Integer durationMinutes;

    /** QR/GEO/MANUAL */
    private String signinMethod;

    /** VALID/PENDING/INVALID */
    private String auditStatus;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
