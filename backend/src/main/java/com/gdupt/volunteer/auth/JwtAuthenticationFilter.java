package com.gdupt.volunteer.auth;

import com.gdupt.volunteer.security.LoginUser;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器：解析 Authorization: Bearer <token>，
 * 校验签名/过期/黑名单后，将 LoginUser 写入 SecurityContext。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtUtil.parse(token);
                if ("access".equals(claims.get("typ", String.class))) {
                    String jti = claims.getId();
                    Boolean banned = redisTemplate.hasKey("auth:blacklist:" + jti);
                    if (!Boolean.TRUE.equals(banned)) {
                        Long userId = Long.valueOf(claims.getSubject());
                        String username = claims.get("username", String.class);
                        @SuppressWarnings("unchecked")
                        List<String> roles = (List<String>) claims.get("roles", List.class);
                        LoginUser loginUser = new LoginUser(userId, username, roles);
                        List<SimpleGrantedAuthority> authorities = roles.stream()
                                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                                .map(a -> (SimpleGrantedAuthority) a)
                                .toList();
                        var authentication = new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (Exception e) {
                // 无效 token：不设置认证，交由后续过滤器/入口点返回 401
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
