package com.kiro.metadata.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token 响应 DTO
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenResponse {
    
    /**
     * 访问令牌
     */
    private String accessToken;
    
    /**
     * 刷新令牌
     */
    private String refreshToken;
    
    /**
     * 令牌类型(默认: Bearer)
     */
    private String tokenType;
    
    /**
     * 过期时间(秒)
     */
    private Long expiresIn;
}
