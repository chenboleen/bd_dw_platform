package com.kiro.metadata.service;

import com.kiro.metadata.entity.User;
import com.kiro.metadata.entity.UserRole;
import com.kiro.metadata.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 权限验证服务
 * 
 * 提供权限检查功能
 * 
 * @author Kiro
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {
    
    private final UserRepository userRepository;
    
    /**
     * 检查当前用户是否具有指定角色
     * 
     * @param requiredRole 需要的角色
     * @return 是否具有权限
     */
    public boolean checkPermission(UserRole requiredRole) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("用户未登录,权限检查失败");
            return false;
        }
        
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
        
        UserRole userRole = user.getRole();
        
        // 权限层级: ADMIN > DEVELOPER > GUEST
        boolean hasPermission = switch (requiredRole) {
            case ADMIN -> userRole == UserRole.ADMIN;
            case DEVELOPER -> userRole == UserRole.ADMIN || userRole == UserRole.DEVELOPER;
            case GUEST -> true; // 所有角色都可以访问 GUEST 级别的资源
        };
        
        if (!hasPermission) {
            log.warn("用户 {} 权限不足,需要 {} 角色,当前角色为 {}", username, requiredRole, userRole);
        }
        
        return hasPermission;
    }
    
    /**
     * 获取当前用户
     * 
     * @return 当前用户
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("用户未登录");
        }
        
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
    }
    
    /**
     * 检查当前用户是否是管理员
     * 
     * @return 是否是管理员
     */
    public boolean isAdmin() {
        return checkPermission(UserRole.ADMIN);
    }
    
    /**
     * 检查当前用户是否是开发者或管理员
     * 
     * @return 是否是开发者或管理员
     */
    public boolean isDeveloperOrAdmin() {
        return checkPermission(UserRole.DEVELOPER);
    }
}
