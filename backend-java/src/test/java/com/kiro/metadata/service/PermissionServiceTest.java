package com.kiro.metadata.service;

import com.kiro.metadata.entity.User;
import com.kiro.metadata.entity.UserRole;
import com.kiro.metadata.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 权限服务单元测试
 * 
 * @author Kiro
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("权限服务测试")
class PermissionServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private PermissionService permissionService;
    
    @Mock
    private SecurityContext securityContext;
    
    private User adminUser;
    private User developerUser;
    private User guestUser;
    
    @BeforeEach
    void setUp() {
        // 创建测试用户
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setIsActive(true);
        
        developerUser = new User();
        developerUser.setId(2L);
        developerUser.setUsername("developer");
        developerUser.setRole(UserRole.DEVELOPER);
        developerUser.setIsActive(true);
        
        guestUser = new User();
        guestUser.setId(3L);
        guestUser.setUsername("guest");
        guestUser.setRole(UserRole.GUEST);
        guestUser.setIsActive(true);
        
        SecurityContextHolder.setContext(securityContext);
    }
    
    @Test
    @DisplayName("管理员可以访问所有资源")
    void testAdminCanAccessAll() {
        // 设置管理员认证
        setupAuthentication("admin", adminUser);
        
        // 验证权限
        assertTrue(permissionService.checkPermission(UserRole.ADMIN));
        assertTrue(permissionService.checkPermission(UserRole.DEVELOPER));
        assertTrue(permissionService.checkPermission(UserRole.GUEST));
    }
    
    @Test
    @DisplayName("开发者可以访问开发者和访客资源")
    void testDeveloperCanAccessDeveloperAndGuest() {
        // 设置开发者认证
        setupAuthentication("developer", developerUser);
        
        // 验证权限
        assertFalse(permissionService.checkPermission(UserRole.ADMIN));
        assertTrue(permissionService.checkPermission(UserRole.DEVELOPER));
        assertTrue(permissionService.checkPermission(UserRole.GUEST));
    }
    
    @Test
    @DisplayName("访客只能访问访客资源")
    void testGuestCanOnlyAccessGuest() {
        // 设置访客认证
        setupAuthentication("guest", guestUser);
        
        // 验证权限
        assertFalse(permissionService.checkPermission(UserRole.ADMIN));
        assertFalse(permissionService.checkPermission(UserRole.DEVELOPER));
        assertTrue(permissionService.checkPermission(UserRole.GUEST));
    }
    
    @Test
    @DisplayName("未登录用户无权限")
    void testUnauthenticatedUserHasNoPermission() {
        // 设置未认证状态
        when(securityContext.getAuthentication()).thenReturn(null);
        
        // 验证权限
        assertFalse(permissionService.checkPermission(UserRole.ADMIN));
        assertFalse(permissionService.checkPermission(UserRole.DEVELOPER));
        assertFalse(permissionService.checkPermission(UserRole.GUEST));
    }
    
    @Test
    @DisplayName("isAdmin 方法正确判断管理员")
    void testIsAdmin() {
        // 管理员
        setupAuthentication("admin", adminUser);
        assertTrue(permissionService.isAdmin());
        
        // 开发者
        setupAuthentication("developer", developerUser);
        assertFalse(permissionService.isAdmin());
        
        // 访客
        setupAuthentication("guest", guestUser);
        assertFalse(permissionService.isAdmin());
    }
    
    @Test
    @DisplayName("isDeveloperOrAdmin 方法正确判断开发者或管理员")
    void testIsDeveloperOrAdmin() {
        // 管理员
        setupAuthentication("admin", adminUser);
        assertTrue(permissionService.isDeveloperOrAdmin());
        
        // 开发者
        setupAuthentication("developer", developerUser);
        assertTrue(permissionService.isDeveloperOrAdmin());
        
        // 访客
        setupAuthentication("guest", guestUser);
        assertFalse(permissionService.isDeveloperOrAdmin());
    }
    
    @Test
    @DisplayName("getCurrentUser 返回当前用户")
    void testGetCurrentUser() {
        setupAuthentication("admin", adminUser);
        
        User currentUser = permissionService.getCurrentUser();
        
        assertNotNull(currentUser);
        assertEquals("admin", currentUser.getUsername());
        assertEquals(UserRole.ADMIN, currentUser.getRole());
    }
    
    /**
     * 设置认证信息
     */
    private void setupAuthentication(String username, User user) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                username,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    }
}
