package com.kiro.metadata.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 影响分析报告 DTO
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImpactReport {
    
    /**
     * 分析的源表ID
     */
    private Long tableId;
    
    /**
     * 受影响的下游表ID列表
     */
    private List<Long> affectedTableIds;
    
    /**
     * 总影响表数
     */
    private Integer totalCount;
    
    /**
     * 最大影响深度
     */
    private Integer maxDepth;
    
    /**
     * 各层级影响表数分布（key: 层级, value: 该层级表数）
     */
    private Map<Integer, Integer> depthDistribution;
}
