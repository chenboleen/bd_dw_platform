package com.kiro.metadata.dto.response;

import com.kiro.metadata.entity.LineageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 血缘关系响应 DTO
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineageResponse {
    
    /**
     * 血缘ID
     */
    private Long id;
    
    /**
     * 上游表ID
     */
    private Long sourceTableId;
    
    /**
     * 下游表ID
     */
    private Long targetTableId;
    
    /**
     * 血缘类型
     */
    private LineageType lineageType;
    
    /**
     * 转换逻辑
     */
    private String transformationLogic;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 创建人ID
     */
    private Long createdBy;
}
