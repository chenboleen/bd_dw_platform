package com.kiro.metadata.dto.response;

import com.kiro.metadata.entity.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 导出状态响应 DTO
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportStatusResponse {
    
    /**
     * 任务ID
     */
    private Long taskId;
    
    /**
     * 任务状态
     */
    private TaskStatus status;
    
    /**
     * 文件路径
     */
    private String filePath;
    
    /**
     * 记录数
     */
    private Integer recordCount;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 开始时间
     */
    private LocalDateTime startedAt;
    
    /**
     * 完成时间
     */
    private LocalDateTime completedAt;
}
