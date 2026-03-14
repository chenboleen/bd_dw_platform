package com.kiro.metadata.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 高级过滤请求 DTO
 * 包含过滤条件和分页参数，用于 POST /api/v1/search/filter 接口
 *
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "高级过滤请求")
public class FilterRequest {

    /**
     * 数据库名过滤
     */
    @Schema(description = "数据库名过滤", example = "dw_prod")
    private String databaseName;

    /**
     * 表类型过滤（TABLE/VIEW/EXTERNAL）
     */
    @Schema(description = "表类型过滤", example = "TABLE", allowableValues = {"TABLE", "VIEW", "EXTERNAL"})
    private String tableType;

    /**
     * 所有者ID过滤
     */
    @Schema(description = "所有者ID过滤", example = "1")
    private Long ownerId;

    /**
     * 更新时间起始（ISO 8601 格式，如 2024-01-01T00:00:00）
     */
    @Schema(description = "更新时间起始（ISO 8601 格式）", example = "2024-01-01T00:00:00")
    private String updatedFrom;

    /**
     * 更新时间截止（ISO 8601 格式，如 2024-12-31T23:59:59）
     */
    @Schema(description = "更新时间截止（ISO 8601 格式）", example = "2024-12-31T23:59:59")
    private String updatedTo;

    /**
     * 页码（从1开始，默认1）
     */
    @Schema(description = "页码，从1开始", example = "1", defaultValue = "1")
    private Integer page = 1;

    /**
     * 每页大小（默认20）
     */
    @Schema(description = "每页大小", example = "20", defaultValue = "20")
    private Integer pageSize = 20;
}
