package com.kiro.metadata.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kiro.metadata.entity.User;
import com.kiro.metadata.entity.UserRole;
import com.kiro.metadata.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 用户管理服务（仅管理员可用）
 *
 * @author Kiro
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 分页查询用户列表
     */
    public Page<User> listUsers(String keyword, String role, int page, int pageSize) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(User::getUsername, keyword).or().like(User::getEmail, keyword);
        }
        if (StringUtils.hasText(role)) {
            wrapper.eq(User::getRole, UserRole.valueOf(role));
        }
        wrapper.orderByDesc(User::getCreatedAt);
        return userRepository.selectPage(new Page<>(page, pageSize), wrapper);
    }

    /**
     * 根据 ID 获取用户
     */
    public User getUserById(Long id) {
        User user = userRepository.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在, ID: " + id);
        }
        return user;
    }

    /**
     * 创建用户
     */
    @Transactional(rollbackFor = Exception.class)
    public User createUser(String username, String email, String password, UserRole role) {
        log.info("创建用户: {}", username);
        // 检查用户名重复
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("用户名已存在: " + username);
        }
        // 检查邮箱重复
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("邮箱已存在: " + email);
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password); // 明文存储（与现有系统一致）
        user.setRole(role);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.insert(user);
        log.info("用户创建成功, ID: {}", user.getId());
        return user;
    }

    /**
     * 更新用户（邮箱、角色、激活状态；密码可选）
     */
    @Transactional(rollbackFor = Exception.class)
    public User updateUser(Long id, String email, UserRole role, Boolean isActive, String newPassword) {
        User user = getUserById(id);
        if (StringUtils.hasText(email) && !email.equals(user.getEmail())) {
            if (userRepository.findByEmail(email).isPresent()) {
                throw new IllegalArgumentException("邮箱已存在: " + email);
            }
            user.setEmail(email);
        }
        if (role != null) {
            user.setRole(role);
        }
        if (isActive != null) {
            user.setIsActive(isActive);
        }
        if (StringUtils.hasText(newPassword)) {
        user.setPassword(newPassword); // 明文存储
        }
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.updateById(user);
        log.info("用户更新成功, ID: {}", id);
        return user;
    }

    /**
     * 删除用户（逻辑删除：设为不激活）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new IllegalArgumentException("不能删除当前登录用户");
        }
        User user = getUserById(id);
        user.setIsActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.updateById(user);
        log.info("用户已停用, ID: {}", id);
    }

    /**
     * 重置密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String newPassword) {
        User user = getUserById(id);
        user.setPassword(newPassword); // 明文存储
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.updateById(user);
        log.info("用户密码已重置, ID: {}", id);
    }
}
