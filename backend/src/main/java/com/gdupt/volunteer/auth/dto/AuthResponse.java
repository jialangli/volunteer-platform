package com.gdupt.volunteer.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;

    private String refreshToken;

    /** access token 有效期（秒） */
    private long expiresIn;

    /** refresh token 有效期（秒） */
    private long refreshExpiresIn;
}
