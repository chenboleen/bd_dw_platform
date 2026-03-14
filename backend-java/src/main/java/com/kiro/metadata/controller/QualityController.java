package com.kiro.metadata.controller;

import com.kiro.metadata.dto.response.QualityMetricsResponse;
import com.kiro.metadata.entity.QualityMetrics;
import com.kiro.metadata.service.QualityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据质量 API 控制器
 *
 * 提供数据质量指标的记录、查询、趋势分析和质量评分接口
 *
 * @author Kiro
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/quality")
@RequiredArgsConstructor
@Tag(name = "数据质量管理", description = "数据质量指标的记录、查询、趋势分析和质量评分接口")
public class QualityController {

    private final QualityService qualityService;

    /**
     * 记录质量指标
     * DEVELOPER 或 ADMIN 角色可操作
     *
     * @param request 质量指标请求
     * @return 记录的质量指标（201 Created）
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    @Operation(
        summary = "记录质量指标",
        description = "为指定表记录数据质量指标，包括记录数、空值率、更新频率、数据新鲜度等，需要 DEVELOPER 或 ADMIN 角色",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "记录成功",
            content = @Content(schema = @Schema(implementation = QualityMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "表不存在")
    })
    public ResponseEntity<Map<String, Object>> recordQualityMetrics(
            @Valid @RequestBody QualityMetricsRequest request) {
        log.info("记录质量指标请求, 表ID: {}", request.getTableId());

        // 将请求 DTO 转换为实体
        QualityMetrics metrics = convertToEntity(request);

        // 调用服务记录指标
        QualityMetrics recorded = qualityService.recordQualityMetrics(metrics);

        // 转换为响应 DTO
        QualityMetricsResponse response = convertToResponse(recorded);

        Map<String, Object> result = buildSuccessResponse("质量指标记录成功", response);

        log.info("质量指标记录成功, ID: {}", recorded.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * 获取表的最新质量指标
     *
     * @param tableId 表ID
     * @return 最新质量指标
     */
    @GetMapping("/{tableId}")
    @Operation(
        summary = "获取最新质量指标",
        description = "获取指定表的最新一条数据质量指标记录",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "查询成功",
            content = @Content(schema = @Schema(implementation = QualityMetricsResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "表不存在或无质量指标记录")
    })
    public ResponseEntity<Map<String, Object>> getQualityMetrics(
            @Parameter(description = "表ID", required = true)
            @PathVariable Long tableId) {
        log.info("获取最新质量指标请求, 表ID: {}", tableId);

        QualityMetrics metrics = qualityService.getQualityMetrics(tableId);

        if (metrics == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "未找到质量指标记录");
            result.put("data", null);
            return ResponseEntity.ok(result);
        }

        QualityMetricsResponse response = convertToResponse(metrics);
        Map<String, Object> result = buildSuccessResponse("获取质量指标成功", response);

