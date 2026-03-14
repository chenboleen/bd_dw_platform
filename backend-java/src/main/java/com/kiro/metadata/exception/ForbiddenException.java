package com.kiro.metadata.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * 权限不足异常
 * 当用户没有权限执行某个操作时抛出
 * 
 * @author Kiro
 */
public class ForbiddenException extends BusinessException {
    
    /**
     * 构造权限不足异常
     * 
     * @param message 错误消息
     */
    public ForbiddenException(String message) {
        super("FORBIDDEN", message, HttpStatus.FORBIDDEN);
    }
    
    /**
     * 构造权限不足异常(带操作和资源信息)
     * 
     * @param action 操作
     * @param resource 资源
     */
    public ForbiddenException(String action, String resource) {
        super("FORBIDDEN", 
              String.format("您没有权限对 %s 执行 %s 操作", resource, action), 
              HttpStatus.FORBIDDEN,
              Map.of("action", action, "resource", resource));
    }
    
    /**
     * 构造权限不足异常(默认消息)
     */
    public ForbiddenException() {
        super("FORBIDDEN", "您没有权限执行此操作", HttpStatus.FORBIDDEN);
    }
}
