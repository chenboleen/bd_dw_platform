package com.kiro.metadata.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * 业务异常基类
 * 所有业务相关的异常都应继承此类
 * 
 * @author Kiro
 */
@Getter
public class BusinessException extends RuntimeException {
    
    /**
     * 错误码
     */
    private final String errorCode;
    
    /**
     * HTTP 状态码
     */
    private final HttpStatus httpStatus;
    
    /**
     * 详细信息
     */
    private final Map<String, Object> details;
    
    /**
     * 构造业务异常
     * 
     * @param errorCode 错误码
     * @param message 错误消息
     * @param httpStatus HTTP 状态码
     */
    public BusinessException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.details = new HashMap<>();
    }
    
    /**
     * 构造业务异常(带详细信息)
     * 
     * @param errorCode 错误码
     * @param message 错误消息
     * @param httpStatus HTTP 状态码
     * @param details 详细信息
     */
    public BusinessException(String errorCode, String message, 
                           HttpStatus httpStatus, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.details = details != null ? details : new HashMap<>();
    }
}
