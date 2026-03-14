package com.kiro.metadata.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 数据目录实体类
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("catalog")
public class Catalog {
    
    /**
     * 目录ID - 主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 目录名称
     */
    @NotBlank(message = "目录名称不能为空")
    @TableField("name")
    private String name;
    
    /**
     * 目录描述
     */
    @TableField("description")
    private String description;
    
    /**
     * 父目录ID
     */
    @TableField("parent_id")
    private Long parentId;
    
    /**
     * 层级(1-5)
     */
    @NotNull(message = "层级不能为空")
    @Min(value = 1, message = "层级最小为1")
    @Max(value = 5, message = "层级最大为5")
    @TableField("level")
    private Integer level;
    
    /**
     * 路径 (如: /业务域/用户域/用户行为)
     */
    @NotBlank(message = "路径不能为空")
    @TableField("path")
    private String path;
    
    /**
     * 创建时间 - 自动填充
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间 - 自动填充
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    /**
     * 创建人ID
     */
    @NotNull(message = "创建人ID不能为空")
    @TableField("created_by")
    private Long createdBy;
}
