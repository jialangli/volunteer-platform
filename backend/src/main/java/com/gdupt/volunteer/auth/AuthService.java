package com.gdupt.volunteer.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdupt.volunteer.auth.dto.AuthResponse;
import com.gdupt.volunteer.auth.dto.LoginRequest;
import com.gdupt.volunteer.auth.dto.RegisterRequest;
import com.gdupt.volunteer.auth.dto.UserInfoResponse;
import com.gdupt.volunteer.common.BusinessException;
import com.gdupt.volunteer.common.ErrorCode;
import com.gdupt.volunteer.entity.OperationLog;
import com.gdupt.volunteer.entity.Role;
import com.gdupt.volunteer.entity.User;
import com.gdupt.volunteer.entity.UserRole;
import com.gdupt.volunteer.mapper.OperationLogMapper;
import com.gdupt.volunteer.mapper.RoleMapper;
import com.gdupt.volunteer.mapper.UserMapper;
import com.gdupt.volunteer.mapper.UserRoleMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

/**
 * 认证服务：登录（带失败限流）、注册（默认志愿者角色）、刷新、登出、当前用户。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String FAIL_KEY = "auth:fail:";
    private static final String LOCK_KEY = "auth:lock:";
    private static final String BLACKLIST_KEY = "auth:blacklist:";
    private static final int MAX_FAILURES = 10;
    private static final int LOCK_MINUTES = 5;

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final OperationLogMapper operationLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redis;

    @Transactional
    public AuthResponse login(LoginRequest req, String ip) {
        checkLocked(req.getUsername());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            recordFailure(req.getUsername());
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        redis.delete(FAIL_KEY + req.getUsername());
        List<String> roles = loadRoles(user.getId());
        logOperation(user.getId(), "auth", "LOGIN", "USER", user.getId(), ip, null);
        return issueTokens(user, roles);
    }

    @Transactional
    public AuthResponse register(RegisterRequest req, String ip) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRealName(req.getRealName());
        user.setStudentNo(req.getStudentNo());
        user.setStatus(1);
        userMapper.insert(user);
        bindRole(user.getId(), "VOLUNTEER");
        logOperation(user.getId(), "auth", "REGISTER", "USER", user.getId(), ip, null);
        return issueTokens(user, List.of("VOLUNTEER"));
    }

    @Transactional
    public AuthResponse refresh(String refreshToken, String ip) {
        Claims claims;
        try {
            claims = jwtUtil.parse(refreshToken);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        if (!"refresh".equals(claims.get("typ", String.class))) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        String jti = claims.getId();
        if (Boolean.TRUE.equals(redis.hasKey(BLACKLIST_KEY + jti))) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        Long userId = Long.valueOf(claims.getSubject());
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        // 轮换：旧 refresh token 立即作废
        redis.opsForValue().set(BLACKLIST_KEY + jti, "1", Duration.ofDays(jwtUtil.getRefreshExpireSeconds() / 86_400L));
        List<String> roles = loadRoles(userId);
        logOperation(userId, "auth", "REFRESH", "USER", userId, ip, null);
        return issueTokens(user, roles);
    }

    @Transactional
    public void logout(String accessToken, String ip) {
        try {
            Claims claims = jwtUtil.parse(accessToken);
            String jti = claims.getId();
            long remain = jwtUtil.getRemainingSeconds(claims);
            if (remain > 0) {
                redis.opsForValue().set(BLACKLIST_KEY + jti, "1", Duration.ofSeconds(remain));
            }
            logOperation(Long.valueOf(claims.getSubject()), "auth", "LOGOUT", "USER", Long.valueOf(claims.getSubject()), ip, null);
        } catch (Exception e) {
            log.debug("登出时 token 已失效，忽略：{}", e.getMessage());
        }
    }

    public UserInfoResponse me(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return new UserInfoResponse(user.getId(), user.getUsername(), user.getRealName(),
                user.getAvatarUrl(), user.getStatus(), loadRoles(userId));
    }

    // ---------- 私有方法 ----------

    private AuthResponse issueTokens(User user, List<String> roles) {
        String access = jwtUtil.createAccessToken(user.getId(), user.getUsername(), roles);
        String refresh = jwtUtil.createRefreshToken(user.getId());
        return new AuthResponse(access, refresh, jwtUtil.getAccessExpireSeconds(), jwtUtil.getRefreshExpireSeconds());
    }

    private List<String> loadRoles(Long userId) {
        List<UserRole> links = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (links.isEmpty()) {
            return List.of("VOLUNTEER");
        }
        List<Long> roleIds = links.stream().map(UserRole::getRoleId).toList();
        return roleMapper.selectBatchIds(roleIds).stream().map(Role::getCode).toList();
    }

    private void bindRole(Long userId, String roleCode) {
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, roleCode));
        if (role == null) {
            return;
        }
        UserRole link = new UserRole();
        link.setUserId(userId);
        link.setRoleId(role.getId());
        userRoleMapper.insert(link);
    }

    private void checkLocked(String username) {
        String locked = redis.opsForValue().get(LOCK_KEY + username);
        if (locked != null) {
            throw new BusinessException(ErrorCode.RATE_LIMITED, "登录失败次数过多，请 5 分钟后再试");
        }
    }

    private void recordFailure(String username) {
        String key = FAIL_KEY + username;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, Duration.ofMinutes(LOCK_MINUTES));
        }
        if (count != null && count >= MAX_FAILURES) {
            redis.opsForValue().set(LOCK_KEY + username, "1", Duration.ofMinutes(LOCK_MINUTES));
            redis.delete(key);
            log.warn("账号 {} 登录失败次数过多，已锁定 {} 分钟", username, LOCK_MINUTES);
        }
    }

    private void logOperation(Long userId, String module, String action, String targetType, Long targetId, String ip, String detail) {
        OperationLog entry = new OperationLog();
        entry.setUserId(userId);
        entry.setModule(module);
        entry.setAction(action);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId);
        entry.setDetail(detail);
        entry.setIp(ip);
        operationLogMapper.insert(entry);
    }
}
