package com.kiro.metadata.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kiro.metadata.dto.response.ImpactReport;
import com.kiro.metadata.dto.response.LineageEdge;
import com.kiro.metadata.dto.response.LineageNode;
import com.kiro.metadata.entity.*;
import com.kiro.metadata.repository.ChangeHistoryRepository;
import com.kiro.metadata.repository.LineageRepository;
import com.kiro.metadata.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 血缘关系服务
 * 提供血缘关系的CRUD操作、图谱生成、循环检测和影响分析
 * 
 * @author Kiro
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LineageService {
    
    private final LineageRepository lineageRepository;
    private final TableRepository tableRepository;
    private final ChangeHistoryRepository changeHistoryRepository;
    
    /**
     * 创建血缘关系
     * 
     * @param lineage 血缘关系
     * @return 创建的血缘关系
     */
    @Transactional(rollbackFor = Exception.class)
    public Lineage createLineage(Lineage lineage) {
        log.info("创建血缘关系: {} -> {}", lineage.getSourceTableId(), lineage.getTargetTableId());
        
        // 验证源表和目标表存在
        TableMetadata sourceTable = tableRepository.selectById(lineage.getSourceTableId());
        if (sourceTable == null) {
            throw new IllegalArgumentException("源表不存在, ID: " + lineage.getSourceTableId());
        }
        
        TableMetadata targetTable = tableRepository.selectById(lineage.getTargetTableId());
        if (targetTable == null) {
            throw new IllegalArgumentException("目标表不存在, ID: " + lineage.getTargetTableId());
        }
        
        // 检测是否会形成循环依赖
        List<Long> cycle = detectCircularDependency(lineage.getTargetTableId(), lineage.getSourceTableId());
        if (!cycle.isEmpty()) {
            throw new IllegalArgumentException(
                "创建血缘关系会形成循环依赖: " + cycle
            );
        }
        
        // 保存血缘关系
        lineageRepository.insert(lineage);
        
        // 记录变更历史
        recordChange(lineage.getId(), OperationType.CREATE, null, 
                    null, lineage.toString(), lineage.getCreatedBy());
        
        log.info("血缘关系创建成功, ID: {}", lineage.getId());
        return lineage;
    }
    
    /**
     * 根据ID获取血缘关系
     * 
     * @param lineageId 血缘ID
     * @return 血缘关系
     */
    public Lineage getLineageById(Long lineageId) {
        log.debug("查询血缘关系, ID: {}", lineageId);
        
        Lineage lineage = lineageRepository.selectById(lineageId);
        if (lineage == null) {
            throw new IllegalArgumentException("血缘关系不存在, ID: " + lineageId);
        }
        
        return lineage;
    }
    
    /**
     * 删除血缘关系
     * 
     * @param lineageId 血缘ID
     * @param userId 操作用户ID
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteLineage(Long lineageId, Long userId) {
        log.info("删除血缘关系, ID: {}", lineageId);
        
        // 查询血缘关系
        Lineage lineage = getLineageById(lineageId);
        
        // 记录变更历史
        recordChange(lineageId, OperationType.DELETE, null, 
                    lineage.toString(), null, userId);
        
        // 删除血缘关系
        int result = lineageRepository.deleteById(lineageId);
        
        log.info("血缘关系删除成功, ID: {}", lineageId);
        return result > 0;
    }
    
    /**
     * 获取上游表列表
     * 
     * @param tableId 表ID
     * @return 上游表ID列表
     */
    public List<Long> getUpstreamTables(Long tableId) {
        log.debug("查询上游表, 表ID: {}", tableId);
        
        QueryWrapper<Lineage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("target_table_id", tableId);
        
        List<Lineage> lineages = lineageRepository.selectList(queryWrapper);
        List<Long> upstreamTableIds = new ArrayList<>();
        
        for (Lineage lineage : lineages) {
            upstreamTableIds.add(lineage.getSourceTableId());
        }
        
        return upstreamTableIds;
    }
    
    /**
     * 获取下游表列表
     * 
     * @param tableId 表ID
     * @return 下游表ID列表
     */
    public List<Long> getDownstreamTables(Long tableId) {
        log.debug("查询下游表, 表ID: {}", tableId);
        
        QueryWrapper<Lineage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("source_table_id", tableId);
        
        List<Lineage> lineages = lineageRepository.selectList(queryWrapper);
        List<Long> downstreamTableIds = new ArrayList<>();
        
        for (Lineage lineage : lineages) {
            downstreamTableIds.add(lineage.getTargetTableId());
        }
        
        return downstreamTableIds;
    }
    
    /**
     * 构建血缘图谱
     * 
     * @param tableId 表ID
     * @param direction 方向(upstream/downstream/both)
     * @param maxDepth 最大深度(1-5)
     * @return 血缘图谱(节点和边)
     */
    public Map<String, Object> buildLineageGraph(Long tableId, String direction, int maxDepth) {
        log.info("构建血缘图谱, 表ID: {}, 方向: {}, 深度: {}", tableId, direction, maxDepth);
        
        // 验证深度
        if (maxDepth < 1 || maxDepth > 5) {
            throw new IllegalArgumentException("深度必须在1-5之间");
        }
        
        // 验证表存在
        TableMetadata table = tableRepository.selectById(tableId);
        if (table == null) {
            throw new IllegalArgumentException("表不存在, ID: " + tableId);
        }
        
        Set<Long> visited = new HashSet<>();
        List<LineageNode> nodes = new ArrayList<>();
        List<LineageEdge> edges = new ArrayList<>();
        
        // 添加根节点
        nodes.add(createNode(table, 0));
        visited.add(tableId);
        
        // 根据方向遍历
        if ("upstream".equals(direction) || "both".equals(direction)) {
            dfsUpstream(tableId, 1, maxDepth, visited, nodes, edges);
        }
        
        if ("downstream".equals(direction) || "both".equals(direction)) {
            dfsDownstream(tableId, 1, maxDepth, visited, nodes, edges);
        }
        
        Map<String, Object> graph = new HashMap<>();
        graph.put("nodes", nodes);
        graph.put("edges", edges);
        
        log.info("血缘图谱构建完成, 节点数: {}, 边数: {}", nodes.size(), edges.size());
        return graph;
    }
    
    /**
     * 获取两表之间的血缘路径
     * 
     * @param sourceTableId 源表ID
     * @param targetTableId 目标表ID
     * @return 路径中的表ID列表
     */
    public List<Long> getLineagePath(Long sourceTableId, Long targetTableId) {
        log.info("查询血缘路径: {} -> {}", sourceTableId, targetTableId);
        
        Set<Long> visited = new HashSet<>();
        List<Long> path = new ArrayList<>();
        path.add(sourceTableId);
        
        if (dfsPath(sourceTableId, targetTableId, visited, path)) {
            return path;
        }
        
        return Collections.emptyList();
    }
    
    /**
     * 检测循环依赖
     * 
     * @param tableId 表ID
     * @return 循环路径中的表ID列表(如果存在循环)
     */
    public List<Long> detectCircularDependency(Long tableId) {
        return detectCircularDependency(tableId, null);
    }
    
    /**
     * 检测循环依赖(内部方法,支持新增边的检测)
     * 
     * @param tableId 表ID
     * @param newTargetId 新增的目标表ID(用于检测新增边是否会形成循环)
     * @return 循环路径中的表ID列表(如果存在循环)
     */
    private List<Long> detectCircularDependency(Long tableId, Long newTargetId) {
        log.debug("检测循环依赖, 表ID: {}", tableId);
        
        Set<Long> visited = new HashSet<>();
        List<Long> path = new ArrayList<>();
        
        if (dfsCycle(tableId, newTargetId, visited, path)) {
            return path;
        }
        
        return Collections.emptyList();
    }
    
    /**
     * 影响分析
     * 
     * @param tableId 表ID
     * @return 影响报告
     */
    public ImpactReport analyzeImpact(Long tableId) {
        log.info("分析影响范围, 表ID: {}", tableId);
        
        Set<Long> affectedTables = new HashSet<>();
        Map<Integer, Integer> depthCount = new HashMap<>();
        
        // 递归查找所有下游表
        analyzeImpactRecursive(tableId, 1, affectedTables, depthCount);
        
        ImpactReport report = new ImpactReport();
        report.setTableId(tableId);
        report.setAffectedTableIds(new ArrayList<>(affectedTables));
        report.setTotalCount(affectedTables.size());
        report.setMaxDepth(depthCount.isEmpty() ? 0 : Collections.max(depthCount.keySet()));
        report.setDepthDistribution(depthCount);
        
        log.info("影响分析完成, 影响表数: {}, 最大深度: {}", 
                report.getTotalCount(), report.getMaxDepth());
        
        return report;
    }
    
    /**
     * DFS遍历上游(递归)
     */
    private void dfsUpstream(Long tableId, int depth, int maxDepth, 
                            Set<Long> visited, List<LineageNode> nodes, 
                            List<LineageEdge> edges) {
        if (depth > maxDepth) {
            return;
        }
        
        List<Long> upstreamIds = getUpstreamTables(tableId);
        
        for (Long upstreamId : upstreamIds) {
            if (!visited.contains(upstreamId)) {
                visited.add(upstreamId);
                
                // 添加节点
                TableMetadata table = tableRepository.selectById(upstreamId);
                if (table != null) {
                    nodes.add(createNode(table, depth));
                    
                    // 添加边
                    edges.add(createEdge(upstreamId, tableId, "upstream"));
                    
                    // 递归遍历
                    dfsUpstream(upstreamId, depth + 1, maxDepth, visited, nodes, edges);
                }
            }
        }
    }
    
    /**
     * DFS遍历下游(递归)
     */
    private void dfsDownstream(Long tableId, int depth, int maxDepth, 
                              Set<Long> visited, List<LineageNode> nodes, 
                              List<LineageEdge> edges) {
        if (depth > maxDepth) {
            return;
        }
        
        List<Long> downstreamIds = getDownstreamTables(tableId);
        
        for (Long downstreamId : downstreamIds) {
            if (!visited.contains(downstreamId)) {
                visited.add(downstreamId);
                
                // 添加节点
                TableMetadata table = tableRepository.selectById(downstreamId);
                if (table != null) {
                    nodes.add(createNode(table, depth));
                    
                    // 添加边
                    edges.add(createEdge(tableId, downstreamId, "downstream"));
                    
                    // 递归遍历
                    dfsDownstream(downstreamId, depth + 1, maxDepth, visited, nodes, edges);
                }
            }
        }
    }
    
    /**
     * DFS查找路径(递归)
     */
    private boolean dfsPath(Long currentId, Long targetId, 
                           Set<Long> visited, List<Long> path) {
        if (currentId.equals(targetId)) {
            return true;
        }
        
        visited.add(currentId);
        List<Long> downstreamIds = getDownstreamTables(currentId);
        
        for (Long downstreamId : downstreamIds) {
            if (!visited.contains(downstreamId)) {
                path.add(downstreamId);
                
                if (dfsPath(downstreamId, targetId, visited, path)) {
                    return true;
                }
                
                path.remove(path.size() - 1);
            }
        }
        
        return false;
    }
    
    /**
     * DFS检测循环(递归)
     */
    private boolean dfsCycle(Long currentId, Long newTargetId, 
                            Set<Long> visited, List<Long> path) {
        // 如果当前节点已在路径中,发现循环
        if (path.contains(currentId)) {
            path.add(currentId);
            return true;
        }
        
        // 如果已访问过,跳过
        if (visited.contains(currentId)) {
            return false;
        }
        
        visited.add(currentId);
        path.add(currentId);
        
        // 获取下游表
        List<Long> downstreamIds = getDownstreamTables(currentId);
        
        // 如果是检测新增边,添加新的目标表
        if (newTargetId != null && currentId.equals(path.get(0))) {
            downstreamIds = new ArrayList<>(downstreamIds);
            downstreamIds.add(newTargetId);
        }
        
        // 递归检测
        for (Long downstreamId : downstreamIds) {
            if (dfsCycle(downstreamId, null, visited, path)) {
                return true;
            }
        }
        
        path.remove(path.size() - 1);
        return false;
    }
    
    /**
     * 递归分析影响范围
     */
    private void analyzeImpactRecursive(Long tableId, int depth, 
                                       Set<Long> affectedTables, 
                                       Map<Integer, Integer> depthCount) {
        List<Long> downstreamIds = getDownstreamTables(tableId);
        
        for (Long downstreamId : downstreamIds) {
            if (!affectedTables.contains(downstreamId)) {
                affectedTables.add(downstreamId);
                depthCount.put(depth, depthCount.getOrDefault(depth, 0) + 1);
                
                // 递归分析
                analyzeImpactRecursive(downstreamId, depth + 1, affectedTables, depthCount);
            }
        }
    }
    
    /**
     * 创建节点
     */
    private LineageNode createNode(TableMetadata table, int depth) {
        LineageNode node = new LineageNode();
        node.setId(table.getId());
        node.setName(table.getDatabaseName() + "." + table.getTableName());
        node.setDatabaseName(table.getDatabaseName());
        node.setTableName(table.getTableName());
        node.setTableType(table.getTableType().name());
        node.setDepth(depth);
        return node;
    }
    
    /**
     * 创建边
     */
    private LineageEdge createEdge(Long sourceId, Long targetId, String type) {
        LineageEdge edge = new LineageEdge();
        edge.setSource(sourceId);
        edge.setTarget(targetId);
        edge.setType(type);
        return edge;
    }
    
    /**
     * 记录变更历史
     */
    private void recordChange(Long entityId, OperationType operation, 
                             String fieldName, String oldValue, 
                             String newValue, Long userId) {
        try {
            ChangeHistory history = new ChangeHistory();
            history.setEntityType("LINEAGE");
            history.setEntityId(entityId);
            history.setOperation(operation);
            history.setFieldName(fieldName);
            history.setOldValue(oldValue);
            history.setNewValue(newValue);
            history.setChangedAt(LocalDateTime.now());
            history.setChangedBy(userId);
            
            changeHistoryRepository.insert(history);
        } catch (Exception e) {
            log.error("记录变更历史失败: {}", e.getMessage(), e);
        }
    }
}
