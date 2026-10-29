package com.gdupt.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity_type")
public class ActivityType {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 1=启用 0=停用 */
    private Integer status;

    private Integer sort;

    private LocalDateTime createdAt;
}
