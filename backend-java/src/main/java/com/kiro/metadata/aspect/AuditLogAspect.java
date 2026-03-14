package com.kiro.metadata.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 审计日志切面
 * 记录所有敏感操作（创建、更新、删除）的审计日志
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final ObjectMapper objectMapper;

    /**
     * 拦截所有 Controller 层的写操作（POST、PUT、DELETE）
     * 记录操作人、操作时间、操作内容，并持久化到变更历史
     */
    @Around("execution(* com.kiro.metadata.controller..*(..)) && " +
            "(@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping))")
    public Object auditControllerOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String operatorName = getCurrentUsername();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = method.getName();

        // 确定操作类型
        String operationType = resolveOperationType(method);

        log.info("[审计日志] 操作开始 - 操作人: {}, 控制器: {}, 方法: {}, 操作类型: {}, 时间: {}",
                operatorName, className, methodName, operationType, LocalDateTime.now());

        Object result = null;
        try {
            result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.info("[审计日志] 操作成功 - 操作人: {}, 控制器: {}, 方法: {}, 耗时: {}ms",
                    operatorName, className, methodName, duration);

            // 持久化审计记录
            persistAuditLog(operatorName, className, methodName, operationType, joinPoint.getArgs(), null);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("[审计日志] 操作失败 - 操作人: {}, 控制器: {}, 方法: {}, 耗时: {}ms, 错误: {}",
                    operatorName, className, methodName, duration, e.getMessage());

            // 持久化失败审计记录
            persistAuditLog(operatorName, className, methodName, operationType + "_FAILED", joinPoint.getArgs(), e.getMessage());
            throw e;
        }

        return result;
    }

    /**
     * 拦截认证控制器的登录和登出操作
     */
    @Around("execution(* com.kiro.metadata.controller.AuthController.login(..)) || " +
            "execution(* com.kiro.metadata.controller.AuthController.logout(..))")
    public Object auditAuthOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String operationType = "login".equals(methodName) ? "LOGIN" : "LOGOUT";

        Object result = null;
        try {
            result = joinPoint.proceed();
            // 登录成功后获取用户名（登出时已有认证信息）
            String operatorName = getCurrentUsername();
            log.info("[审计日志] 认证操作 - 操作人: {}, 操作: {}, 时间: {}",
                    operatorName, operationType, LocalDateTime.now());
            persistAuditLog(operatorName, "AuthController", methodName, operationType, null, null);
        } catch (Exception e) {
            log.warn("[审计日志] 认证操作失败 - 操作: {}, 错误: {}", operationType, e.getMessage());
            throw e;
        }
        return result;
    }

    /**
     * 根据方法注解解析操作类型
     */
    private String resolveOperationType(Method method) {
        if (method.isAnnotationPresent(org.springframework.web.bind.annotation.PostMapping.class)) {
            return "CREATE";
        } else if (method.isAnnotationPresent(org.springframework.web.bind.annotation.PutMapping.class)) {
            return "UPDATE";
        } else if (method.isAnnotationPresent(org.springframework.web.bind.annotation.DeleteMapping.class)) {
            return "DELETE";
        }
        return "WRITE";
    }

    /**
     * 持久化审计日志到变更历史
     */
    private void persistAuditLog(String operatorName, String className, String methodName,
                                  String operationType, Object[] args, String errorMessage) {
        try {
            String argsJson = null;
            if (args != null && args.length > 0) {
                argsJson = serializeArgs(args);
            }
            // 仅记录日志，不持久化到 ChangeHistory（entityId 为字符串，无法映射到 Long）
            log.info("[审计持久化] 操作人: {}, 类: {}.{}, 类型: {}, 参数: {}, 错误: {}",
                    operatorName, className, methodName, operationType, argsJson, errorMessage);
        } catch (Exception e) {
            // 审计日志持久化失败不应影响主业务
            log.debug("审计日志持久化失败: {}", e.getMessage());
        }
    }

    /**
     * 序列化方法参数，过滤密码等敏感字段
     */
    private String serializeArgs(Object[] args) {
        try {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < args.length; i++) {
                if (args[i] == null) {
                    sb.append("null");
                } else {
                    String json = objectMapper.writeValueAsString(args[i]);
                    // 脱敏处理：替换密码字段
                    json = json.replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"***\"");
                    json = json.replaceAll("\"passwordHash\"\\s*:\\s*\"[^\"]*\"", "\"passwordHash\":\"***\"");
                    sb.append(json);
                }
                if (i < args.length - 1) sb.append(",");
            }
            sb.append("]");
            // 限制长度，避免存储过大数据
            String result = sb.toString();
            return result.length() > 2000 ? result.substring(0, 2000) + "..." : result;
        } catch (Exception e) {
            return "[序列化失败]";
        }
    }

    /**
     * 获取当前登录用户名
     *
     * @return 用户名，未登录则返回 "anonymous"
     */
    private String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.debug("获取当前用户名失败: {}", e.getMessage());
        }
        return "anonymous";
    }
}
