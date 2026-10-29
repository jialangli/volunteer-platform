package com.gdupt.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String passwordHash;

    private String realName;

    private String studentNo;

    /** 手机号：安全模块上线后改为 AES 加密存储 */
    private String phone;

    private String avatarUrl;

    /** 1=正常 0=禁用 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 软删除标记（预留，后续管理模块使用） */
    private LocalDateTime deletedAt;
}