        return ResponseEntity.ok(result);
    }

    /**
     * 获取质量趋势
     * 查询指定天数内的质量快照，用于趋势分析
     *
     * @param tableId 表ID
     * @param days    查询天数（默认 30 天）
     * @return 质量趋势数据列表
     */
    @GetMapping("/{tableId}/trend")
    @Operation(
        summary = "获取质量趋势",
        description = "查询指定天数内的质量指标快照，按时间升序排列，用于趋势图表展示",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "表不存在")
    })
    public ResponseEntity<Map<String, Object>> getQualityTrend(
            @Parameter(description = "表ID", required = true)
            @PathVariable Long tableId,
            @Parameter(description = "查询天数，默认 30 天", example = "30")
            @RequestParam(defaultValue = "30") int days) {
        log.info("获取质量趋势请求, 表ID: {}, 天数: {}", tableId, days);

        List<QualityMetrics> trend = qualityService.getQualityTrend(tableId, days);

        List<QualityMetricsResponse> responseList = trend.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());

        Map<String, Object> result = buildSuccessResponse("获取质量趋势成功", responseList);
        result.put("total", responseList.size());
        result.put("days", days);

        log.info("质量趋势获取成功, 表ID: {}, 记录数: {}", tableId, responseList.size());
        return ResponseEntity.ok(result);
    }

    /**
     * 计算质量分数
     * 根据最新质量指标计算综合质量评分（0-100）
     *
     * @param tableId 表ID
     * @return 质量分数
     */
    @GetMapping("/{tableId}/score")
    @Operation(
        summary = "计算质量分数",
        description = "根据表的最新质量指标计算综合质量评分（0-100），综合考虑空值率、数据新鲜度、记录数和更新频率",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "计算成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "表不存在或无质量指标记录")
    })
    public ResponseEntity<Map<String, Object>> calculateQualityScore(
            @Parameter(description = "表ID", required = true)
            @PathVariable Long tableId) {
        log.info("计算质量分数请求, 表ID: {}", tableId);

        // 先获取最新指标
        QualityMetrics metrics = qualityService.getQualityMetrics(tableId);

        if (metrics == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "未找到质量指标记录，无法计算质量分数");
            result.put("data", null);
            return ResponseEntity.ok(result);
        }

        // 计算质量分数
        double score = qualityService.calculateQualityScore(metrics);

        Map<String, Object> scoreData = new HashMap<>();
        scoreData.put("tableId", tableId);
        scoreData.put("score", score);
        scoreData.put("grade", getScoreGrade(score));
        scoreData.put("measuredAt", metrics.getMeasuredAt());

        Map<String, Object> result = buildSuccessResponse("质量分数计算成功", scoreData);

        log.info("质量分数计算成功, 表ID: {}, 分数: {}", tableId, score);
        return ResponseEntity.ok(result);
    }

    // ==================== 内部请求 DTO ====================

    /**
     * 质量指标请求 DTO
     */
    @Data
    @NoArgsConstructor
    public static class QualityMetricsRequest {

        /** 表ID */
        @NotNull(message = "表ID不能为空")
        private Long tableId;

        /** 记录数 */
        private Long recordCount;

        /** 空值率(0-1) */
        @DecimalMin(value = "0.0", message = "空值率不能小于0")
        @DecimalMax(value = "1.0", message = "空值率不能大于1")
        private BigDecimal nullRate;

        /** 更新频率 (DAILY/WEEKLY/MONTHLY) */
        private String updateFrequency;

        /** 数据新鲜度(小时) */
        private Integer dataFreshnessHours;

        /** 测量时间（为空时使用当前时间） */
        private LocalDateTime measuredAt;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 将请求 DTO 转换为实体
     */
    private QualityMetrics convertToEntity(QualityMetricsRequest request) {
        QualityMetrics metrics = new QualityMetrics();
        metrics.setTableId(request.getTableId());
        metrics.setRecordCount(request.getRecordCount());
        metrics.setNullRate(request.getNullRate());
        metrics.setUpdateFrequency(request.getUpdateFrequency());
        metrics.setDataFreshnessHours(request.getDataFreshnessHours());
        metrics.setMeasuredAt(request.getMeasuredAt() != null ? request.getMeasuredAt() : LocalDateTime.now());
        return metrics;
    }

    /**
     * 将实体转换为响应 DTO
     */
    private QualityMetricsResponse convertToResponse(QualityMetrics metrics) {
        return QualityMetricsResponse.builder()
            .id(metrics.getId())
            .tableId(metrics.getTableId())
            .recordCount(metrics.getRecordCount())
            .nullRate(metrics.getNullRate())
            .updateFrequency(metrics.getUpdateFrequency())
            .dataFreshnessHours(metrics.getDataFreshnessHours())
            .measuredAt(metrics.getMeasuredAt())
            .createdAt(metrics.getCreatedAt())
            .build();
    }

    /**
     * 根据分数获取等级
     */
    private String getScoreGrade(double score) {
        if (score >= 90) return "优秀";
        if (score >= 75) return "良好";
        if (score >= 60) return "合格";
        return "不合格";
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
}
