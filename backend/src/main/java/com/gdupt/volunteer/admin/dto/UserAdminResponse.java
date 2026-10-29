package com.gdupt.volunteer.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 用户管理列表项 */
@Data
@Builder
public class UserAdminResponse {

    private Long id;

    private String username;

    private String realName;

    private String studentNo;

    private String phone;

    /** 1 正常 / 0 禁用 */
    private Integer status;

    private List<String> roles;

    /** 角色 ID 列表（用于角色编辑回显） */
    private List<Long> roleIds;

    private LocalDateTime createdAt;
}
