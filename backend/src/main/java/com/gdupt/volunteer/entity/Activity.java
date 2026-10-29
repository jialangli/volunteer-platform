package com.gdupt.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity")
public class Activity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    private Long typeId;

    /** 组织者用户ID */
    private Long organizerId;

    private String location;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 签到开放时间 */
    private LocalDateTime signinStart;

    /** 签到截止时间 */
    private LocalDateTime signinEnd;

    /** 签退截止时间 */
    private LocalDateTime signoutEnd;

    /** 名额，0=不限 */
    private Integer capacity;

    /** 剩余名额（防超卖原子扣减） */
    private Integer capacityRemaining;

    /** DRAFT/RECRUITING/ONGOING/FINISHED/ARCHIVED/CANCELLED */
    private String status;

    private String coverUrl;

    /** 乐观锁版本号（自定义 SQL 手动维护） */
    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 软删除标记（查询需显式过滤 deleted_at IS NULL） */
    private LocalDateTime deletedAt;
}
