package com.kiro.metadata.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kiro.metadata.dto.response.ChangeHistoryResponse;
import com.kiro.metadata.entity.ChangeHistory;
import com.kiro.metadata.service.HistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import java.util.stream.Collectors;

/**
 * 变更历史 API 控制器
 *
 * 提供实体变更历史、用户操作历史和最近变更记录的查询接口
 *
 * @author Kiro
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
@Tag(name = "变更历史管理", description = "实体变更历史、用户操作历史和最近变更记录查询接口")
public class HistoryController {

    private final HistoryService historyService;

    /**
     * 获取实体变更历史（分页）
     * 查询指定实体类型和 ID 的所有变更记录，按时间倒序排列
     *
     * @param entityType 实体类型（如 TABLE、COLUMN、CATALOG）
     * @param entityId   实体ID
     * @param page       页码（默认 1）
     * @param pageSize   每页大小（默认 20）
     * @return 分页变更历史记录
     */
    @GetMapping("/{entityType}/{entityId}")
    @Operation(
        summary = "获取实体变更历史",
        description = "查询指定实体类型和 ID 的所有变更记录，按时间倒序排列，支持分页",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public ResponseEntity<Map<String, Object>> getEntityHistory(
            @Parameter(description = "实体类型（TABLE/COLUMN/CATALOG）", required = true, example = "TABLE")
            @PathVariable String entityType,
            @Parameter(description = "实体ID", required = true, example = "1")
            @PathVariable Long entityId,
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20") int pageSize) {
        log.info("获取实体变更历史请求, 类型: {}, ID: {}, 页码: {}", entityType, entityId, page);

        Page<ChangeHistory> historyPage = historyService.getEntityHistory(
            entityType.toUpperCase(), entityId, page, pageSize);

        List<ChangeHistoryResponse> records = historyPage.getRecords().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());

        Map<String, Object> result = buildPagedResponse(
            "获取实体变更历史成功",
            records,
            historyPage.getTotal(),
            page,
            pageSize
        );

        log.info("实体变更历史获取成功, 类型: {}, ID: {}, 总数: {}", entityType, entityId, historyPage.getTotal());
        return ResponseEntity.ok(result);
    }

    /**
     * 获取用户操作历史（分页）
     * 查询指定用户的所有操作记录，按时间倒序排列
     *
     * @param userId   用户ID
     * @param page     页码（默认 1）
     * @param pageSize 每页大小（默认 20）
     * @return 分页用户操作历史
     */
    @GetMapping("/user/{userId}")
    @Operation(
        summary = "获取用户操作历史",
        description = "查询指定用户的所有操作记录，按时间倒序排列，支持分页",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    public ResponseEntity<Map<String, Object>> getUserActivity(
            @Parameter(description = "用户ID", required = true, example = "1")
            @PathVariable Long userId,
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20") int pageSize) {
        log.info("获取用户操作历史请求, 用户ID: {}, 页码: {}", userId, page);

        Page<ChangeHistory> historyPage = historyService.getUserActivity(userId, page, pageSize);

        List<ChangeHistoryResponse> records = historyPage.getRecords().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());

        Map<String, Object> result = buildPagedResponse(
            "获取用户操作历史成功",
            records,
            historyPage.getTotal(),
            page,
            pageSize
        );

        log.info("用户操作历史获取成功, 用户ID: {}, 总数: {}", userId, historyPage.getTotal());
        return ResponseEntity.ok(result);
    }

    /**
     * 获取最近变更记录
     * 查询最近指定小时内的变更记录
     *
     * @param hours 小时数（默认 24 小时）
     * @param limit 返回记录数上限（默认 50 条）
     * @return 最近变更记录列表
     */
    @GetMapping("/recent")
    @Operation(
        summary = "获取最近变更记录",
        description = "查询最近指定小时内的变更记录，按时间倒序排列",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public ResponseEntity<Map<String, Object>> getRecentChanges(
            @Parameter(description = "查询小时数，默认 24 小时", example = "24")
            @RequestParam(defaultValue = "24") int hours,
            @Parameter(description = "返回记录数上限，默认 50 条", example = "50")
            @RequestParam(defaultValue = "50") int limit) {
        log.info("获取最近变更记录请求, 小时数: {}, 限制: {}", hours, limit);

        List<ChangeHistory> recentChanges = historyService.getRecentChanges(hours, limit);

        List<ChangeHistoryResponse> records = recentChanges.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "获取最近变更记录成功");
        result.put("data", records);
        result.put("total", records.size());
        result.put("hours", hours);

        log.info("最近变更记录获取成功, 记录数: {}", records.size());
        return ResponseEntity.ok(result);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 将变更历史实体转换为响应 DTO
     */
    private ChangeHistoryResponse convertToResponse(ChangeHistory history) {
        return ChangeHistoryResponse.builder()
            .id(history.getId())
            .entityType(history.getEntityType())
            .entityId(history.getEntityId())
            .operation(history.getOperation() != null ? history.getOperation().name() : null)
            .fieldName(history.getFieldName())
            .oldValue(history.getOldValue())
            .newValue(history.getNewValue())
            .changedAt(history.getChangedAt())
            .changedBy(history.getChangedBy())
            .build();
    }

    /**
     * 构建分页响应格式
     */
    private Map<String, Object> buildPagedResponse(String message, List<?> records,
                                                    long total, int page, int pageSize) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", records);
        response.put("total", total);
        response.put("page", page);
        response.put("pageSize", pageSize);
        response.put("totalPages", (int) Math.ceil((double) total / pageSize));
        return response;
    }
}
