package com.kiro.metadata.controller;

import com.kiro.metadata.dto.request.FilterRequest;
import com.kiro.metadata.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 搜索 API 控制器
 *
 * 提供全文搜索、搜索建议（自动补全）和高级过滤接口
 * 基于 Elasticsearch 实现，支持多字段匹配、高亮显示和分页
 *
 * @author Kiro
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "搜索管理", description = "全文搜索、搜索建议及高级过滤接口")
public class SearchController {

    private final SearchService searchService;

    /**
     * 全文搜索
     * 支持按关键词搜索表名、数据库名、描述、字段名等，并可附加过滤条件
     *
     * @param keyword      搜索关键词（必填）
     * @param databaseName 数据库名过滤（可选）
     * @param tableType    表类型过滤（可选，TABLE/VIEW/EXTERNAL）
     * @param ownerId      所有者ID过滤（可选）
     * @param page         页码（默认1）
     * @param pageSize     每页大小（默认20）
     * @return 搜索结果，包含匹配文档列表、总数、分页信息
     */
    @GetMapping
    @Operation(
        summary = "全文搜索",
        description = "基于 Elasticsearch 的全文搜索，支持多字段匹配（表名、数据库名、描述、字段名），"
            + "支持模糊匹配和高亮显示，可附加数据库名、表类型、所有者等过滤条件",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "搜索成功",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(responseCode = "400", description = "请求参数错误（关键词为空）"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "500", description = "搜索引擎异常")
    })
    public ResponseEntity<Map<String, Object>> search(
            @Parameter(description = "搜索关键词", required = true, example = "用户行为")
            @RequestParam String keyword,
            @Parameter(description = "数据库名过滤", example = "dw_prod")
            @RequestParam(required = false) String databaseName,
            @Parameter(description = "表类型过滤（TABLE/VIEW/EXTERNAL）", example = "TABLE")
            @RequestParam(required = false) String tableType,
            @Parameter(description = "所有者ID过滤", example = "1")
            @RequestParam(required = false) Long ownerId,
            @Parameter(description = "页码，从1开始", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20") int pageSize) {

        log.info("全文搜索请求, 关键词: {}, 页码: {}, 每页大小: {}", keyword, page, pageSize);

        // 构建过滤条件
        Map<String, Object> filters = buildFilters(databaseName, tableType, ownerId);

        // 调用搜索服务（页码从0开始，对外从1开始）
        Map<String, Object> result = searchService.searchTables(keyword, filters, page - 1, pageSize);

        // 统一返回格式
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "搜索成功");
        response.put("data", result);

        log.info("全文搜索完成, 关键词: {}, 总数: {}", keyword, result.get("total"));
        return ResponseEntity.ok(response);
    }

    /**
     * 搜索建议（自动补全）
     * 根据输入前缀返回匹配的表名建议列表，用于搜索框自动补全
     *
     * @param prefix 搜索前缀（必填）
     * @param limit  返回建议数量上限（默认10）
     * @return 建议列表，格式为 "数据库名.表名"
     */
    @GetMapping("/suggest")
    @Operation(
        summary = "搜索建议（自动补全）",
        description = "根据输入前缀返回匹配的表名建议列表，用于搜索框自动补全功能。"
            + "返回格式为 \"数据库名.表名\"",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "获取建议成功",
            content = @Content(schema = @Schema(implementation = List.class))
        ),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "500", description = "搜索引擎异常")
    })
    public ResponseEntity<List<String>> suggest(
            @Parameter(description = "搜索前缀", required = true, example = "user_")
            @RequestParam String prefix,
            @Parameter(description = "返回建议数量上限", example = "10")
            @RequestParam(defaultValue = "10") int limit) {

        log.debug("搜索建议请求, 前缀: {}, 限制: {}", prefix, limit);

        List<String> suggestions = searchService.suggest(prefix, limit);

        log.debug("搜索建议完成, 前缀: {}, 返回{}条建议", prefix, suggestions.size());
        return ResponseEntity.ok(suggestions);
    }

    /**
     * 高级过滤
     * 通过请求体传入多维度过滤条件，支持数据库名、表类型、所有者、更新时间范围等
     *
     * @param filterRequest 过滤请求（包含过滤条件和分页参数）
     * @return 过滤结果，包含匹配文档列表、总数、分页信息
     */
    @PostMapping("/filter")
    @Operation(
        summary = "高级过滤",
        description = "通过请求体传入多维度过滤条件进行精确筛选，支持数据库名、表类型、所有者ID、"
            + "更新时间范围等条件的组合过滤，适合复杂查询场景",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "过滤成功",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "500", description = "搜索引擎异常")
    })
    public ResponseEntity<Map<String, Object>> filter(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "过滤条件及分页参数",
                required = true,
                content = @Content(schema = @Schema(implementation = FilterRequest.class))
            )
            @RequestBody FilterRequest filterRequest) {

        log.info("高级过滤请求, 条件: databaseName={}, tableType={}, ownerId={}",
            filterRequest.getDatabaseName(), filterRequest.getTableType(), filterRequest.getOwnerId());

        // 将 FilterRequest 转换为 Map
        Map<String, Object> filters = new HashMap<>();
        if (filterRequest.getDatabaseName() != null && !filterRequest.getDatabaseName().isBlank()) {
            filters.put("databaseName", filterRequest.getDatabaseName());
        }
        if (filterRequest.getTableType() != null && !filterRequest.getTableType().isBlank()) {
            filters.put("tableType", filterRequest.getTableType());
        }
        if (filterRequest.getOwnerId() != null) {
            filters.put("ownerId", filterRequest.getOwnerId());
        }
        if (filterRequest.getUpdatedFrom() != null && !filterRequest.getUpdatedFrom().isBlank()) {
            filters.put("updatedFrom", filterRequest.getUpdatedFrom());
        }
        if (filterRequest.getUpdatedTo() != null && !filterRequest.getUpdatedTo().isBlank()) {
            filters.put("updatedTo", filterRequest.getUpdatedTo());
        }

        // 获取分页参数（页码从0开始，对外从1开始）
        int page = filterRequest.getPage() != null ? filterRequest.getPage() - 1 : 0;
        int pageSize = filterRequest.getPageSize() != null ? filterRequest.getPageSize() : 20;

        // 调用过滤服务
        Map<String, Object> result = searchService.filterTables(filters, page, pageSize);

        // 统一返回格式
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "过滤成功");
        response.put("data", result);

        log.info("高级过滤完成, 总数: {}", result.get("total"));
        return ResponseEntity.ok(response);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建过滤条件 Map
     * 将请求参数中非空的过滤条件组装为 Map
     *
     * @param databaseName 数据库名
     * @param tableType    表类型
     * @param ownerId      所有者ID
     * @return 过滤条件 Map
     */
    private Map<String, Object> buildFilters(String databaseName, String tableType, Long ownerId) {
        Map<String, Object> filters = new HashMap<>();
        if (databaseName != null && !databaseName.isBlank()) {
            filters.put("databaseName", databaseName);
        }
        if (tableType != null && !tableType.isBlank()) {
            filters.put("tableType", tableType);
        }
        if (ownerId != null) {
            filters.put("ownerId", ownerId);
        }
        return filters;
    }
}
