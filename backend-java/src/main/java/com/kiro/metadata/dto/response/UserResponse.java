package com.kiro.metadata.dto.response;

import com.kiro.metadata.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户响应 DTO
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    
    /**
     * 用户ID
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 角色
     */
    private UserRole role;
    
    /**
     * 是否激活
     */
    private Boolean isActive;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
