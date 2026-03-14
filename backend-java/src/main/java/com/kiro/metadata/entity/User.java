package com.kiro.metadata.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("users")
public class User {
    
    /**
     * 用户ID - 主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户名 - 唯一
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    @TableField("username")
    private String username;
    
    /**
     * 邮箱 - 唯一
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @TableField("email")
    private String email;
    
    /**
     * 密码(明文存储 - 注意:生产环境应使用加密存储)
     */
    @NotBlank(message = "密码不能为空")
    @TableField("password")
    private String password;
    
    /**
     * 用户角色
     */
    @TableField("role")
    private UserRole role;
    
    /**
     * 是否激活
     */
    @TableField("is_active")
    private Boolean isActive;
    
    /**
     * 创建时间 - 自动填充
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间 - 自动填充
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    /**
     * 最后登录时间
     */
    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;
}
