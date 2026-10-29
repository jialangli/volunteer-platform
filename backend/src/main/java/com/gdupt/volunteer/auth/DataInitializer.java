package com.gdupt.volunteer.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdupt.volunteer.entity.Role;
import com.gdupt.volunteer.entity.User;
import com.gdupt.volunteer.entity.UserRole;
import com.gdupt.volunteer.mapper.RoleMapper;
import com.gdupt.volunteer.mapper.UserMapper;
import com.gdupt.volunteer.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动时初始化演示账号（幂等，重复启动不会重复创建）：
 * - admin / admin123    管理员
 * - organizer / organizer123  组织者
 * 志愿者账号请通过注册接口创建。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        ensureUser("admin", "admin123", "系统管理员", "ADMIN");
        ensureUser("organizer", "organizer123", "演示组织者", "ORGANIZER");
    }

    private void ensureUser(String username, String rawPassword, String realName, String roleCode) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (count != null && count > 0) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRealName(realName);
        user.setStatus(1);
        userMapper.insert(user);

        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, roleCode));
        if (role != null) {
            UserRole link = new UserRole();
            link.setUserId(user.getId());
            link.setRoleId(role.getId());
            userRoleMapper.insert(link);
        }
        log.info("已初始化演示账号：{} / {}（角色 {}）", username, rawPassword, roleCode);
    }
}
