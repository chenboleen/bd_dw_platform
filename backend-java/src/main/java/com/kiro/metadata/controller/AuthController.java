package com.kiro.metadata.controller;

import com.kiro.metadata.dto.request.LoginRequest;
import com.kiro.metadata.dto.request.RefreshTokenRequest;
import com.kiro.metadata.dto.response.TokenResponse;
import com.kiro.metadata.dto.response.UserResponse;
import com.kiro.metadata.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户认证 API 控制器
 * 
 * 提供用户登录、登出、令牌刷新和获取当前用户信息等接口
 * 
 * @author Kiro
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户认证和授权相关接口")
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     * 
     * 验证用户名和密码，成功后返回 JWT 访问令牌和刷新令牌
     * 
     * @param loginRequest 登录请求（用户名、密码）
     * @return JWT 令牌响应
     */
    @PostMapping("/login")
    @Operation(
        summary = "用户登录",
        description = "使用用户名和密码进行登录，成功后返回 JWT 访问令牌和刷新令牌"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "登录成功",
            content = @Content(schema = @Schema(implementation = TokenResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "用户名或密码错误")
    })
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest loginRequest) {
        log.info("用户登录请求: {}", loginRequest.getUsername());
        TokenResponse tokenResponse = authService.login(loginRequest);
        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * 用户登出
     * 
     * 清除服务端认证上下文，客户端需自行删除本地存储的令牌
     * 
     * @param token Authorization 请求头中的 Bearer 令牌
     * @return 登出成功消息
     */
    @PostMapping("/logout")
    @Operation(
        summary = "用户登出",
        description = "清除认证上下文，客户端需自行删除本地存储的令牌",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "登出成功"),
        @ApiResponse(responseCode = "401", description = "未认证或令牌无效")
    })
    public ResponseEntity<Map<String, String>> logout(
            @Parameter(description = "Bearer 令牌", required = true)
            @RequestHeader("Authorization") String token) {
        log.info("用户登出请求");
        authService.logout();
        return ResponseEntity.ok(Map.of("message", "登出成功"));
    }

    /**
     * 刷新访问令牌
     * 
     * 使用有效的刷新令牌获取新的访问令牌和刷新令牌
     * 
     * @param refreshTokenRequest 包含刷新令牌的请求体
     * @return 新的 JWT 令牌响应
     */
    @PostMapping("/refresh")
    @Operation(
        summary = "刷新访问令牌",
        description = "使用有效的刷新令牌获取新的访问令牌，旧令牌将失效"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "令牌刷新成功",
            content = @Content(schema = @Schema(implementation = TokenResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "刷新令牌无效或已过期"),
        @ApiResponse(responseCode = "401", description = "用户不存在或已被禁用")
    })
    public ResponseEntity<TokenResponse> refresh(
            @RequestBody @Valid RefreshTokenRequest refreshTokenRequest) {
        log.info("令牌刷新请求");
        TokenResponse tokenResponse = authService.refreshToken(refreshTokenRequest.getRefreshToken());
        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * 获取当前登录用户信息
     * 
     * 返回当前已认证用户的详细信息
     * 
     * @param userDetails Spring Security 注入的当前用户详情
     * @return 当前用户信息
     */
    @GetMapping("/me")
    @Operation(
        summary = "获取当前用户信息",
        description = "返回当前已认证用户的详细信息，包括用户名、邮箱、角色等",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "获取成功",
            content = @Content(schema = @Schema(implementation = UserResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "未认证或令牌无效")
    })
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("获取当前用户信息: {}", userDetails != null ? userDetails.getUsername() : "未知");
        UserResponse userResponse = authService.getCurrentUser();
        return ResponseEntity.ok(userResponse);
    }
}
