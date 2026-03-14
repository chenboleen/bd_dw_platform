package com.kiro.metadata.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 导出任务实体类
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("export_task")
public class ExportTask {
    
    /**
     * 任务ID - 主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 任务类型
     */
    @NotNull(message = "任务类型不能为空")
    @TableField("task_type")
    private ExportType taskType;
    
    /**
     * 过滤条件(JSON字符串)
     */
    @TableField("filters")
    private String filters;
    
    /**
     * 任务状态
     */
    @NotNull(message = "任务状态不能为空")
    @TableField("status")
    private TaskStatus status;
    
    /**
     * 文件路径
     */
    @TableField("file_path")
    private String filePath;
    
    /**
     * 记录数
     */
    @TableField("record_count")
    private Integer recordCount;
    
    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;
    
    /**
     * 创建时间 - 自动填充
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 创建人ID
     */
    @NotNull(message = "创建人ID不能为空")
    @TableField("created_by")
    private Long createdBy;
    
    /**
     * 开始时间
     */
    @TableField("started_at")
    private LocalDateTime startedAt;
    
    /**
     * 完成时间
     */
    @TableField("completed_at")
    private LocalDateTime completedAt;
}
