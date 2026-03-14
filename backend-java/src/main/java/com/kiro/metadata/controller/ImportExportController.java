package com.kiro.metadata.controller;

import com.kiro.metadata.dto.request.ExportRequest;
import com.kiro.metadata.dto.response.ExportStatusResponse;
import com.kiro.metadata.entity.ExportTask;
import com.kiro.metadata.entity.ExportType;
import com.kiro.metadata.service.ImportExportService;
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
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 导入导出 API 控制器
 *
 * 提供 CSV/JSON 格式的批量导入、异步导出任务创建、状态查询和文件下载接口
 *
 * @author Kiro
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/import-export")
@RequiredArgsConstructor
@Tag(name = "导入导出管理", description = "CSV/JSON 批量导入、异步导出任务管理和文件下载接口")
public class ImportExportController {

    private final ImportExportService importExportService;

    /**
     * 导入 CSV 文件
     * 从 CSV 文件批量导入表元数据，DEVELOPER 或 ADMIN 角色可操作
     *
     * @param file CSV 文件（multipart/form-data）
     * @return 导入结果（成功数、失败数、错误详情）
     */
    @PostMapping(value = "/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    @Operation(
        summary = "导入 CSV 文件",
        description = "从 CSV 文件批量导入表元数据。CSV 文件必须包含 database_name、table_name、table_type 列，需要 DEVELOPER 或 ADMIN 角色",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "导入完成（包含成功数和失败数）"),
        @ApiResponse(responseCode = "400", description = "文件格式错误或缺少必需列"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public ResponseEntity<Map<String, Object>> importFromCsv(
            @Parameter(description = "CSV 文件", required = true)
            @RequestParam("file") MultipartFile file) {
        log.info("导入 CSV 文件请求, 文件名: {}, 大小: {} bytes",
            file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            Map<String, Object> error = buildErrorResponse("文件不能为空");
            return ResponseEntity.badRequest().body(error);
        }

        Long userId = getCurrentUserId();
        Map<String, Object> importResult = importExportService.importFromCsv(file, userId);

        Map<String, Object> result = buildSuccessResponse("CSV 导入完成", importResult);

        log.info("CSV 导入完成, 成功: {}, 失败: {}",
            importResult.get("successCount"), importResult.get("failCount"));
        return ResponseEntity.ok(result);
    }

    /**
     * 导入 JSON 文件
     * 从 JSON 文件批量导入表元数据，DEVELOPER 或 ADMIN 角色可操作
     *
     * @param file JSON 文件（multipart/form-data）
     * @return 导入结果（成功数、失败数、错误详情）
     */
    @PostMapping(value = "/import/json", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    @Operation(
        summary = "导入 JSON 文件",
        description = "从 JSON 文件批量导入表元数据。JSON 文件应为数组格式，每个对象包含 databaseName、tableName、tableType 字段，需要 DEVELOPER 或 ADMIN 角色",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "导入完成（包含成功数和失败数）"),
        @ApiResponse(responseCode = "400", description = "文件格式错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public ResponseEntity<Map<String, Object>> importFromJson(
            @Parameter(description = "JSON 文件", required = true)
            @RequestParam("file") MultipartFile file) {
        log.info("导入 JSON 文件请求, 文件名: {}, 大小: {} bytes",
            file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            Map<String, Object> error = buildErrorResponse("文件不能为空");
            return ResponseEntity.badRequest().body(error);
        }

        Long userId = getCurrentUserId();
        Map<String, Object> importResult = importExportService.importFromJson(file, userId);

        Map<String, Object> result = buildSuccessResponse("JSON 导入完成", importResult);

        log.info("JSON 导入完成, 成功: {}, 失败: {}",
            importResult.get("successCount"), importResult.get("failCount"));
        return ResponseEntity.ok(result);
    }

    /**
     * 创建导出任务
     * 创建异步导出任务，支持 CSV 和 JSON 格式，DEVELOPER 或 ADMIN 角色可操作
     *
     * @param request 导出请求（包含导出类型和过滤条件）
     * @return 导出任务 ID
     */
    @PostMapping("/export")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    @Operation(
        summary = "创建导出任务",
        description = "创建异步导出任务，支持 CSV 和 JSON 格式。任务创建后立即返回任务 ID，导出在后台异步执行，需要 DEVELOPER 或 ADMIN 角色",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "任务创建成功，异步执行中"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public ResponseEntity<Map<String, Object>> createExportTask(
            @Valid @RequestBody ExportRequest request) {
        log.info("创建导出任务请求, 类型: {}", request.getExportType());

        // 解析导出类型
        ExportType exportType;
        try {
            exportType = ExportType.valueOf(request.getExportType().toUpperCase());
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = buildErrorResponse("不支持的导出类型: " + request.getExportType() + "，支持 CSV 和 JSON");
            return ResponseEntity.badRequest().body(error);
        }

        Long userId = getCurrentUserId();
        Long taskId = importExportService.createExportTask(
            exportType,
            request.getFilters(),
            userId
        );

        // 根据类型触发异步导出
        if (exportType == ExportType.CSV) {
            importExportService.exportToCsv(taskId);
        } else {
            importExportService.exportToJson(taskId);
        }

        Map<String, Object> taskData = new HashMap<>();
        taskData.put("taskId", taskId);
        taskData.put("exportType", exportType.name());
        taskData.put("status", "PENDING");

        Map<String, Object> result = buildSuccessResponse("导出任务创建成功，正在后台处理", taskData);

        log.info("导出任务创建成功, 任务ID: {}, 类型: {}", taskId, exportType);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    /**
     * 查询导出任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态信息
     */
    @GetMapping("/export/{taskId}/status")
    @Operation(
        summary = "查询导出任务状态",
        description = "查询指定导出任务的当前状态，包括 PENDING（等待中）、RUNNING（执行中）、COMPLETED（已完成）、FAILED（失败）",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "查询成功",
            content = @Content(schema = @Schema(implementation = ExportStatusResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "任务不存在")
    })
    public ResponseEntity<Map<String, Object>> getExportStatus(
            @Parameter(description = "导出任务ID", required = true)
            @PathVariable Long taskId) {
        log.info("查询导出任务状态请求, 任务ID: {}", taskId);

        ExportTask task = importExportService.getExportStatus(taskId);

        ExportStatusResponse response = convertToStatusResponse(task);
        Map<String, Object> result = buildSuccessResponse("查询导出任务状态成功", response);

        log.info("导出任务状态查询成功, 任务ID: {}, 状态: {}", taskId, task.getStatus());
        return ResponseEntity.ok(result);
    }

    /**
     * 下载导出文件
     * 下载已完成的导出任务生成的文件
     *
     * @param taskId 任务ID
     * @return 文件流
     */
    @GetMapping("/export/{taskId}/download")
    @Operation(
        summary = "下载导出文件",
        description = "下载已完成的导出任务生成的文件，任务状态必须为 COMPLETED",
        security = @SecurityRequirement(name = "Bearer认证")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "下载成功，返回文件流"),
        @ApiResponse(responseCode = "400", description = "任务未完成，无法下载"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "任务不存在或文件不存在")
    })
    public ResponseEntity<?> downloadExportFile(
            @Parameter(description = "导出任务ID", required = true)
            @PathVariable Long taskId) {
        log.info("下载导出文件请求, 任务ID: {}", taskId);

        try {
            // 先查询任务状态
            ExportTask task = importExportService.getExportStatus(taskId);

            // 获取文件流
            InputStream fileStream = importExportService.downloadExportFile(taskId);

            // 确定文件名和 Content-Type
            String fileName = buildFileName(task);
            String contentType = task.getTaskType() == ExportType.CSV
                ? "text/csv;charset=UTF-8"
                : "application/json;charset=UTF-8";

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + fileName + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, contentType);

            log.info("导出文件下载成功, 任务ID: {}, 文件名: {}", taskId, fileName);
            return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(contentType))
                .body(new InputStreamResource(fileStream));

        } catch (IllegalStateException e) {
            // 任务未完成
            Map<String, Object> error = buildErrorResponse(e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (IOException e) {
            log.error("下载导出文件失败, 任务ID: {}", taskId, e);
            Map<String, Object> error = buildErrorResponse("文件下载失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 将导出任务实体转换为状态响应 DTO
     */
    private ExportStatusResponse convertToStatusResponse(ExportTask task) {
        return ExportStatusResponse.builder()
            .taskId(task.getId())
            .status(task.getStatus())
            .filePath(task.getFilePath())
            .recordCount(task.getRecordCount())
            .errorMessage(task.getErrorMessage())
            .createdAt(task.getCreatedAt())
            .startedAt(task.getStartedAt())
            .completedAt(task.getCompletedAt())
            .build();
    }

    /**
     * 根据任务信息构建下载文件名
     */
    private String buildFileName(ExportTask task) {
        String extension = task.getTaskType() == ExportType.CSV ? "csv" : "json";
        if (task.getFilePath() != null) {
            // 从文件路径中提取文件名
            String[] parts = task.getFilePath().replace("\\", "/").split("/");
            return parts[parts.length - 1];
        }
        return "export_" + task.getId() + "." + extension;
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
     * 构建错误响应格式
     */
    private Map<String, Object> buildErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("data", null);
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
