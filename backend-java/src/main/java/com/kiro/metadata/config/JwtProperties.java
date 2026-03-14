package com.kiro.metadata.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置属性类
 * 
 * 从 application.yml 中读取 JWT 相关配置
 * 
 * @author Kiro
 * @since 2024-01-01
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * JWT 签名密钥
     * 必须是足够长的强随机字符串
     */
    private String secret;

    /**
     * 访问令牌过期时间(毫秒)
     * 默认: 86400000 (24小时)
     */
    private Long expiration;

    /**
     * 刷新令牌过期时间(毫秒)
     * 默认: 604800000 (7天)
     */
    private Long refreshExpiration;
}
