package com.kiro.metadata.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kiro.metadata.dto.response.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * JWT 认证入口点
 * 处理未认证用户访问受保护资源的情况
 * 
 * @author Kiro
 */
@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    private final ObjectMapper objectMapper;
    
    public JwtAuthenticationEntryPoint() {
        this.objectMapper = new ObjectMapper();
        // 注册 JavaTimeModule 以支持 LocalDateTime 序列化
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * 处理认证失败的请求
     * 当用户未认证或认证失败时,返回 401 错误响应
     * 
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param authException 认证异常
     * @throws IOException IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    public void commence(HttpServletRequest request, 
                        HttpServletResponse response,
                        AuthenticationException authException) 
            throws IOException, ServletException {
        
        // 记录认证失败日志
        log.warn("认证失败 - 请求路径: {}, 错误信息: {}, IP: {}", 
                request.getRequestURI(), 
                authException.getMessage(),
                getClientIp(request));
        
        // 构建错误响应
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("UNAUTHORIZED")
                .message("认证失败,请先登录")
                .details(authException.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        // 设置响应状态码和内容类型
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        // 写入 JSON 响应
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
    
    /**
     * 获取客户端真实 IP 地址
     * 考虑代理和负载均衡的情况
     * 
     * @param request HTTP 请求
     * @return 客户端 IP 地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果有多个代理,取第一个 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
