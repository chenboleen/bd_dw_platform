package com.kiro.metadata.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL解析服务
 * 从SQL语句中提取血缘关系信息
 * 
 * @author Kiro
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqlParserService {
    
    /**
     * 从SQL语句提取血缘关系
     * 解析SQL中的源表和目标表，返回血缘关系映射列表
     * 
     * @param sql SQL语句（支持 SELECT、INSERT INTO...SELECT、CREATE TABLE AS SELECT）
     * @return 血缘关系列表，每条记录包含 sourceTable 和 targetTable 字段
     */
    public List<Map<String, String>> extractLineageFromSql(String sql) {
        log.info("开始解析SQL提取血缘关系");
        
        List<Map<String, String>> lineages = new ArrayList<>();
        
        try {
            // 解析SQL语句
            Statement statement = CCJSqlParserUtil.parse(sql);
            
            // 根据语句类型提取血缘
            if (statement instanceof Select select) {
                // 纯SELECT语句，提取所有涉及的表（无明确目标表）
                List<String> sourceTables = extractTableNames(select);
                for (String sourceTable : sourceTables) {
                    Map<String, String> lineage = new HashMap<>();
                    lineage.put("sourceTable", sourceTable);
                    lineage.put("targetTable", "");
                    lineage.put("type", "SELECT");
                    lineages.add(lineage);
                }
                
            } else if (statement instanceof Insert insert) {
                // INSERT INTO ... SELECT
                lineages.addAll(extractFromInsert(insert));
                
            } else if (statement instanceof CreateTable createTable) {
                // CREATE TABLE AS SELECT
                lineages.addAll(extractFromCreateTable(createTable));
            }
            
            log.info("SQL解析完成, 提取到{}条血缘关系", lineages.size());
            
        } catch (JSQLParserException e) {
            log.error("SQL解析失败: {}", e.getMessage(), e);
            throw new IllegalArgumentException("SQL解析失败: " + e.getMessage(), e);
        }
        
        return lineages;
    }
    
    /**
     * 从INSERT语句提取血缘
     */
    private List<Map<String, String>> extractFromInsert(Insert insert) {
        List<Map<String, String>> lineages = new ArrayList<>();
        
        try {
            // 获取目标表
            String targetTable = insert.getTable().getName();
            
            // 获取SELECT部分的源表
            if (insert.getSelect() != null) {
                List<String> sourceTables = extractTableNames(insert.getSelect());
                for (String sourceTable : sourceTables) {
                    Map<String, String> lineage = new HashMap<>();
                    lineage.put("sourceTable", sourceTable);
                    lineage.put("targetTable", targetTable);
                    lineage.put("type", "INSERT");
                    lineages.add(lineage);
                }
            }
            
        } catch (Exception e) {
            log.error("提取INSERT血缘失败: {}", e.getMessage(), e);
        }
        
        return lineages;
    }
    
    /**
     * 从CREATE TABLE语句提取血缘
     */
    private List<Map<String, String>> extractFromCreateTable(CreateTable createTable) {
        List<Map<String, String>> lineages = new ArrayList<>();
        
        try {
            // 获取目标表
            String targetTable = createTable.getTable().getName();
            
            // 获取SELECT部分的源表
            if (createTable.getSelect() != null) {
                List<String> sourceTables = extractTableNames(createTable.getSelect());
                for (String sourceTable : sourceTables) {
                    Map<String, String> lineage = new HashMap<>();
                    lineage.put("sourceTable", sourceTable);
                    lineage.put("targetTable", targetTable);
                    lineage.put("type", "CREATE_TABLE_AS");
                    lineages.add(lineage);
                }
            }
            
        } catch (Exception e) {
            log.error("提取CREATE TABLE血缘失败: {}", e.getMessage(), e);
        }
        
        return lineages;
    }
    
    /**
     * 从SELECT语句中提取所有表名
     * jsqlparser 4.7: Select 实现了 Statement 接口，显式转换消除歧义
     */
    private List<String> extractTableNames(Select select) {
        try {
            TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
            Statement stmt = select;
            return tablesNamesFinder.getTableList(stmt);
        } catch (Exception e) {
            log.error("提取表名失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
