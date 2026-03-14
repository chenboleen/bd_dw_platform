package com.kiro.metadata.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kiro.metadata.entity.Catalog;
import com.kiro.metadata.entity.ChangeHistory;
import com.kiro.metadata.entity.OperationType;
import com.kiro.metadata.entity.TableMetadata;
import com.kiro.metadata.repository.CatalogRepository;
import com.kiro.metadata.repository.ChangeHistoryRepository;
import com.kiro.metadata.repository.TableRepository;
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
 * 数据目录服务
 * 提供目录的CRUD操作和树形结构管理
 * 
 * @author Kiro
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogService {
    
    private final CatalogRepository catalogRepository;
    private final TableRepository tableRepository;
    private final ChangeHistoryRepository changeHistoryRepository;
    
    /**
     * 创建目录节点
     * 
     * @param catalog 目录信息
     * @return 创建的目录
     */
    @Transactional(rollbackFor = Exception.class)
    public Catalog createCatalog(Catalog catalog) {
        log.info("创建目录节点: {}", catalog.getName());

        // 验证父目录存在并自动计算 level 和 path
        if (catalog.getParentId() != null) {
            Catalog parent = catalogRepository.selectById(catalog.getParentId());
            if (parent == null) {
                throw new IllegalArgumentException("父目录不存在, ID: " + catalog.getParentId());
            }

            int newLevel = parent.getLevel() + 1;
            if (newLevel > 5) {
                throw new IllegalArgumentException("目录层级超过限制（最多5层）");
            }

            // 自动计算层级和路径
            catalog.setLevel(newLevel);
            catalog.setPath(parent.getPath() + "/" + catalog.getName());
        } else {
            // 根目录
            catalog.setLevel(1);
            catalog.setPath("/" + catalog.getName());
        }

        // 保存目录
        catalogRepository.insert(catalog);

        // 记录变更历史
        recordChange(catalog.getId(), OperationType.CREATE, null,
                    null, catalog.toString(), catalog.getCreatedBy());

        log.info("目录节点创建成功, ID: {}", catalog.getId());
        return catalog;
    }
    
    /**
     * 获取目录树
     * 
     * @return 根节点列表
     */
    public List<Map<String, Object>> getCatalogTree() {
        log.info("获取目录树");
        
        // 查询所有目录节点
        QueryWrapper<Catalog> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("level", "name");
        List<Catalog> allCatalogs = catalogRepository.selectList(queryWrapper);
        
        // 构建树形结构
        Map<Long, Map<String, Object>> catalogMap = new HashMap<>();
        List<Map<String, Object>> rootNodes = new ArrayList<>();
        
        // 第一遍:创建所有节点
        for (Catalog catalog : allCatalogs) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", catalog.getId());
            node.put("name", catalog.getName());
            node.put("description", catalog.getDescription());
            node.put("level", catalog.getLevel());
            node.put("path", catalog.getPath());
            node.put("parentId", catalog.getParentId());
            node.put("children", new ArrayList<Map<String, Object>>());
            
            catalogMap.put(catalog.getId(), node);
        }
        
        // 第二遍:构建父子关系
        for (Catalog catalog : allCatalogs) {
            Map<String, Object> node = catalogMap.get(catalog.getId());
            
            if (catalog.getParentId() == null) {
                // 根节点
                rootNodes.add(node);
            } else {
                // 子节点
                Map<String, Object> parent = catalogMap.get(catalog.getParentId());
                if (parent != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> children = 
                        (List<Map<String, Object>>) parent.get("children");
                    children.add(node);
                }
            }
        }
        
        log.info("目录树构建完成, 根节点数: {}", rootNodes.size());
        return rootNodes;
    }
    
    /**
     * 移动目录节点
     * 
     * @param catalogId 目录ID
     * @param newParentId 新父目录ID
     * @param userId 操作用户ID
     * @return 更新后的目录
     */
    @Transactional(rollbackFor = Exception.class)
    public Catalog moveCatalog(Long catalogId, Long newParentId, Long userId) {
        log.info("移动目录节点, ID: {}, 新父目录ID: {}", catalogId, newParentId);
        
        // 查询目录
        Catalog catalog = catalogRepository.selectById(catalogId);
        if (catalog == null) {
            throw new IllegalArgumentException("目录不存在, ID: " + catalogId);
        }
        
        String oldValue = catalog.toString();
        
        // 查询新父目录
        if (newParentId != null) {
            Catalog newParent = catalogRepository.selectById(newParentId);
            if (newParent == null) {
                throw new IllegalArgumentException("新父目录不存在, ID: " + newParentId);
            }
            
            // 验证层级限制
            int newLevel = newParent.getLevel() + 1;
            if (newLevel > 5) {
                throw new IllegalArgumentException("移动后层级超过限制(最多5层)");
            }
            
            // 更新目录信息
            catalog.setParentId(newParentId);
            catalog.setLevel(newLevel);
            catalog.setPath(newParent.getPath() + "/" + catalog.getName());
            
        } else {
            // 移动到根目录
            catalog.setParentId(null);
            catalog.setLevel(1);
            catalog.setPath("/" + catalog.getName());
        }
        
        // 保存更新
        catalogRepository.updateById(catalog);
        
        // 记录变更历史
        recordChange(catalogId, OperationType.UPDATE, null, 
                    oldValue, catalog.toString(), userId);
        
        log.info("目录节点移动成功, ID: {}", catalogId);
        return catalog;
    }
    
    /**
     * 更新目录名称和描述
     * 同时递归更新所有子目录的 path
     *
     * @param id          目录ID
     * @param name        新名称
     * @param description 新描述
     * @param userId      操作用户ID
     * @return 更新后的目录
     */
    @Transactional(rollbackFor = Exception.class)
    public Catalog updateCatalog(Long id, String name, String description, Long userId) {
        log.info("更新目录, ID: {}, 新名称: {}", id, name);

        Catalog catalog = catalogRepository.selectById(id);
        if (catalog == null) {
            throw new IllegalArgumentException("目录不存在, ID: " + id);
        }

        String oldPath = catalog.getPath();
        String oldValue = catalog.toString();

        // 计算新 path
        String newPath;
        if (catalog.getParentId() != null) {
            Catalog parent = catalogRepository.selectById(catalog.getParentId());
            newPath = (parent != null ? parent.getPath() : "") + "/" + name;
        } else {
            newPath = "/" + name;
        }

        catalog.setName(name);
        catalog.setDescription(description);
        catalog.setPath(newPath);
        catalogRepository.updateById(catalog);

        // 递归更新所有子目录的 path（将旧前缀替换为新前缀）
        if (!oldPath.equals(newPath)) {
            List<Catalog> descendants = catalogRepository.findByPathPrefix(oldPath);
            for (Catalog child : descendants) {
                child.setPath(newPath + child.getPath().substring(oldPath.length()));
                catalogRepository.updateById(child);
            }
            log.info("递归更新子目录 path 完成, 影响 {} 个子目录", descendants.size());
        }

        // 记录变更历史
        recordChange(id, OperationType.UPDATE, "name/description", oldValue, catalog.toString(), userId);

        log.info("目录更新成功, ID: {}", id);
        return catalog;
    }

    /**
     * 删除目录节点（仅叶子节点可删除）
     *
     * @param id     目录ID
     * @param userId 操作用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCatalog(Long id, Long userId) {
        log.info("删除目录, ID: {}", id);

        Catalog catalog = catalogRepository.selectById(id);
        if (catalog == null) {
            throw new IllegalArgumentException("目录不存在, ID: " + id);
        }

        // 检查是否有子目录
        List<Catalog> children = catalogRepository.findByParentId(id);
        if (!children.isEmpty()) {
            throw new IllegalArgumentException("该目录下存在子目录，请先删除子目录");
        }

        // 检查是否有关联的表
        List<TableMetadata> tables = catalogRepository.getTablesInCatalog(id);
        if (!tables.isEmpty()) {
            throw new IllegalArgumentException("该目录下存在关联的表，请先移除关联");
        }

        catalogRepository.deleteById(id);
        recordChange(id, OperationType.DELETE, null, catalog.toString(), null, userId);
        log.info("目录删除成功, ID: {}", id);
    }

    /**
     * 将表添加到目录（一表一目录约束：自动移除旧关联）
     * 
     * @param tableId 表ID
     * @param catalogId 目录ID
     * @param userId 操作用户ID
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean addTableToCatalog(Long tableId, Long catalogId, Long userId) {
        log.info("将表添加到目录, 表ID: {}, 目录ID: {}", tableId, catalogId);
        
        // 验证表和目录存在
        TableMetadata table = tableRepository.selectById(tableId);
        if (table == null) {
            throw new IllegalArgumentException("表不存在, ID: " + tableId);
        }
        
        Catalog catalog = catalogRepository.selectById(catalogId);
        if (catalog == null) {
            throw new IllegalArgumentException("目录不存在, ID: " + catalogId);
        }

        // 一表一目录：先移除该表的所有旧目录关联
        catalogRepository.removeAllCatalogsFromTable(tableId);
        log.debug("已移除表 {} 的旧目录关联", tableId);
        
        // 插入新关联关系
        catalogRepository.addTableToCatalog(tableId, catalogId);
        
        log.info("表添加到目录成功");
        return true;
    }
    
    /**
     * 从目录移除表
     * 
     * @param tableId 表ID
     * @param catalogId 目录ID
     * @param userId 操作用户ID
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean removeTableFromCatalog(Long tableId, Long catalogId, Long userId) {
        log.info("从目录移除表, 表ID: {}, 目录ID: {}", tableId, catalogId);
        
        // 删除关联关系(使用原生SQL)
        catalogRepository.removeTableFromCatalog(tableId, catalogId);
        
        log.info("表从目录移除成功");
        return true;
    }
    
    /**
     * 获取目录下的表
     * 
     * @param catalogId 目录ID
     * @return 表列表
     */
    public List<TableMetadata> getTablesInCatalog(Long catalogId) {
        log.debug("查询目录下的表, 目录ID: {}", catalogId);
        
        // 验证目录存在
        Catalog catalog = catalogRepository.selectById(catalogId);
        if (catalog == null) {
            throw new IllegalArgumentException("目录不存在, ID: " + catalogId);
        }
        
        // 查询关联的表(使用原生SQL)
        return catalogRepository.getTablesInCatalog(catalogId);
    }
    
    /**
     * 获取所有目录节点（扁平列表，用于下拉选择）
     *
     * @return 所有目录列表
     */
    public List<Catalog> getAllCatalogs() {
        QueryWrapper<Catalog> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("level", "name");
        return catalogRepository.selectList(queryWrapper);
    }

    /**
     * 记录变更历史
     */
    private void recordChange(Long entityId, OperationType operation, 
                             String fieldName, String oldValue, 
                             String newValue, Long userId) {
        try {
            ChangeHistory history = new ChangeHistory();
            history.setEntityType("CATALOG");
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
