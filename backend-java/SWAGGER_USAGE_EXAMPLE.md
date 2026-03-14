# Swagger 注解使用示例

本文档提供了在 Controller 和 DTO 中使用 Swagger 注解的示例代码。

## Controller 注解示例

### 1. 基本 Controller 结构

```java
package com.kiro.metadata.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tables")
@Tag(name = "表元数据", description = "数据表元数据管理接口")
public class TableController {

    @GetMapping
    @Operation(
        summary = "查询表列表",
        description = "分页查询数据表元数据列表，支持按表名、数据库名过滤"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "查询成功",
            content = @Content(schema = @Schema(implementation = PagedResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "请求参数错误"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "未认证或 Token 无效"
        )
    })
    public ResponseEntity<PagedResponse<TableResponse>> getTables(
        @Parameter(description = "页码（从 1 开始）", example = "1")
        @RequestParam(defaultValue = "1") Integer page,
        
        @Parameter(description = "每页大小（20-100）", example = "20")
        @RequestParam(defaultValue = "20") Integer size,
        
        @Parameter(description = "表名（模糊匹配）", example = "user")
        @RequestParam(required = false) String tableName,
        
        @Parameter(description = "数据库名", example = "metadata_db")
        @RequestParam(required = false) String databaseName
    ) {
        // 实现代码
        return ResponseEntity.ok(new PagedResponse<>());
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "获取表详情",
        description = "根据表 ID 获取表的详细元数据信息，包括字段列表"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "查询成功",
            content = @Content(schema = @Schema(implementation = TableResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "表不存在"
        )
    })
    public ResponseEntity<TableResponse> getTable(
        @Parameter(description = "表 ID", required = true, example = "1")
        @PathVariable Long id
    ) {
        // 实现代码
        return ResponseEntity.ok(new TableResponse());
    }

    @PostMapping
    @Operation(
        summary = "创建表元数据",
        description = "创建新的数据表元数据记录"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "创建成功",
            content = @Content(schema = @Schema(implementation = TableResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "请求参数错误"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "无权限执行此操作"
        )
    })
    public ResponseEntity<TableResponse> createTable(
        @Parameter(description = "表元数据创建请求", required = true)
        @RequestBody @Valid TableCreateRequest request
    ) {
        // 实现代码
        return ResponseEntity.status(HttpStatus.CREATED).body(new TableResponse());
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "更新表元数据",
        description = "更新指定表的元数据信息"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "更新成功",
            content = @Content(schema = @Schema(implementation = TableResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "表不存在"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "无权限执行此操作"
        )
    })
    public ResponseEntity<TableResponse> updateTable(
        @Parameter(description = "表 ID", required = true, example = "1")
        @PathVariable Long id,
        
        @Parameter(description = "表元数据更新请求", required = true)
        @RequestBody @Valid TableUpdateRequest request
    ) {
        // 实现代码
        return ResponseEntity.ok(new TableResponse());
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除表元数据",
        description = "删除指定的表元数据记录（软删除）"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "删除成功"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "表不存在"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "无权限执行此操作"
        )
    })
    public ResponseEntity<Void> deleteTable(
        @Parameter(description = "表 ID", required = true, example = "1")
        @PathVariable Long id
    ) {
        // 实现代码
        return ResponseEntity.noContent().build();
    }
}
```

### 2. 认证 Controller 示例

```java
package com.kiro.metadata.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "认证管理", description = "用户认证和授权相关接口")
public class AuthController {

    @PostMapping("/login")
    @Operation(
        summary = "用户登录",
        description = "使用用户名和密码登录，成功后返回 JWT Token"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "登录成功",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "用户名或密码错误"
        )
    })
    public ResponseEntity<LoginResponse> login(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "登录请求",
            required = true,
            content = @Content(schema = @Schema(implementation = LoginRequest.class))
        )
        @RequestBody @Valid LoginRequest request
    ) {
        // 实现代码
        return ResponseEntity.ok(new LoginResponse());
    }

    @PostMapping("/logout")
    @Operation(
        summary = "用户登出",
        description = "登出当前用户，使 Token 失效"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "登出成功"
        )
    })
    public ResponseEntity<Void> logout() {
        // 实现代码
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @Operation(
        summary = "获取当前用户信息",
        description = "获取当前登录用户的详细信息"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "查询成功",
            content = @Content(schema = @Schema(implementation = UserResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "未认证或 Token 无效"
        )
    })
    public ResponseEntity<UserResponse> getCurrentUser() {
        // 实现代码
        return ResponseEntity.ok(new UserResponse());
    }
}
```

## DTO 注解示例

### 1. Request DTO

