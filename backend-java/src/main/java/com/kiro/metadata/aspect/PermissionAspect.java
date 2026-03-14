package com.kiro.metadata.aspect;

import com.kiro.metadata.annotation.RequireRole;
import com.kiro.metadata.entity.UserRole;
import com.kiro.metadata.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 权限验证切面
 * 
 * 拦截带有 @RequireRole 注解的方法,进行权限验证
 * 
 * @author Kiro
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {
    
    private final PermissionService permissionService;
    
    /**
     * 权限验证切面
     * 
     * @param joinPoint 切入点
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("@annotation(com.kiro.metadata.annotation.RequireRole)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireRole requireRole = method.getAnnotation(RequireRole.class);
        
        if (requireRole != null) {
            UserRole requiredRole = requireRole.value();
            boolean hasPermission = permissionService.checkPermission(requiredRole);
            
            if (!hasPermission) {
                String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
                log.error("权限不足,无法访问方法: {}, 需要角色: {}", methodName, requiredRole);
                throw new AccessDeniedException("权限不足,需要 " + requiredRole + " 角色");
            }
        }
        
        return joinPoint.proceed();
    }
}
