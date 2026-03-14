package com.kiro.metadata.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 错误响应 DTO
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    
    /**
     * 错误码
     */
    private String code;
    
    /**
     * 错误消息
     */
    private String message;
    
    /**
     * 详细信息
     */
    private String details;
    
    /**
     * 时间戳
     */
    private LocalDateTime timestamp;
    
    /**
     * 请求路径
     */
    private String path;
}
