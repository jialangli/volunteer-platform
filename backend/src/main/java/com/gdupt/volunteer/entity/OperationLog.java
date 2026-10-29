package com.gdupt.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String module;

    private String action;

    private String targetType;

    private Long targetId;

    /** 变更前后快照（JSON 字符串） */
    private String detail;

    private String ip;

    private LocalDateTime createdAt;
}
