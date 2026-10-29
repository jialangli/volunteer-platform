package com.gdupt.volunteer.security;

import java.util.List;

/**
 * 登录用户主体：放入 Spring SecurityContext 的 principal。
 */
public record LoginUser(Long id, String username, List<String> roles) {
}
