package com.kiro.metadata.controller;

import com.kiro.metadata.dto.request.LineageCreateRequest;
import com.kiro.metadata.dto.request.SqlParseRequest;
import com.kiro.metadata.dto.response.ImpactReport;
import com.kiro.metadata.dto.response.LineageResponse;
import com.kiro.metadata.entity.Lineage;
import com.kiro.metadata.service.LineageService;
import com.kiro.metadata.service.SqlParserService;
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
 * 血缘关系 API 控制器
 *
 * 提供血缘关系的创建、删除、查询、图谱生成、影响分析及SQL解析接口
 * 支持基于角色的权限控制（DEVELOPER/ADMIN 可创建，ADMIN 可删除）
 *
 * @author Kiro
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/lineage")
@RequiredArgsConstructor
@Tag(name = "血缘关系管理", description = "血缘关系的创建、删除、查询、图谱生成、影响分析及SQL解析接口")
public class LineageController {

    private final LineageService lineageService;
    private final SqlParserService sqlParserService;

    /**
     * 创建血缘关系
     *
     * @param request 血缘关系创建请求
     * @return 创建的血缘关系信息（201 Created）
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    @Operation(
        summary = "创建血缘关系",
        description = "创建表之间的血缘关系，需要 DEVELOPER 或 ADMIN 角色。系统会自动检测循环依赖。",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "创建成功",
            content = @Content(schema = @Schema(implementation = LineageResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "请求参数错误或存在循环依赖"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "源表或目标表不存在")
    })
    public ResponseEntity<Map<String, Object>> createLineage(
            @Valid @RequestBody LineageCreateRequest request) {
        log.info("创建血缘关系请求: {} -> {}", request.getSourceTableId(), request.getTargetTableId());

        // 将请求 DTO 转换为实体
        Lineage lineage = convertToEntity(request);

        // 调用服务创建血缘关系
        Lineage created = lineageService.createLineage(lineage);

        // 转换为响应 DTO
        LineageResponse response = convertToResponse(created);

        log.info("血缘关系创建成功, ID: {}", created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(buildSuccessResponse("血缘关系创建成功", response));
    }

    /**
     * 删除血缘关系
     *
     * @param id 血缘关系ID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "删除血缘关系",
        description = "删除指定的血缘关系记录，需要 ADMIN 角色",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "删除成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要 ADMIN 角色"),
        @ApiResponse(responseCode = "404", description = "血缘关系不存在")
    })
    public ResponseEntity<Void> deleteLineage(
            @Parameter(description = "血缘关系ID", required = true)
            @PathVariable Long id) {
        log.info("删除血缘关系请求, ID: {}", id);

        Long userId = getCurrentUserId();
        lineageService.deleteLineage(id, userId);

        log.info("血缘关系删除成功, ID: {}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 获取指定表的上游表列表
     *
     * @param tableId 表ID
     * @return 上游表ID列表
     */
    @GetMapping("/upstream/{tableId}")
    @Operation(
        summary = "获取上游表列表",
        description = "获取指定表的所有直接上游表ID列表",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "表不存在")
    })
    public ResponseEntity<Map<String, Object>> getUpstreamTables(
            @Parameter(description = "表ID", required = true)
            @PathVariable Long tableId) {
        log.debug("查询上游表, 表ID: {}", tableId);

        List<Long> upstreamTableIds = lineageService.getUpstreamTables(tableId);

        return ResponseEntity.ok(buildSuccessResponse("查询上游表成功", upstreamTableIds));
    }

    /**
     * 获取指定表的下游表列表
     *
     * @param tableId 表ID
     * @return 下游表ID列表
     */
    @GetMapping("/downstream/{tableId}")
    @Operation(
        summary = "获取下游表列表",
        description = "获取指定表的所有直接下游表ID列表",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "表不存在")
    })
    public ResponseEntity<Map<String, Object>> getDownstreamTables(
            @Parameter(description = "表ID", required = true)
            @PathVariable Long tableId) {
        log.debug("查询下游表, 表ID: {}", tableId);

        List<Long> downstreamTableIds = lineageService.getDownstreamTables(tableId);

        return ResponseEntity.ok(buildSuccessResponse("查询下游表成功", downstreamTableIds));
    }

    /**
     * 获取血缘关系图谱
     *
     * @param tableId   表ID
     * @param direction 方向（upstream/downstream/both，默认 both）
     * @param depth     深度（1-5，默认 3）
     * @return 血缘图谱（包含节点和边）
     */
    @GetMapping("/graph/{tableId}")
    @Operation(
        summary = "获取血缘关系图谱",
        description = "构建并返回指定表的血缘关系图谱，支持上游、下游或双向查询，可指定遍历深度（1-5）",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "参数错误（深度超出范围）"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "表不存在")
    })
    public ResponseEntity<Map<String, Object>> getLineageGraph(
            @Parameter(description = "表ID", required = true)
            @PathVariable Long tableId,
            @Parameter(description = "查询方向：upstream（上游）、downstream（下游）、both（双向）")
            @RequestParam(defaultValue = "both") String direction,
            @Parameter(description = "遍历深度，范围 1-5")
            @RequestParam(defaultValue = "3") int depth) {
        log.info("获取血缘图谱, 表ID: {}, 方向: {}, 深度: {}", tableId, direction, depth);

        Map<String, Object> graph = lineageService.buildLineageGraph(tableId, direction, depth);

        return ResponseEntity.ok(buildSuccessResponse("获取血缘图谱成功", graph));
    }

    /**
     * 影响分析
     * 分析指定表变更后对下游表的影响范围
     *
     * @param tableId 表ID
     * @return 影响分析报告
     */
    @PostMapping("/impact/{tableId}")
    @Operation(
        summary = "影响分析",
        description = "分析指定表发生变更后，对所有下游依赖表的影响范围，返回影响报告",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "分析成功",
            content = @Content(schema = @Schema(implementation = ImpactReport.class))
        ),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "表不存在")
    })
    public ResponseEntity<Map<String, Object>> analyzeImpact(
            @Parameter(description = "表ID", required = true)
            @PathVariable Long tableId) {
        log.info("影响分析请求, 表ID: {}", tableId);

        ImpactReport report = lineageService.analyzeImpact(tableId);

        return ResponseEntity.ok(buildSuccessResponse("影响分析成功", report));
    }

    /**
     * 解析SQL提取血缘关系
     * 从SQL语句中自动识别源表和目标表，提取血缘关系信息
     *
     * @param request SQL解析请求（包含 sql 字段）
     * @return 提取的血缘关系列表，每条记录包含 sourceTable、targetTable、type 字段
     */
    @PostMapping("/parse-sql")
    @Operation(
        summary = "解析SQL提取血缘关系",
        description = "解析SQL语句，自动提取源表和目标表之间的血缘关系。支持 SELECT、INSERT INTO...SELECT、CREATE TABLE AS SELECT 等语句类型。",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "解析成功"),
        @ApiResponse(responseCode = "400", description = "SQL语句为空或解析失败"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public ResponseEntity<Map<String, Object>> parseSql(
            @RequestBody SqlParseRequest request) {
        log.info("SQL解析请求, SQL长度: {}", request.getSql() != null ? request.getSql().length() : 0);

        List<Map<String, String>> lineages = sqlParserService.extractLineageFromSql(request.getSql());

        log.info("SQL解析完成, 提取到{}条血缘关系", lineages.size());
        return ResponseEntity.ok(buildSuccessResponse("SQL解析完成", lineages));
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 将创建请求 DTO 转换为实体
     *
     * @param request 创建请求
     * @return 血缘关系实体
     */
    private Lineage convertToEntity(LineageCreateRequest request) {
        Lineage lineage = new Lineage();
        lineage.setSourceTableId(request.getSourceTableId());
        lineage.setTargetTableId(request.getTargetTableId());
        lineage.setLineageType(request.getLineageType());
        lineage.setTransformationLogic(request.getTransformationLogic());
        lineage.setCreatedBy(request.getCreatedBy());
        return lineage;
    }

    /**
     * 将血缘关系实体转换为响应 DTO
     *
     * @param lineage 血缘关系实体
     * @return 血缘关系响应 DTO
     */
    private LineageResponse convertToResponse(Lineage lineage) {
        return LineageResponse.builder()
            .id(lineage.getId())
            .sourceTableId(lineage.getSourceTableId())
            .targetTableId(lineage.getTargetTableId())
            .lineageType(lineage.getLineageType())
            .transformationLogic(lineage.getTransformationLogic())
            .createdAt(lineage.getCreatedAt())
            .updatedAt(lineage.getUpdatedAt())
            .createdBy(lineage.getCreatedBy())
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
