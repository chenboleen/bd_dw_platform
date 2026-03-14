package com.kiro.metadata.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kiro.metadata.entity.ExportTask;
import com.kiro.metadata.entity.TaskStatus;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 导出任务 Repository
 * 
 * @author Kiro
 */
@Mapper
public interface ExportTaskRepository extends BaseMapper<ExportTask> {
    
    /**
     * 根据创建人ID查询导出任务
     * 
     * @param createdBy 创建人ID
     * @return 导出任务列表
     */
    default List<ExportTask> findByCreatedBy(Long createdBy) {
        return selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExportTask>()
                .eq(ExportTask::getCreatedBy, createdBy)
                .orderByDesc(ExportTask::getCreatedAt)
        );
    }
    
    /**
     * 根据状态查询导出任务
     * 
     * @param status 任务状态
     * @return 导出任务列表
     */
    default List<ExportTask> findByStatus(TaskStatus status) {
        return selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExportTask>()
                .eq(ExportTask::getStatus, status)
                .orderByDesc(ExportTask::getCreatedAt)
        );
    }
}
