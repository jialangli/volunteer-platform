package com.gdupt.volunteer.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * JWT 工具：签发 access token（typ=access，含角色）与 refresh token（typ=refresh）。
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final int accessExpireMinutes;
    private final int refreshExpireDays;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.access-expire-minutes}") int accessExpireMinutes,
                   @Value("${app.jwt.refresh-expire-days}") int refreshExpireDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpireMinutes = accessExpireMinutes;
        this.refreshExpireDays = refreshExpireDays;
    }

    public String createAccessToken(Long userId, String username, List<String> roles) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("roles", roles)
                .claim("typ", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpireMinutes * 60_000L))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(Long userId) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("typ", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpireDays * 86_400_000L))
                .signWith(key)
                .compact();
    }

    /** 解析并校验签名/过期，失败抛 JwtException */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public long getRemainingSeconds(Claims claims) {
        long millis = claims.getExpiration().getTime() - System.currentTimeMillis();
        return Math.max(0, millis / 1000);
    }

    public int getAccessExpireSeconds() {
        return accessExpireMinutes * 60;
    }

    public int getRefreshExpireSeconds() {
        return refreshExpireDays * 86_400;
    }
}