```java
package com.kiro.metadata.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "表元数据创建请求")
public class TableCreateRequest {

    @Schema(
        description = "表名",
        example = "user_info",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "表名不能为空")
    @Size(max = 100, message = "表名长度不能超过 100 个字符")
    private String tableName;

    @Schema(
        description = "数据库名",
        example = "metadata_db",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "数据库名不能为空")
    @Size(max = 100, message = "数据库名长度不能超过 100 个字符")
    private String databaseName;

    @Schema(
        description = "表类型",
        example = "TABLE",
        allowableValues = {"TABLE", "VIEW", "EXTERNAL_TABLE"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "表类型不能为空")
    private String tableType;

    @Schema(
        description = "表注释",
        example = "用户信息表",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Size(max = 500, message = "表注释长度不能超过 500 个字符")
    private String comment;

    @Schema(
        description = "存储格式",
        example = "PARQUET",
        allowableValues = {"PARQUET", "ORC", "CSV", "JSON"},
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String storageFormat;

    @Schema(
        description = "存储位置",
        example = "/data/warehouse/user_info",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Size(max = 500, message = "存储位置长度不能超过 500 个字符")
    private String location;

    @Schema(
        description = "数据大小（字节）",
        example = "1048576",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Min(value = 0, message = "数据大小不能为负数")
    private Long dataSize;

    @Schema(
        description = "分区字段列表",
        example = "[\"dt\", \"hour\"]",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private List<String> partitionKeys;

    @Schema(
        description = "字段列表",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotEmpty(message = "字段列表不能为空")
    @Valid
    private List<ColumnCreateRequest> columns;
}
```

### 2. Response DTO

```java
package com.kiro.metadata.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "表元数据响应")
public class TableResponse {

    @Schema(description = "表 ID", example = "1")
    private Long id;

    @Schema(description = "表名", example = "user_info")
    private String tableName;

    @Schema(description = "数据库名", example = "metadata_db")
    private String databaseName;

    @Schema(description = "表类型", example = "TABLE")
    private String tableType;

    @Schema(description = "表注释", example = "用户信息表")
    private String comment;

    @Schema(description = "存储格式", example = "PARQUET")
    private String storageFormat;

    @Schema(description = "存储位置", example = "/data/warehouse/user_info")
    private String location;

    @Schema(description = "数据大小（字节）", example = "1048576")
    private Long dataSize;

    @Schema(description = "分区字段列表", example = "[\"dt\", \"hour\"]")
    private List<String> partitionKeys;

    @Schema(description = "创建时间", example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间", example = "2024-01-01T12:00:00")
    private LocalDateTime updatedAt;

    @Schema(description = "创建人", example = "admin")
    private String createdBy;

    @Schema(description = "更新人", example = "admin")
    private String updatedBy;

    @Schema(description = "字段列表")
    private List<ColumnResponse> columns;
}
```

### 3. 分页响应 DTO

```java
package com.kiro.metadata.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "分页响应")
public class PagedResponse<T> {

    @Schema(description = "数据列表")
    private List<T> items;

    @Schema(description = "总记录数", example = "100")
    private Long total;

    @Schema(description = "当前页码", example = "1")
    private Integer page;

    @Schema(description = "每页大小", example = "20")
    private Integer size;

    @Schema(description = "总页数", example = "5")
    private Integer totalPages;

    @Schema(description = "是否有下一页", example = "true")
    private Boolean hasNext;

    @Schema(description = "是否有上一页", example = "false")
    private Boolean hasPrevious;
}
```

### 4. 登录请求 DTO

```java
package com.kiro.metadata.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "登录请求")
public class LoginRequest {

    @Schema(
        description = "用户名",
        example = "admin",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在 3-50 个字符之间")
    private String username;

    @Schema(
        description = "密码",
        example = "password123",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在 6-100 个字符之间")
    private String password;
}
```

### 5. 登录响应 DTO

```java
package com.kiro.metadata.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "访问令牌（JWT Token）", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "令牌类型", example = "Bearer")
    private String tokenType;

    @Schema(description = "过期时间（秒）", example = "86400")
    private Long expiresIn;

    @Schema(description = "用户信息")
    private UserResponse user;
}
```

## 常用注解说明

### Controller 层注解

| 注解 | 用途 | 示例 |
|------|------|------|
| `@Tag` | 标记 Controller 所属的标签 | `@Tag(name = "表元数据", description = "...")` |
| `@Operation` | 描述 API 操作 | `@Operation(summary = "查询表列表", description = "...")` |
| `@Parameter` | 描述请求参数 | `@Parameter(description = "页码", example = "1")` |
| `@ApiResponse` | 描述单个响应 | `@ApiResponse(responseCode = "200", description = "成功")` |
| `@ApiResponses` | 描述多个响应 | `@ApiResponses({...})` |

### DTO 层注解

| 注解 | 用途 | 示例 |
|------|------|------|
| `@Schema` | 描述模型或字段 | `@Schema(description = "用户名", example = "admin")` |
| `requiredMode` | 标记字段是否必需 | `requiredMode = Schema.RequiredMode.REQUIRED` |
| `allowableValues` | 限制字段可选值 | `allowableValues = {"TABLE", "VIEW"}` |
| `example` | 提供示例值 | `example = "user_info"` |

## 最佳实践

1. **使用中文描述**: 所有 description 字段使用中文，便于团队理解
2. **提供示例值**: 为所有字段提供 example，便于 API 测试
3. **详细的错误响应**: 为每个接口定义可能的错误响应码
4. **参数验证**: 结合 `@Valid` 和 Bean Validation 注解进行参数验证
5. **统一响应格式**: 使用统一的响应包装类（如 `ApiResponse<T>`）
6. **版本控制**: 在 URL 中包含 API 版本号（如 `/api/v1/`）

## 参考资源

- [SpringDoc OpenAPI 官方文档](https://springdoc.org/)
- [OpenAPI 3.0 规范](https://swagger.io/specification/)
- [Swagger 注解指南](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations)
