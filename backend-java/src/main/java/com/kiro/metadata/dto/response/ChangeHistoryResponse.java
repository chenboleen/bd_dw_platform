package com.kiro.metadata.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 变更历史响应 DTO
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeHistoryResponse {
    
    /**
     * 历史ID
     */
    private Long id;
    
    /**
     * 实体类型
     */
    private String entityType;
    
    /**
     * 实体ID
     */
    private Long entityId;
    
    /**
     * 操作类型
     */
    private String operation;
    
    /**
     * 字段名
     */
    private String fieldName;
    
    /**
     * 旧值(JSON)
     */
    private String oldValue;
    
    /**
     * 新值(JSON)
     */
    private String newValue;
    
    /**
     * 变更时间
     */
    private LocalDateTime changedAt;
    
    /**
     * 变更人ID
     */
    private Long changedBy;
}
