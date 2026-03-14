package com.kiro.metadata.controller;

import com.kiro.metadata.dto.request.CatalogCreateRequest;
import com.kiro.metadata.dto.response.CatalogResponse;
import com.kiro.metadata.entity.Catalog;
import com.kiro.metadata.entity.TableMetadata;
import com.kiro.metadata.service.CatalogService;
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

/**
 * 数据目录 API 控制器
 *
 * 提供数据目录的创建、树形查询、移动、以及表与目录关联管理接口
 * 支持最多 5 层的树形目录结构
 *
 * @author Kiro
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/catalogs")
@RequiredArgsConstructor
@Tag(name = "数据目录管理", description = "数据目录的创建、树形查询、移动及表关联管理接口")
public class CatalogController {

    private final CatalogService catalogService;

    /**
     * 创建目录节点
     * 仅 ADMIN 角色可操作
     *
     * @param request 目录创建请求
     * @return 创建的目录信息（201 Created）
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "创建目录节点",
        description = "创建数据目录节点，支持最多 5 层树形结构，需要 ADMIN 角色",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "创建成功",
            content = @Content(schema = @Schema(implementation = CatalogResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "请求参数错误或层级超限"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要 ADMIN 角色"),
        @ApiResponse(responseCode = "404", description = "父目录不存在")
    })
    public ResponseEntity<Map<String, Object>> createCatalog(
            @Valid @RequestBody CatalogCreateRequest request) {
        log.info("创建目录节点请求: {}, 父目录ID: {}", request.getName(), request.getParentId());

        // 将请求 DTO 转换为实体
        Catalog catalog = convertToEntity(request);

        // 调用服务创建目录
        Catalog created = catalogService.createCatalog(catalog);

        // 转换为响应 DTO
        CatalogResponse response = convertToResponse(created);

        Map<String, Object> result = buildSuccessResponse("目录创建成功", response);

        log.info("目录节点创建成功, ID: {}", created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * 获取目录树
     * 返回完整的树形目录结构
     *
     * @return 根节点列表（包含子节点）
     */
    @GetMapping("/tree")
    @Operation(
        summary = "获取目录树",
        description = "获取完整的树形目录结构，返回根节点列表，每个节点包含其子节点",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public ResponseEntity<Map<String, Object>> getCatalogTree() {
        log.info("获取目录树请求");

        List<Map<String, Object>> tree = catalogService.getCatalogTree();

        Map<String, Object> result = buildSuccessResponse("获取目录树成功", tree);

        log.info("目录树获取成功, 根节点数: {}", tree.size());
        return ResponseEntity.ok(result);
    }

    /**
     * 获取扁平化目录列表（用于下拉选择）
     *
     * @return 所有目录节点列表（不含树形结构）
     */
    @GetMapping("/flat")
    @Operation(
        summary = "获取扁平化目录列表",
        description = "获取所有目录节点的扁平列表，用于下拉选择",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    public ResponseEntity<Map<String, Object>> getCatalogFlat() {
        log.debug("获取扁平化目录列表");
        List<Catalog> catalogs = catalogService.getAllCatalogs();
        List<Map<String, Object>> result = catalogs.stream().map(c -> {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("id", c.getId());
            item.put("name", c.getName());
            item.put("path", c.getPath());
            item.put("level", c.getLevel());
            return item;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(buildSuccessResponse("获取目录列表成功", result));
    }

    /**
     * 更新目录名称和描述
     * 无论是否绑定表都可以修改，同时递归更新子目录 path
     * 仅 ADMIN 角色可操作
     *
     * @param id      目录ID
     * @param request 更新请求（name、description）
     * @return 更新后的目录信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "更新目录",
        description = "更新目录名称和描述，自动递归更新所有子目录的路径，需要 ADMIN 角色",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要 ADMIN 角色"),
        @ApiResponse(responseCode = "404", description = "目录不存在")
    })
    public ResponseEntity<Map<String, Object>> updateCatalog(
            @Parameter(description = "目录ID", required = true)
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        log.info("更新目录请求, ID: {}", id);

        String name = request.get("name");
        String description = request.get("description");

        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(buildSuccessResponse("目录名称不能为空", null));
        }

        Long userId = getCurrentUserId();
        Catalog updated = catalogService.updateCatalog(id, name, description, userId);

        CatalogResponse response = convertToResponse(updated);
        Map<String, Object> result = buildSuccessResponse("目录更新成功", response);

        log.info("目录更新成功, ID: {}", id);
        return ResponseEntity.ok(result);
    }

    /**
     * 删除目录节点（仅叶子节点可删除）
     * 仅 ADMIN 角色可操作
     *
     * @param id 目录ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "删除目录",
        description = "删除目录节点，仅叶子节点（无子目录且无关联表）可删除，需要 ADMIN 角色",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "400", description = "目录下存在子目录或关联表"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要 ADMIN 角色"),
        @ApiResponse(responseCode = "404", description = "目录不存在")
    })
    public ResponseEntity<Map<String, Object>> deleteCatalog(
            @Parameter(description = "目录ID", required = true)
            @PathVariable Long id) {
        log.info("删除目录请求, ID: {}", id);

        Long userId = getCurrentUserId();
        catalogService.deleteCatalog(id, userId);

        Map<String, Object> result = buildSuccessResponse("目录删除成功", null);
        log.info("目录删除成功, ID: {}", id);
        return ResponseEntity.ok(result);
    }

    /**
     * 移动目录节点
     * 仅 ADMIN 角色可操作
     *
     * @param id          目录ID
     * @param newParentId 新父目录ID（为 null 时移动到根目录）
     * @return 更新后的目录信息
     */
    @PutMapping("/{id}/move")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "移动目录节点",
        description = "将目录节点移动到新的父目录下，newParentId 为 null 时移动到根目录，需要 ADMIN 角色",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "移动成功"),
        @ApiResponse(responseCode = "400", description = "移动后层级超限"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要 ADMIN 角色"),
        @ApiResponse(responseCode = "404", description = "目录或新父目录不存在")
    })
    public ResponseEntity<Map<String, Object>> moveCatalog(
            @Parameter(description = "目录ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "新父目录ID，为 null 时移动到根目录")
            @RequestParam(required = false) Long newParentId) {
        log.info("移动目录节点请求, ID: {}, 新父目录ID: {}", id, newParentId);

        Long userId = getCurrentUserId();
        Catalog moved = catalogService.moveCatalog(id, newParentId, userId);

        CatalogResponse response = convertToResponse(moved);
        Map<String, Object> result = buildSuccessResponse("目录移动成功", response);

        log.info("目录节点移动成功, ID: {}", id);
        return ResponseEntity.ok(result);
    }

    /**
     * 添加表到目录
     * DEVELOPER 或 ADMIN 角色可操作
     *
     * @param catalogId 目录ID
     * @param tableId   表ID
     * @return 操作结果
     */
    @PostMapping("/{catalogId}/tables/{tableId}")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    @Operation(
        summary = "添加表到目录",
        description = "将指定表关联到目录，需要 DEVELOPER 或 ADMIN 角色",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "添加成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "目录或表不存在")
    })
    public ResponseEntity<Map<String, Object>> addTableToCatalog(
            @Parameter(description = "目录ID", required = true)
            @PathVariable Long catalogId,
            @Parameter(description = "表ID", required = true)
            @PathVariable Long tableId) {
        log.info("添加表到目录请求, 目录ID: {}, 表ID: {}", catalogId, tableId);

        Long userId = getCurrentUserId();
        boolean success = catalogService.addTableToCatalog(tableId, catalogId, userId);

        Map<String, Object> result = buildSuccessResponse("表添加到目录成功", success);

        return ResponseEntity.ok(result);
    }

    /**
     * 从目录移除表
     * DEVELOPER 或 ADMIN 角色可操作
     *
     * @param catalogId 目录ID
     * @param tableId   表ID
     * @return 操作结果
     */
    @DeleteMapping("/{catalogId}/tables/{tableId}")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    @Operation(
        summary = "从目录移除表",
        description = "将指定表从目录中移除，需要 DEVELOPER 或 ADMIN 角色",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "移除成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "目录或表不存在")
    })
    public ResponseEntity<Map<String, Object>> removeTableFromCatalog(
            @Parameter(description = "目录ID", required = true)
            @PathVariable Long catalogId,
            @Parameter(description = "表ID", required = true)
            @PathVariable Long tableId) {
        log.info("从目录移除表请求, 目录ID: {}, 表ID: {}", catalogId, tableId);

        Long userId = getCurrentUserId();
        boolean success = catalogService.removeTableFromCatalog(tableId, catalogId, userId);

        Map<String, Object> result = buildSuccessResponse("表从目录移除成功", success);

        return ResponseEntity.ok(result);
    }

    /**
     * 获取目录下的表列表
     *
     * @param catalogId 目录ID
     * @return 表列表
     */
    @GetMapping("/{catalogId}/tables")
    @Operation(
        summary = "获取目录下的表列表",
        description = "查询指定目录下关联的所有表",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "目录不存在")
    })
    public ResponseEntity<Map<String, Object>> getTablesInCatalog(
            @Parameter(description = "目录ID", required = true)
            @PathVariable Long catalogId) {
        log.info("获取目录下的表列表请求, 目录ID: {}", catalogId);

        List<TableMetadata> tables = catalogService.getTablesInCatalog(catalogId);

        Map<String, Object> result = buildSuccessResponse("获取目录表列表成功", tables);
        result.put("total", tables.size());

        log.info("目录下的表列表获取成功, 目录ID: {}, 表数量: {}", catalogId, tables.size());
        return ResponseEntity.ok(result);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 将创建请求 DTO 转换为实体
     * level、path 由 CatalogService 根据 parentId 自动计算
     * createdBy 从当前登录用户获取
     */
    private Catalog convertToEntity(CatalogCreateRequest request) {
        Catalog catalog = new Catalog();
        catalog.setName(request.getName());
        catalog.setDescription(request.getDescription());
        catalog.setParentId(request.getParentId());
        // level 和 path 由 CatalogService.createCatalog 自动计算
        // 先设置默认值，service 层会覆盖
        catalog.setLevel(request.getParentId() == null ? 1 : 0);
        catalog.setPath("");
        catalog.setCreatedBy(getCurrentUserId());
        return catalog;
    }

    /**
     * 将目录实体转换为响应 DTO
     */
    private CatalogResponse convertToResponse(Catalog catalog) {
        return CatalogResponse.builder()
            .id(catalog.getId())
            .name(catalog.getName())
            .description(catalog.getDescription())
            .parentId(catalog.getParentId())
            .level(catalog.getLevel())
            .path(catalog.getPath())
            .createdAt(catalog.getCreatedAt())
            .updatedAt(catalog.getUpdatedAt())
            .createdBy(catalog.getCreatedBy())
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
