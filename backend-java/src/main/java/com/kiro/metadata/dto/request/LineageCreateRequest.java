package com.kiro.metadata.dto.request;

import com.kiro.metadata.entity.LineageType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 血缘关系创建请求 DTO
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineageCreateRequest {
    
    /**
     * 上游表ID
     */
    @NotNull(message = "上游表ID不能为空")
    private Long sourceTableId;
    
    /**
     * 下游表ID
     */
    @NotNull(message = "下游表ID不能为空")
    private Long targetTableId;
    
    /**
     * 血缘类型
     */
    @NotNull(message = "血缘类型不能为空")
    private LineageType lineageType;
    
    /**
     * 转换逻辑
     */
    private String transformationLogic;
    
    /**
     * 创建人ID
     */
    @NotNull(message = "创建人ID不能为空")
    private Long createdBy;
}
