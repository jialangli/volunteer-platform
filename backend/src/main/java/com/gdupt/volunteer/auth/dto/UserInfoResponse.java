package com.gdupt.volunteer.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class UserInfoResponse {

    private Long id;

    private String username;

    private String realName;

    private String avatarUrl;

    private Integer status;

    private List<String> roles;
}
