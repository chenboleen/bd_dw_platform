package com.kiro.metadata.controller;

import com.kiro.metadata.dto.request.ColumnCreateRequest;
import com.kiro.metadata.dto.request.ColumnUpdateRequest;
import com.kiro.metadata.dto.request.ReorderColumnsRequest;
import com.kiro.metadata.dto.response.ColumnResponse;
import com.kiro.metadata.entity.ColumnMetadata;
import com.kiro.metadata.service.ColumnService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 字段元数据 API 控制器
 *
 * 提供字段元数据的增删改及排序接口
 * 支持基于角色的权限控制（DEVELOPER/ADMIN 可增改，ADMIN 可删除）
 *
 * @author Kiro
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/columns")
@RequiredArgsConstructor
@Tag(name = "字段元数据管理", description = "字段元数据的增删改及排序接口")
public class ColumnController {

    private final ColumnService columnService;

    /**
     * 创建字段元数据
     *
     * @param request 字段创建请求
     * @return 创建的字段信息（201 Created）
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    @Operation(
        summary = "创建字段元数据",
        description = "为指定表创建新的字段元数据记录，需要 DEVELOPER 或 ADMIN 角色",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "创建成功",
            content = @Content(schema = @Schema(implementation = ColumnResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "关联的表不存在")
    })
    public ResponseEntity<ColumnResponse> createColumn(
            @Valid @RequestBody ColumnCreateRequest request) {
        log.info("创建字段元数据请求, 表ID: {}, 字段名: {}", request.getTableId(), request.getColumnName());

        // 将请求 DTO 转换为实体
        ColumnMetadata column = convertToEntity(request);

        // 调用服务创建字段
        Long userId = getCurrentUserId();
        ColumnMetadata created = columnService.createColumn(column, userId);

        // 转换为响应 DTO
        ColumnResponse response = convertToResponse(created);

        log.info("字段元数据创建成功, ID: {}", created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 更新字段元数据
     *
     * @param id      字段ID
     * @param request 更新请求
     * @return 更新后的字段信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    @Operation(
        summary = "更新字段元数据",
        description = "更新字段的数据类型、可空性、分区键标识及描述信息，需要 DEVELOPER 或 ADMIN 角色",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "更新成功",
            content = @Content(schema = @Schema(implementation = ColumnResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "字段不存在")
    })
    public ResponseEntity<ColumnResponse> updateColumn(
            @Parameter(description = "字段ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody ColumnUpdateRequest request) {
        log.info("更新字段元数据请求, ID: {}", id);

        // 将请求 DTO 转换为实体（仅包含可更新字段）
        ColumnMetadata updates = new ColumnMetadata();
        updates.setDataType(request.getDataType());
        updates.setIsNullable(request.getIsNullable());
        updates.setIsPartitionKey(request.getIsPartitionKey());
        updates.setDescription(request.getDescription());

        // 调用服务更新
        Long userId = getCurrentUserId();
        ColumnMetadata updated = columnService.updateColumn(id, updates, userId);

        // 转换为响应 DTO
        ColumnResponse response = convertToResponse(updated);

        log.info("字段元数据更新成功, ID: {}", id);
        return ResponseEntity.ok(response);
    }

    /**
     * 删除字段元数据
     *
     * @param id 字段ID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "删除字段元数据",
        description = "删除指定字段的元数据记录，需要 ADMIN 角色",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "删除成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要 ADMIN 角色"),
        @ApiResponse(responseCode = "404", description = "字段不存在")
    })
    public ResponseEntity<Void> deleteColumn(
            @Parameter(description = "字段ID", required = true)
            @PathVariable Long id) {
        log.info("删除字段元数据请求, ID: {}", id);

        Long userId = getCurrentUserId();
        columnService.deleteColumn(id, userId);

        log.info("字段元数据删除成功, ID: {}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 批量调整字段顺序
     *
     * @param request 排序请求（包含表ID和字段ID列表）
     * @return 操作结果
     */
    @PutMapping("/reorder")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    @Operation(
        summary = "批量调整字段顺序",
        description = "按照请求中字段ID列表的顺序重新排列字段，需要 DEVELOPER 或 ADMIN 角色",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "排序成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "字段不存在或不属于指定表")
    })
    public ResponseEntity<Map<String, Object>> reorderColumns(
            @RequestBody ReorderColumnsRequest request) {
        log.info("调整字段顺序请求, 表ID: {}, 字段数量: {}",
                request.getTableId(), request.getColumnIds().size());

        Long userId = getCurrentUserId();
        columnService.reorderColumns(request.getTableId(), request.getColumnIds(), userId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "字段顺序调整成功");
        result.put("tableId", request.getTableId());
        result.put("count", request.getColumnIds().size());

        log.info("字段顺序调整成功, 表ID: {}", request.getTableId());
        return ResponseEntity.ok(result);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 将创建请求 DTO 转换为实体
     *
     * @param request 创建请求
     * @return 字段元数据实体
     */
    private ColumnMetadata convertToEntity(ColumnCreateRequest request) {
        ColumnMetadata column = new ColumnMetadata();
        column.setTableId(request.getTableId());
        column.setColumnName(request.getColumnName());
        column.setDataType(request.getDataType());
        column.setColumnOrder(request.getColumnOrder());
        column.setIsNullable(request.getIsNullable() != null ? request.getIsNullable() : true);
        column.setIsPartitionKey(request.getIsPartitionKey() != null ? request.getIsPartitionKey() : false);
        column.setDescription(request.getDescription());
        return column;
    }

    /**
     * 将字段元数据实体转换为响应 DTO
     *
     * @param column 字段元数据实体
     * @return 字段响应 DTO
     */
    private ColumnResponse convertToResponse(ColumnMetadata column) {
        return ColumnResponse.builder()
            .id(column.getId())
            .tableId(column.getTableId())
            .columnName(column.getColumnName())
            .dataType(column.getDataType())
            .columnOrder(column.getColumnOrder())
            .isNullable(column.getIsNullable())
            .isPartitionKey(column.getIsPartitionKey())
            .description(column.getDescription())
            .createdAt(column.getCreatedAt())
            .updatedAt(column.getUpdatedAt())
            .build();
    }

    /**
     * 获取当前登录用户ID
     * 从 Spring Security 上下文中提取用户ID
     *
     * @return 当前用户ID，未登录时返回默认值 1L
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                log.debug("当前用户: {}", userDetails.getUsername());
            }
        }
        // 默认返回系统用户ID（实际项目中应从 JWT token 中解析）
        return 1L;
    }
}
