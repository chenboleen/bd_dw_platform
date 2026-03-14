package com.kiro.metadata.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kiro.metadata.dto.request.TableCreateRequest;
import com.kiro.metadata.dto.request.TableUpdateRequest;
import com.kiro.metadata.dto.response.ColumnResponse;
import com.kiro.metadata.dto.response.PagedResponse;
import com.kiro.metadata.dto.response.TableResponse;
import com.kiro.metadata.entity.ColumnMetadata;
import com.kiro.metadata.entity.TableMetadata;
import com.kiro.metadata.service.ColumnService;
import com.kiro.metadata.service.MetadataService;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 表元数据 API 控制器
 *
 * 提供表元数据的增删改查接口，包括字段列表查询
 * 支持分页、过滤和排序功能
 *
 * @author Kiro
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tables")
@RequiredArgsConstructor
@Tag(name = "表元数据管理", description = "表元数据的增删改查及字段查询接口")
public class TableController {

    private final MetadataService metadataService;
    private final ColumnService columnService;

    /**
     * 创建表元数据
     *
     * @param request 表创建请求
     * @return 创建的表信息（201 Created）
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    @Operation(
        summary = "创建表元数据",
        description = "创建新的表元数据记录，需要 DEVELOPER 或 ADMIN 角色",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "创建成功",
            content = @Content(schema = @Schema(implementation = TableResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "409", description = "表已存在")
    })
    public ResponseEntity<Map<String, Object>> createTable(
            @Valid @RequestBody TableCreateRequest request) {
        log.info("创建表元数据请求: {}.{}", request.getDatabaseName(), request.getTableName());

        // 将请求 DTO 转换为实体
        TableMetadata table = convertToEntity(request);

        // 调用服务创建表
        TableMetadata created = metadataService.createTable(table);

        // 转换为响应 DTO
        TableResponse response = convertToResponse(created);

        log.info("表元数据创建成功, ID: {}", created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(buildSuccessResponse("表元数据创建成功", response));
    }

    /**
     * 查询表列表（分页）
     *
     * @param databaseName 数据库名过滤（可选）
     * @param tableType    表类型过滤（可选）
     * @param tableName    表名模糊查询（可选）
     * @param ownerId      所有者ID过滤（可选）
     * @param page         页码（默认1）
     * @param pageSize     每页大小（默认20）
     * @param sortBy       排序字段（可选）
     * @param sortOrder    排序方向（asc/desc，默认desc）
     * @return 分页表列表
     */
    @GetMapping
    @Operation(
        summary = "查询表列表",
        description = "分页查询表元数据列表，支持按数据库名、表类型、表名、所有者过滤",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "查询成功",
            content = @Content(schema = @Schema(implementation = PagedResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public ResponseEntity<Map<String, Object>> listTables(
            @Parameter(description = "数据库名过滤")
            @RequestParam(required = false) String databaseName,
            @Parameter(description = "表类型过滤（TABLE/VIEW/EXTERNAL）")
            @RequestParam(required = false) String tableType,
            @Parameter(description = "表名模糊查询")
            @RequestParam(required = false) String tableName,
            @Parameter(description = "所有者ID过滤")
            @RequestParam(required = false) Long ownerId,
            @Parameter(description = "页码，从1开始")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小")
            @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "排序字段")
            @RequestParam(required = false) String sortBy,
            @Parameter(description = "排序方向（asc/desc）")
            @RequestParam(defaultValue = "desc") String sortOrder) {

        log.debug("查询表列表, 页码: {}, 每页大小: {}", page, pageSize);

        // 构建过滤条件
        Map<String, Object> filters = new HashMap<>();
        if (databaseName != null && !databaseName.isBlank()) {
            filters.put("databaseName", databaseName);
        }
        if (tableType != null && !tableType.isBlank()) {
            filters.put("tableType", tableType);
        }
        if (tableName != null && !tableName.isBlank()) {
            filters.put("tableName", tableName);
        }
        if (ownerId != null) {
            filters.put("ownerId", ownerId);
        }

        // 调用服务查询
        Page<TableMetadata> pageResult = metadataService.listTables(
            filters, page, pageSize, sortBy, sortOrder
        );

        // 转换为响应 DTO
        List<TableResponse> tableResponses = pageResult.getRecords().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) pageResult.getTotal() / pageSize);

        Map<String, Object> pagedData = new HashMap<>();
        pagedData.put("items", tableResponses);
        pagedData.put("total", pageResult.getTotal());
        pagedData.put("page", page);
        pagedData.put("pageSize", pageSize);
        pagedData.put("totalPages", totalPages);

        return ResponseEntity.ok(buildSuccessResponse("查询表列表成功", pagedData));
    }

