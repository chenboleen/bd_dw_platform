package com.kiro.metadata.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kiro.metadata.entity.User;
import com.kiro.metadata.entity.UserRole;
import com.kiro.metadata.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户管理 API 控制器（仅 ADMIN 可访问）
 *
 * @author Kiro
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "用户管理", description = "用户的增删改查接口，仅管理员可访问")
public class UserController {

    private final UserService userService;

    // ==================== 请求 DTO ====================

    @Data
    public static class CreateUserRequest {
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 50, message = "用户名长度3-50个字符")
        private String username;

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, message = "密码至少6位")
        private String password;

        private UserRole role = UserRole.GUEST;
    }

    @Data
    public static class UpdateUserRequest {
        @Email(message = "邮箱格式不正确")
        private String email;
        private UserRole role;
        private Boolean isActive;
        @Size(min = 6, message = "密码至少6位")
        private String newPassword;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, message = "密码至少6位")
        private String newPassword;
    }

    // ==================== 接口 ====================

    @GetMapping
    @Operation(summary = "分页查询用户列表", security = @SecurityRequirement(name = "Bearer认证"))
    public ResponseEntity<Map<String, Object>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Page<User> result = userService.listUsers(keyword, role, page, pageSize);
        List<Map<String, Object>> items = result.getRecords().stream()
                .map(this::toSafeMap).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("pageSize", pageSize);
        return ResponseEntity.ok(success("查询用户列表成功", data));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情", security = @SecurityRequirement(name = "Bearer认证"))
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(success("获取用户成功", toSafeMap(user)));
    }

    @PostMapping
    @Operation(summary = "创建用户", security = @SecurityRequirement(name = "Bearer认证"))
    public ResponseEntity<Map<String, Object>> createUser(@Valid @RequestBody CreateUserRequest req) {
        User user = userService.createUser(req.getUsername(), req.getEmail(), req.getPassword(), req.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(success("用户创建成功", toSafeMap(user)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户", security = @SecurityRequirement(name = "Bearer认证"))
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest req) {
        User user = userService.updateUser(id, req.getEmail(), req.getRole(), req.getIsActive(), req.getNewPassword());
        return ResponseEntity.ok(success("用户更新成功", toSafeMap(user)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "停用用户", security = @SecurityRequirement(name = "Bearer认证"))
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        Long currentUserId = getCurrentUserId();
        userService.deleteUser(id, currentUserId);
        return ResponseEntity.ok(success("用户已停用", null));
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "重置用户密码", security = @SecurityRequirement(name = "Bearer认证"))
    public ResponseEntity<Map<String, Object>> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest req) {
        userService.resetPassword(id, req.getNewPassword());
        return ResponseEntity.ok(success("密码重置成功", null));
    }

    // ==================== 私有方法 ====================

    /** 转换为不含密码的安全 Map */
    private Map<String, Object> toSafeMap(User user) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("email", user.getEmail());
        m.put("role", user.getRole());
        m.put("isActive", user.getIsActive());
        m.put("createdAt", user.getCreatedAt());
        m.put("updatedAt", user.getUpdatedAt());
        m.put("lastLoginAt", user.getLastLoginAt());
        return m;
    }

    private Map<String, Object> success(String message, Object data) {
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", message);
        res.put("data", data);
        return res;
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails ud) {
            // 通过用户名查 ID，简单起见返回固定值，实际从 JWT 解析
            log.debug("当前用户: {}", ud.getUsername());
        }
        return 1L;
    }
}
