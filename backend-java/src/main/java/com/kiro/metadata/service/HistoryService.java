package com.kiro.metadata.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiro.metadata.entity.ChangeHistory;
import com.kiro.metadata.entity.OperationType;
import com.kiro.metadata.repository.ChangeHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 变更历史服务
 * 提供变更记录的查询、对比和分析功能
 * 
 * @author Kiro
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryService {
    
    private final ChangeHistoryRepository changeHistoryRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 记录变更
     * 
     * @param entityType 实体类型
     * @param entityId 实体ID
     * @param operation 操作类型
     * @param fieldName 字段名
     * @param oldValue 旧值
     * @param newValue 新值
     * @param userId 用户ID
     * @return 变更记录
     */
    @Transactional(rollbackFor = Exception.class)
    public ChangeHistory recordChange(String entityType, Long entityId, 
                                     OperationType operation, String fieldName, 
                                     Object oldValue, Object newValue, Long userId) {
        log.debug("记录变更, 实体类型: {}, 实体ID: {}, 操作: {}", 
                 entityType, entityId, operation);
        
        try {
            ChangeHistory history = new ChangeHistory();
            history.setEntityType(entityType);
            history.setEntityId(entityId);
            history.setOperation(operation);
            history.setFieldName(fieldName);
            
            // 将对象序列化为JSON字符串
            if (oldValue != null) {
                history.setOldValue(objectMapper.writeValueAsString(oldValue));
            }
            if (newValue != null) {
                history.setNewValue(objectMapper.writeValueAsString(newValue));
            }
            
            history.setChangedAt(LocalDateTime.now());
            history.setChangedBy(userId);
            
            changeHistoryRepository.insert(history);
            
            log.debug("变更记录成功, ID: {}", history.getId());
            return history;
            
        } catch (Exception e) {
            log.error("记录变更失败: {}", e.getMessage(), e);
            throw new RuntimeException("记录变更失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取实体的变更历史
     * 
     * @param entityType 实体类型
     * @param entityId 实体ID
     * @param page 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public Page<ChangeHistory> getEntityHistory(String entityType, Long entityId, 
                                               int page, int pageSize) {
        log.info("查询实体变更历史, 类型: {}, ID: {}", entityType, entityId);
        
        QueryWrapper<ChangeHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("entity_type", entityType)
                   .eq("entity_id", entityId)
                   .orderByDesc("changed_at");
        
        Page<ChangeHistory> pageParam = new Page<>(page, pageSize);
        return changeHistoryRepository.selectPage(pageParam, queryWrapper);
    }
    
    /**
     * 获取用户的操作历史
     * 
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public Page<ChangeHistory> getUserActivity(Long userId, int page, int pageSize) {
        log.info("查询用户操作历史, 用户ID: {}", userId);
        
        QueryWrapper<ChangeHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("changed_by", userId)
                   .orderByDesc("changed_at");
        
        Page<ChangeHistory> pageParam = new Page<>(page, pageSize);
        return changeHistoryRepository.selectPage(pageParam, queryWrapper);
    }
    
    /**
     * 对比两个版本的差异
     * 
     * @param entityType 实体类型
     * @param entityId 实体ID
     * @param version1Time 版本1时间
     * @param version2Time 版本2时间
     * @return 差异列表
     */
    public List<Map<String, Object>> compareVersions(String entityType, Long entityId, 
                                                     LocalDateTime version1Time, 
                                                     LocalDateTime version2Time) {
        log.info("对比版本差异, 实体类型: {}, ID: {}", entityType, entityId);
        
        // 查询版本1的状态
        QueryWrapper<ChangeHistory> query1 = new QueryWrapper<>();
        query1.eq("entity_type", entityType)
             .eq("entity_id", entityId)
             .le("changed_at", version1Time)
             .orderByDesc("changed_at");
        List<ChangeHistory> history1 = changeHistoryRepository.selectList(query1);
        
        // 查询版本2的状态
        QueryWrapper<ChangeHistory> query2 = new QueryWrapper<>();
        query2.eq("entity_type", entityType)
             .eq("entity_id", entityId)
             .le("changed_at", version2Time)
             .orderByDesc("changed_at");
        List<ChangeHistory> history2 = changeHistoryRepository.selectList(query2);
        
        // 构建版本1的状态
        Map<String, String> state1 = buildState(history1);
        
        // 构建版本2的状态
        Map<String, String> state2 = buildState(history2);
        
        // 对比差异
        List<Map<String, Object>> diffs = new ArrayList<>();
        
        // 检查所有字段
        for (String field : state2.keySet()) {
            String value1 = state1.get(field);
            String value2 = state2.get(field);
            
            if (value1 == null || !value1.equals(value2)) {
                Map<String, Object> diff = new HashMap<>();
                diff.put("field", field);
                diff.put("oldValue", value1);
                diff.put("newValue", value2);
                diffs.add(diff);
            }
        }
        
        log.info("找到{}处差异", diffs.size());
        return diffs;
    }
    
    /**
     * 获取最近的变更记录
     * 
     * @param hours 小时数
     * @param limit 限制数量
     * @return 变更记录列表
     */
    public List<ChangeHistory> getRecentChanges(int hours, int limit) {
        log.info("查询最近{}小时的变更记录", hours);
        
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        
        QueryWrapper<ChangeHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.ge("changed_at", startTime)
                   .orderByDesc("changed_at")
                   .last("LIMIT " + limit);
        
        return changeHistoryRepository.selectList(queryWrapper);
    }
    
    /**
     * 获取实体的变更统计
     * 
     * @param entityType 实体类型
     * @param entityId 实体ID
     * @return 统计信息
     */
    public Map<String, Object> getChangeStatistics(String entityType, Long entityId) {
        log.info("查询变更统计, 实体类型: {}, ID: {}", entityType, entityId);
        
        QueryWrapper<ChangeHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("entity_type", entityType)
                   .eq("entity_id", entityId);
        
        List<ChangeHistory> history = changeHistoryRepository.selectList(queryWrapper);
        
        // 统计各类操作的数量
        Map<String, Integer> operationCount = new HashMap<>();
        operationCount.put("CREATE", 0);
        operationCount.put("UPDATE", 0);
        operationCount.put("DELETE", 0);
        
        LocalDateTime firstChange = null;
        LocalDateTime lastChange = null;
        
        for (ChangeHistory change : history) {
            String operation = change.getOperation().name();
            operationCount.put(operation, operationCount.getOrDefault(operation, 0) + 1);
            
            if (firstChange == null || change.getChangedAt().isBefore(firstChange)) {
                firstChange = change.getChangedAt();
            }
            if (lastChange == null || change.getChangedAt().isAfter(lastChange)) {
                lastChange = change.getChangedAt();
            }
        }
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalChanges", history.size());
        statistics.put("operationCount", operationCount);
        statistics.put("firstChange", firstChange);
        statistics.put("lastChange", lastChange);
        
        return statistics;
    }
    
    /**
     * 根据变更历史构建状态
     */
    private Map<String, String> buildState(List<ChangeHistory> history) {
        Map<String, String> state = new HashMap<>();
        
        // 从最早的变更开始应用
        for (int i = history.size() - 1; i >= 0; i--) {
            ChangeHistory change = history.get(i);
            
            if (change.getFieldName() != null) {
                // 字段级变更
                state.put(change.getFieldName(), change.getNewValue());
            } else {
                // 整体变更(CREATE/DELETE)
                if (change.getOperation() == OperationType.CREATE) {
                    // 解析新值中的所有字段
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> fields = objectMapper.readValue(
                            change.getNewValue(), Map.class);
                        for (Map.Entry<String, Object> entry : fields.entrySet()) {
                            state.put(entry.getKey(), 
                                objectMapper.writeValueAsString(entry.getValue()));
                        }
                    } catch (Exception e) {
                        log.error("解析变更值失败: {}", e.getMessage());
                    }
                }
            }
        }
        
        return state;
    }
}
