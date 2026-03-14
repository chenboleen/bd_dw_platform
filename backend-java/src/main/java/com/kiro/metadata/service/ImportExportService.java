package com.kiro.metadata.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiro.metadata.entity.*;
import com.kiro.metadata.repository.ExportTaskRepository;
import com.kiro.metadata.repository.TableRepository;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 导入导出服务
 * 提供CSV和JSON格式的批量导入导出功能
 * 
 * @author Kiro
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportExportService {
    
    private final TableRepository tableRepository;
    private final ExportTaskRepository exportTaskRepository;
    private final ObjectMapper objectMapper;
    
    private static final String EXPORT_DIR = "exports/";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    /**
     * 从CSV导入表元数据
     * 
     * @param file CSV文件
     * @param userId 用户ID
     * @return 导入结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importFromCsv(MultipartFile file, Long userId) {
        log.info("开始从CSV导入表元数据, 文件名: {}", file.getOriginalFilename());
        
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            // 读取表头
            String[] headers = reader.readNext();
            if (headers == null || headers.length < 3) {
                throw new IllegalArgumentException("CSV文件格式错误: 缺少必需的列");
            }
            
            // 验证必需列
            List<String> headerList = Arrays.asList(headers);
            if (!headerList.contains("database_name") || 
                !headerList.contains("table_name") || 
                !headerList.contains("table_type")) {
                throw new IllegalArgumentException(
                    "CSV文件格式错误: 必须包含 database_name, table_name, table_type 列"
                );
            }
            
            // 读取数据行
            String[] line;
            int rowNum = 1;
            while ((line = reader.readNext()) != null) {
                rowNum++;
                try {
                    TableMetadata table = parseCsvLine(headers, line, userId);
                    
                    // 检查是否已存在
                    QueryWrapper<TableMetadata> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("database_name", table.getDatabaseName())
                               .eq("table_name", table.getTableName());
                    
                    if (tableRepository.selectCount(queryWrapper) > 0) {
                        errors.add(String.format("第%d行: 表 %s.%s 已存在", 
                            rowNum, table.getDatabaseName(), table.getTableName()));
                        failCount++;
                        continue;
                    }
                    
                    // 插入表
                    tableRepository.insert(table);
                    successCount++;
                    
                } catch (Exception e) {
                    errors.add(String.format("第%d行: %s", rowNum, e.getMessage()));
                    failCount++;
                    log.error("导入第{}行失败: {}", rowNum, e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("CSV导入失败: {}", e.getMessage(), e);
            throw new RuntimeException("CSV导入失败: " + e.getMessage(), e);
        }
        
        log.info("CSV导入完成, 成功: {}, 失败: {}", successCount, failCount);
        
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("errors", errors);
        return result;
    }
    
    /**
     * 从JSON导入表元数据
     * 
     * @param file JSON文件
     * @param userId 用户ID
     * @return 导入结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importFromJson(MultipartFile file, Long userId) {
        log.info("开始从JSON导入表元数据, 文件名: {}", file.getOriginalFilename());
        
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();
        
        try {
            // 解析JSON
            List<Map<String, Object>> tables = objectMapper.readValue(
                file.getInputStream(), 
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            for (int i = 0; i < tables.size(); i++) {
                try {
                    Map<String, Object> tableData = tables.get(i);
                    TableMetadata table = parseJsonObject(tableData, userId);
                    
                    // 检查是否已存在
                    QueryWrapper<TableMetadata> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("database_name", table.getDatabaseName())
                               .eq("table_name", table.getTableName());
                    
                    if (tableRepository.selectCount(queryWrapper) > 0) {
                        errors.add(String.format("第%d条: 表 %s.%s 已存在", 
                            i + 1, table.getDatabaseName(), table.getTableName()));
                        failCount++;
                        continue;
                    }
                    
                    // 插入表
                    tableRepository.insert(table);
                    successCount++;
                    
                } catch (Exception e) {
                    errors.add(String.format("第%d条: %s", i + 1, e.getMessage()));
                    failCount++;
                    log.error("导入第{}条失败: {}", i + 1, e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("JSON导入失败: {}", e.getMessage(), e);
            throw new RuntimeException("JSON导入失败: " + e.getMessage(), e);
        }
        
        log.info("JSON导入完成, 成功: {}, 失败: {}", successCount, failCount);
        
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("errors", errors);
        return result;
    }
    
    /**
     * 创建导出任务
     * 
     * @param taskType 任务类型
     * @param filters 过滤条件
     * @param userId 用户ID
     * @return 任务ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createExportTask(ExportType taskType, Map<String, Object> filters, Long userId) {
        log.info("创建导出任务, 类型: {}", taskType);
        
        ExportTask task = new ExportTask();
        task.setTaskType(taskType);
        try {
            task.setFilters(objectMapper.writeValueAsString(filters));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("序列化过滤条件失败", e);
        }
        task.setStatus(TaskStatus.PENDING);
        task.setCreatedBy(userId);
        
        exportTaskRepository.insert(task);
        
        log.info("导出任务创建成功, ID: {}", task.getId());
        return task.getId();
    }
    
    /**
     * 异步导出到CSV
     * 
     * @param taskId 任务ID
     * @return 文件路径
     */
    @Async
    public CompletableFuture<String> exportToCsv(Long taskId) {
        log.info("开始异步导出CSV, 任务ID: {}", taskId);
        
        ExportTask task = exportTaskRepository.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("导出任务不存在, ID: " + taskId);
        }
        
        try {
            // 更新任务状态
            task.setStatus(TaskStatus.RUNNING);
            task.setStartedAt(LocalDateTime.now());
            exportTaskRepository.updateById(task);
            
            // 解析过滤条件
            Map<String, Object> filters = objectMapper.readValue(
                task.getFilters(), 
                new TypeReference<Map<String, Object>>() {}
            );
            
            // 查询数据
            QueryWrapper<TableMetadata> queryWrapper = buildQueryWrapper(filters);
            List<TableMetadata> tables = tableRepository.selectList(queryWrapper);
            
            // 生成文件名
            String fileName = String.format("tables_export_%s.csv", 
                LocalDateTime.now().format(FORMATTER));
            String filePath = EXPORT_DIR + fileName;
            
            // 确保目录存在
            new File(EXPORT_DIR).mkdirs();
            
            // 写入CSV
            try (CSVWriter writer = new CSVWriter(
                    new FileWriter(filePath, StandardCharsets.UTF_8))) {
                
                // 写入表头
                String[] headers = {"database_name", "table_name", "table_type", 
                    "description", "storage_format", "storage_location", 
                    "data_size_bytes", "created_at", "updated_at"};
                writer.writeNext(headers);
                
                // 写入数据
                for (TableMetadata table : tables) {
                    String[] row = {
                        table.getDatabaseName(),
                        table.getTableName(),
                        table.getTableType().name(),
                        table.getDescription(),
                        table.getStorageFormat(),
                        table.getStorageLocation(),
                        String.valueOf(table.getDataSizeBytes()),
                        table.getCreatedAt() != null ? table.getCreatedAt().toString() : "",
                        table.getUpdatedAt() != null ? table.getUpdatedAt().toString() : ""
                    };
                    writer.writeNext(row);
                }
            }
            
            // 更新任务状态
            task.setStatus(TaskStatus.COMPLETED);
            task.setFilePath(filePath);
            task.setRecordCount(tables.size());
            task.setCompletedAt(LocalDateTime.now());
            exportTaskRepository.updateById(task);
            
            log.info("CSV导出完成, 任务ID: {}, 记录数: {}", taskId, tables.size());
            return CompletableFuture.completedFuture(filePath);
            
        } catch (Exception e) {
            log.error("CSV导出失败, 任务ID: {}", taskId, e);
            
            // 更新任务状态为失败
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            task.setCompletedAt(LocalDateTime.now());
            exportTaskRepository.updateById(task);
            
            throw new RuntimeException("CSV导出失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 异步导出到JSON
     * 
     * @param taskId 任务ID
     * @return 文件路径
     */
    @Async
    public CompletableFuture<String> exportToJson(Long taskId) {
        log.info("开始异步导出JSON, 任务ID: {}", taskId);
        
        ExportTask task = exportTaskRepository.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("导出任务不存在, ID: " + taskId);
        }
        
        try {
            // 更新任务状态
            task.setStatus(TaskStatus.RUNNING);
            task.setStartedAt(LocalDateTime.now());
            exportTaskRepository.updateById(task);
            
            // 解析过滤条件
            Map<String, Object> filters = objectMapper.readValue(
                task.getFilters(), 
                new TypeReference<Map<String, Object>>() {}
            );
            
            // 查询数据
            QueryWrapper<TableMetadata> queryWrapper = buildQueryWrapper(filters);
            List<TableMetadata> tables = tableRepository.selectList(queryWrapper);
            
            // 生成文件名
            String fileName = String.format("tables_export_%s.json", 
                LocalDateTime.now().format(FORMATTER));
            String filePath = EXPORT_DIR + fileName;
            
            // 确保目录存在
            new File(EXPORT_DIR).mkdirs();
            
            // 写入JSON
            objectMapper.writerWithDefaultPrettyPrinter()
                       .writeValue(new File(filePath), tables);
            
            // 更新任务状态
            task.setStatus(TaskStatus.COMPLETED);
            task.setFilePath(filePath);
            task.setRecordCount(tables.size());
            task.setCompletedAt(LocalDateTime.now());
            exportTaskRepository.updateById(task);
            
            log.info("JSON导出完成, 任务ID: {}, 记录数: {}", taskId, tables.size());
            return CompletableFuture.completedFuture(filePath);
            
        } catch (Exception e) {
            log.error("JSON导出失败, 任务ID: {}", taskId, e);
            
            // 更新任务状态为失败
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            task.setCompletedAt(LocalDateTime.now());
            exportTaskRepository.updateById(task);
            
            throw new RuntimeException("JSON导出失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取导出任务状态
     * 
     * @param taskId 任务ID
     * @return 任务信息
     */
    public ExportTask getExportStatus(Long taskId) {
        log.debug("查询导出任务状态, ID: {}", taskId);
        
        ExportTask task = exportTaskRepository.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("导出任务不存在, ID: " + taskId);
        }
        
        return task;
    }
    
    /**
     * 下载导出文件
     * 
     * @param taskId 任务ID
     * @return 文件输入流
     */
    public InputStream downloadExportFile(Long taskId) throws IOException {
        log.info("下载导出文件, 任务ID: {}", taskId);
        
        ExportTask task = exportTaskRepository.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("导出任务不存在, ID: " + taskId);
        }
        
        if (task.getStatus() != TaskStatus.COMPLETED) {
            throw new IllegalStateException("导出任务未完成, 状态: " + task.getStatus());
        }
        
        File file = new File(task.getFilePath());
        if (!file.exists()) {
            throw new FileNotFoundException("导出文件不存在: " + task.getFilePath());
        }
        
        return new FileInputStream(file);
    }
    
    /**
     * 解析CSV行数据
     */
    private TableMetadata parseCsvLine(String[] headers, String[] values, Long userId) {
        Map<String, String> data = new HashMap<>();
        for (int i = 0; i < headers.length && i < values.length; i++) {
            data.put(headers[i], values[i]);
        }
        
        TableMetadata table = new TableMetadata();
        table.setDatabaseName(data.get("database_name"));
        table.setTableName(data.get("table_name"));
        table.setTableType(TableType.valueOf(data.get("table_type")));
        table.setDescription(data.get("description"));
        table.setStorageFormat(data.get("storage_format"));
        table.setStorageLocation(data.get("storage_location"));
        
        if (data.containsKey("data_size_bytes") && !data.get("data_size_bytes").isEmpty()) {
            table.setDataSizeBytes(Long.parseLong(data.get("data_size_bytes")));
        }
        
        table.setOwnerId(userId);
        
        return table;
    }
    
    /**
     * 解析JSON对象
     */
    private TableMetadata parseJsonObject(Map<String, Object> data, Long userId) {
        TableMetadata table = new TableMetadata();
        table.setDatabaseName((String) data.get("databaseName"));
        table.setTableName((String) data.get("tableName"));
        table.setTableType(TableType.valueOf((String) data.get("tableType")));
        table.setDescription((String) data.get("description"));
        table.setStorageFormat((String) data.get("storageFormat"));
        table.setStorageLocation((String) data.get("storageLocation"));
        
        if (data.containsKey("dataSizeBytes")) {
            table.setDataSizeBytes(((Number) data.get("dataSizeBytes")).longValue());
        }
        
        table.setOwnerId(userId);
        
        return table;
    }
    
    /**
     * 构建查询条件
     */
    private QueryWrapper<TableMetadata> buildQueryWrapper(Map<String, Object> filters) {
        QueryWrapper<TableMetadata> queryWrapper = new QueryWrapper<>();
        
        if (filters != null) {
            if (filters.containsKey("databaseName")) {
                queryWrapper.eq("database_name", filters.get("databaseName"));
            }
            if (filters.containsKey("tableType")) {
                queryWrapper.eq("table_type", filters.get("tableType"));
            }
            if (filters.containsKey("ownerId")) {
                queryWrapper.eq("owner_id", filters.get("ownerId"));
            }
        }
        
        return queryWrapper;
    }
}