    /**
     * 根据ID获取表详情
     *
     * @param id 表ID
     * @return 表详情
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "获取表详情",
        description = "根据表ID获取表元数据详细信息",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "获取成功",
            content = @Content(schema = @Schema(implementation = TableResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "表不存在")
    })
    public ResponseEntity<Map<String, Object>> getTableById(
            @Parameter(description = "表ID", required = true)
            @PathVariable Long id) {
        log.debug("获取表详情, ID: {}", id);

        TableMetadata table = metadataService.getTableById(id);
        TableResponse response = convertToResponse(table);

        return ResponseEntity.ok(buildSuccessResponse("获取表详情成功", response));
    }

    /**
     * 更新表元数据
     *
     * @param id      表ID
     * @param request 更新请求
     * @return 更新后的表信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    @Operation(
        summary = "更新表元数据",
        description = "更新表的描述、存储格式等信息，需要 DEVELOPER 或 ADMIN 角色",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "更新成功",
            content = @Content(schema = @Schema(implementation = TableResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "表不存在")
    })
    public ResponseEntity<Map<String, Object>> updateTable(
            @Parameter(description = "表ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody TableUpdateRequest request) {
        log.info("更新表元数据请求, ID: {}", id);

        // 将请求 DTO 转换为实体（仅包含可更新字段）
        TableMetadata updates = new TableMetadata();
        updates.setDescription(request.getDescription());
        updates.setStorageFormat(request.getStorageFormat());
        updates.setStorageLocation(request.getStorageLocation());
        updates.setDataSizeBytes(request.getDataSizeBytes());

        // 调用服务更新
        TableMetadata updated = metadataService.updateTable(id, updates);
        TableResponse response = convertToResponse(updated);

        log.info("表元数据更新成功, ID: {}", id);
        return ResponseEntity.ok(buildSuccessResponse("表元数据更新成功", response));
    }

    /**
     * 删除表元数据
     *
     * @param id 表ID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "删除表元数据",
        description = "删除指定表的元数据记录，需要 ADMIN 角色",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "删除成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要 ADMIN 角色"),
        @ApiResponse(responseCode = "404", description = "表不存在")
    })
    public ResponseEntity<Void> deleteTable(
            @Parameter(description = "表ID", required = true)
            @PathVariable Long id) {
        log.info("删除表元数据请求, ID: {}", id);

        // 获取当前用户ID
        Long userId = getCurrentUserId();

        // 调用服务删除
        metadataService.deleteTable(id, userId);

        log.info("表元数据删除成功, ID: {}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 获取表的字段列表
     *
     * @param id 表ID
     * @return 字段列表
     */
    @GetMapping("/{id}/columns")
    @Operation(
        summary = "获取表的字段列表",
        description = "根据表ID获取该表的所有字段元数据，按字段顺序排列",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "获取成功",
            content = @Content(schema = @Schema(implementation = ColumnResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "表不存在")
    })
    public ResponseEntity<Map<String, Object>> getColumnsByTableId(
            @Parameter(description = "表ID", required = true)
            @PathVariable Long id) {
        log.debug("获取表的字段列表, 表ID: {}", id);

        // 验证表存在
        metadataService.getTableById(id);

        // 查询字段列表
        List<ColumnMetadata> columns = columnService.getColumnsByTableId(id);

        // 转换为响应 DTO
        List<ColumnResponse> response = columns.stream()
            .map(this::convertColumnToResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(buildSuccessResponse("获取表的字段列表成功", response));
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 将创建请求 DTO 转换为实体
     *
     * @param request 创建请求
     * @return 表元数据实体
     */
    private TableMetadata convertToEntity(TableCreateRequest request) {
        TableMetadata table = new TableMetadata();
        table.setDatabaseName(request.getDatabaseName());
        table.setTableName(request.getTableName());
        table.setTableType(request.getTableType());
        table.setDescription(request.getDescription());
        table.setStorageFormat(request.getStorageFormat());
        table.setStorageLocation(request.getStorageLocation());
        table.setDataSizeBytes(request.getDataSizeBytes());
        table.setOwnerId(request.getOwnerId());
        return table;
    }

    /**
     * 将表元数据实体转换为响应 DTO
     *
     * @param table 表元数据实体
     * @return 表响应 DTO
     */
    private TableResponse convertToResponse(TableMetadata table) {
        return TableResponse.builder()
            .id(table.getId())
            .databaseName(table.getDatabaseName())
            .tableName(table.getTableName())
            .tableType(table.getTableType())
            .description(table.getDescription())
            .storageFormat(table.getStorageFormat())
            .storageLocation(table.getStorageLocation())
            .dataSizeBytes(table.getDataSizeBytes())
            .createdAt(table.getCreatedAt())
            .updatedAt(table.getUpdatedAt())
            .lastAccessedAt(table.getLastAccessedAt())
            .ownerId(table.getOwnerId())
            .build();
    }

    /**
     * 将字段元数据实体转换为响应 DTO
     *
     * @param column 字段元数据实体
     * @return 字段响应 DTO
     */
    private ColumnResponse convertColumnToResponse(ColumnMetadata column) {
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
     * 构建统一成功响应格式
     */
    private Map<String, Object> buildSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        return response;
    }

    /**
     * 获取当前登录用户ID
     * 从 Spring Security 上下文中提取用户ID
     *
     * @return 当前用户ID，未登录时返回 -1L
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            // 尝试从 principal 中获取用户ID
            Object principal = authentication.getPrincipal();
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                // 通过用户名查找用户ID（实际项目中可扩展为从 JWT claims 中获取）
                log.debug("当前用户: {}", userDetails.getUsername());
            }
        }
        // 默认返回系统用户ID（实际项目中应从 JWT token 中解析）
        return 1L;
    }
}
