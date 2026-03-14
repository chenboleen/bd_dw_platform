package com.kiro.metadata.service;

import com.kiro.metadata.dto.request.LoginRequest;
import com.kiro.metadata.dto.response.TokenResponse;
import com.kiro.metadata.dto.response.UserResponse;
import com.kiro.metadata.entity.User;
import com.kiro.metadata.repository.UserRepository;
import com.kiro.metadata.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 认证服务
 * 
 * 处理用户登录、登出、Token 刷新等认证相关操作
 * 
 * @author Kiro
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    
    /**
     * 用户登录
     * 
     * @param loginRequest 登录请求
     * @return Token 响应
     */
    @Transactional
    public TokenResponse login(LoginRequest loginRequest) {
        // 认证用户
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // 生成 Token
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(loginRequest.getUsername());
        
        // 更新最后登录时间
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.updateById(user);
        
        log.info("用户登录成功: {}", loginRequest.getUsername());
        
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationInSeconds())
                .build();
    }
    
    /**
     * 用户登出
     * 
     * 注意: 由于使用无状态 JWT,登出只需要客户端删除 Token
     * 服务端不需要特殊处理
     */
    public void logout() {
        SecurityContextHolder.clearContext();
        log.info("用户登出成功");
    }
    
    /**
     * 刷新 Token
     * 
     * @param refreshToken 刷新令牌
     * @return 新的 Token 响应
     */
    public TokenResponse refreshToken(String refreshToken) {
        // 验证刷新令牌
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("无效的刷新令牌");
        }
        
        // 从刷新令牌中获取用户名
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        
        // 验证用户是否存在且激活
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        
        if (!user.getIsActive()) {
            throw new IllegalArgumentException("用户已被禁用");
        }
        
        // 生成新的 Token
        String newAccessToken = jwtTokenProvider.generateAccessToken(username);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);
        
        log.info("Token 刷新成功: {}", username);
        
        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationInSeconds())
                .build();
    }
    
    /**
     * 获取当前登录用户信息
     * 
     * @return 用户响应
     */
    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("用户未登录");
        }
        
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
