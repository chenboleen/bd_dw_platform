package com.kiro.metadata.dto.response;

import com.kiro.metadata.entity.LineageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 血缘图边 DTO
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineageEdge {
    
    /**
     * 上游表ID
     */
    private Long source;
    
    /**
     * 下游表ID
     */
    private Long target;
    
    /**
     * 边类型（upstream/downstream）
     */
    private String type;
    
    /**
     * 血缘类型
     */
    private LineageType lineageType;
    
    /**
     * 转换逻辑
     */
    private String transformationLogic;
}
