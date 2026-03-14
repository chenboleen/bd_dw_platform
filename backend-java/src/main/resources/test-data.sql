-- 测试数据初始化脚本
-- 使用前请确保已执行 schema.sql

USE metadata_db;

-- ==========================================
-- 1. 插入测试用户
-- ==========================================
INSERT INTO users (username, email, password, role, is_active) VALUES
('developer', 'dev@kiro.com', 'dev123', 'DEVELOPER', TRUE),
('guest', 'guest@kiro.com', 'guest123', 'GUEST', TRUE)
ON DUPLICATE KEY UPDATE username=username;

-- ==========================================
-- 2. 插入测试表元数据
-- ==========================================
INSERT INTO tables (database_name, table_name, table_type, description, storage_format, storage_location, data_size_bytes, owner_id) VALUES
-- 用户域
('dw_ods', 'ods_user_info', 'TABLE', '用户基础信息表', 'PARQUET', 'hdfs://namenode/warehouse/dw_ods/ods_user_info', 104857600, 1),
('dw_ods', 'ods_user_login', 'TABLE', '用户登录记录表', 'ORC', 'hdfs://namenode/warehouse/dw_ods/ods_user_login', 52428800, 1),
('dw_dwd', 'dwd_user_info', 'TABLE', '用户明细宽表', 'PARQUET', 'hdfs://namenode/warehouse/dw_dwd/dwd_user_info', 209715200, 1),
('dw_dws', 'dws_user_daily', 'TABLE', '用户每日汇总表', 'ORC', 'hdfs://namenode/warehouse/dw_dws/dws_user_daily', 10485760, 1),
('dw_ads', 'ads_user_profile', 'VIEW', '用户画像视图', NULL, NULL, 0, 1),
-- 交易域
('dw_ods', 'ods_order_info', 'TABLE', '订单基础信息表', 'PARQUET', 'hdfs://namenode/warehouse/dw_ods/ods_order_info', 524288000, 1),
('dw_ods', 'ods_order_detail', 'TABLE', '订单明细表', 'ORC', 'hdfs://namenode/warehouse/dw_ods/ods_order_detail', 1073741824, 1),
('dw_dwd', 'dwd_order_wide', 'TABLE', '订单明细宽表', 'PARQUET', 'hdfs://namenode/warehouse/dw_dwd/dwd_order_wide', 2147483648, 1),
('dw_dws', 'dws_sales_daily', 'TABLE', '销售日报表', 'ORC', 'hdfs://namenode/warehouse/dw_dws/dws_sales_daily', 52428800, 1),
('dw_ads', 'ads_sales_overview', 'VIEW', '销售概览视图', NULL, NULL, 0, 1),
-- 商品域
('dw_ods', 'ods_product_info', 'TABLE', '商品信息表', 'PARQUET', 'hdfs://namenode/warehouse/dw_ods/ods_product_info', 20971520, 1),
('dw_ods', 'ods_product_category', 'EXTERNAL', '商品分类外部表', 'TEXTFILE', 'hdfs://namenode/warehouse/dw_ods/ods_product_category', 1048576, 1),
('dw_dwd', 'dwd_product_wide', 'TABLE', '商品宽表', 'PARQUET', 'hdfs://namenode/warehouse/dw_dwd/dwd_product_wide', 41943040, 1)
ON DUPLICATE KEY UPDATE table_name=table_name;

-- ==========================================
-- 3. 插入测试字段元数据
-- ==========================================
-- ods_user_info 表字段
INSERT INTO columns (table_id, column_name, data_type, column_order, is_nullable, is_partition_key, description) VALUES
(1, 'user_id', 'BIGINT', 1, FALSE, TRUE, '用户ID'),
(1, 'username', 'VARCHAR(100)', 2, FALSE, FALSE, '用户名'),
(1, 'email', 'VARCHAR(100)', 3, TRUE, FALSE, '邮箱'),
(1, 'phone', 'VARCHAR(20)', 4, TRUE, FALSE, '手机号'),
(1, 'gender', 'TINYINT', 5, TRUE, FALSE, '性别 0-未知 1-男 2-女'),
(1, 'birthday', 'DATE', 6, TRUE, FALSE, '生日'),
(1, 'register_time', 'DATETIME', 7, FALSE, FALSE, '注册时间'),
(1, 'last_login_time', 'DATETIME', 8, TRUE, FALSE, '最后登录时间'),
(1, 'status', 'TINYINT', 9, FALSE, FALSE, '状态 0-禁用 1-启用');

-- ods_order_info 表字段
INSERT INTO columns (table_id, column_name, data_type, column_order, is_nullable, is_partition_key, description) VALUES
(6, 'order_id', 'BIGINT', 1, FALSE, TRUE, '订单ID'),
(6, 'user_id', 'BIGINT', 2, FALSE, FALSE, '用户ID'),
(6, 'order_no', 'VARCHAR(50)', 3, FALSE, FALSE, '订单号'),
(6, 'total_amount', 'DECIMAL(12,2)', 4, FALSE, FALSE, '订单总金额'),
(6, 'pay_amount', 'DECIMAL(12,2)', 5, FALSE, FALSE, '实付金额'),
(6, 'order_status', 'TINYINT', 6, FALSE, FALSE, '订单状态'),
(6, 'create_time', 'DATETIME', 7, FALSE, FALSE, '创建时间'),
(6, 'pay_time', 'DATETIME', 8, TRUE, FALSE, '支付时间');

-- ods_product_info 表字段
INSERT INTO columns (table_id, column_name, data_type, column_order, is_nullable, is_partition_key, description) VALUES
(11, 'product_id', 'BIGINT', 1, FALSE, TRUE, '商品ID'),
(11, 'product_name', 'VARCHAR(200)', 2, FALSE, FALSE, '商品名称'),
(11, 'category_id', 'BIGINT', 3, FALSE, FALSE, '分类ID'),
(11, 'price', 'DECIMAL(10,2)', 4, FALSE, FALSE, '价格'),
(11, 'stock', 'INT', 5, FALSE, FALSE, '库存'),
(11, 'status', 'TINYINT', 6, FALSE, FALSE, '状态'),
(11, 'create_time', 'DATETIME', 7, FALSE, FALSE, '创建时间');

-- ==========================================
-- 4. 插入血缘关系
-- ==========================================
INSERT INTO lineage (source_table_id, target_table_id, lineage_type, transformation_logic, created_by) VALUES
(1, 3, 'DIRECT', '用户基础信息清洗和标准化', 1),
(3, 4, 'DIRECT', '按用户维度每日汇总统计', 1),
(4, 5, 'INDIRECT', '用户画像聚合视图', 1),
(6, 8, 'DIRECT', '订单与订单明细关联宽表', 1),
(8, 9, 'DIRECT', '销售数据每日汇总', 1),
(1, 8, 'DIRECT', '用户信息关联订单宽表', 1),
(11, 8, 'DIRECT', '商品信息关联订单宽表', 1),
(11, 13, 'DIRECT', '商品信息维度扩展', 1)
ON DUPLICATE KEY UPDATE source_table_id=source_table_id;

-- ==========================================
-- 5. 插入数据目录
-- ==========================================
INSERT INTO catalog (name, description, parent_id, level, path, created_by) VALUES
('数据仓库', '数据仓库整体目录', NULL, 1, '/数据仓库', 1),
('ODS层', '原始数据层', 1, 2, '/数据仓库/ODS层', 1),
('DWD层', '明细数据层', 1, 2, '/数据仓库/DWD层', 1),
('DWS层', '汇总数据层', 1, 2, '/数据仓库/DWS层', 1),
('ADS层', '应用数据层', 1, 2, '/数据仓库/ADS层', 1),
('用户域', '用户相关数据', 2, 3, '/数据仓库/ODS层/用户域', 1),
('交易域', '交易相关数据', 2, 3, '/数据仓库/ODS层/交易域', 1),
('商品域', '商品相关数据', 2, 3, '/数据仓库/ODS层/商品域', 1)
ON DUPLICATE KEY UPDATE path=path;

-- ==========================================
-- 6. 插入表与目录关联
-- ==========================================
INSERT INTO table_catalog (table_id, catalog_id) VALUES
(1, 6), (2, 6), (3, 3), (4, 4), (5, 5),
(6, 7), (7, 7), (8, 3), (9, 4), (10, 5),
(11, 8), (12, 8), (13, 3)
ON DUPLICATE KEY UPDATE table_id=table_id;

-- ==========================================
-- 7. 插入数据质量指标
-- ==========================================
INSERT INTO quality_metrics (table_id, record_count, null_rate, update_frequency, data_freshness_hours, measured_at) VALUES
(1, 1000000, 0.0012, 'DAILY', 24, NOW()),
(2, 5000000, 0.0025, 'HOURLY', 1, NOW()),
(3, 980000, 0.0008, 'DAILY', 24, NOW()),
(4, 365, 0.0000, 'DAILY', 24, NOW()),
(6, 2000000, 0.0015, 'HOURLY', 1, NOW()),
(7, 8000000, 0.0005, 'HOURLY', 1, NOW()),
(8, 7500000, 0.0003, 'DAILY', 24, NOW()),
(9, 730, 0.0000, 'DAILY', 24, NOW()),
(11, 50000, 0.0030, 'DAILY', 24, NOW()),
(13, 48000, 0.0010, 'DAILY', 24, NOW());

-- ==========================================
-- 8. 插入变更历史记录
-- ==========================================
INSERT INTO change_history (entity_type, entity_id, operation, field_name, old_value, new_value, changed_at, changed_by) VALUES
('TABLE', 1, 'CREATE', NULL, NULL, '{"databaseName":"dw_ods","tableName":"ods_user_info","tableType":"TABLE"}', DATE_SUB(NOW(), INTERVAL 30 DAY), 1),
('TABLE', 6, 'CREATE', NULL, NULL, '{"databaseName":"dw_ods","tableName":"ods_order_info","tableType":"TABLE"}', DATE_SUB(NOW(), INTERVAL 25 DAY), 1),
('TABLE', 11, 'CREATE', NULL, NULL, '{"databaseName":"dw_ods","tableName":"ods_product_info","tableType":"TABLE"}', DATE_SUB(NOW(), INTERVAL 20 DAY), 1),
('TABLE', 3, 'CREATE', NULL, NULL, '{"databaseName":"dw_dwd","tableName":"dwd_user_info","tableType":"TABLE"}', DATE_SUB(NOW(), INTERVAL 15 DAY), 1),
('TABLE', 8, 'CREATE', NULL, NULL, '{"databaseName":"dw_dwd","tableName":"dwd_order_wide","tableType":"TABLE"}', DATE_SUB(NOW(), INTERVAL 10 DAY), 1),
('TABLE', 1, 'UPDATE', 'description', '用户基础信息', '用户基础信息表', DATE_SUB(NOW(), INTERVAL 5 DAY), 1),
('COLUMN', 1, 'UPDATE', 'description', '用户ID', '用户唯一标识ID', DATE_SUB(NOW(), INTERVAL 3 DAY), 1),
('TABLE', 4, 'CREATE', NULL, NULL, '{"databaseName":"dw_dws","tableName":"dws_user_daily","tableType":"TABLE"}', DATE_SUB(NOW(), INTERVAL 2 DAY), 1),
('LINEAGE', 1, 'CREATE', NULL, NULL, '{"sourceTableId":1,"targetTableId":3,"lineageType":"DIRECT"}', DATE_SUB(NOW(), INTERVAL 1 DAY), 1),
('CATALOG', 6, 'CREATE', NULL, NULL, '{"name":"用户域","level":3}', NOW(), 1);

-- ==========================================
-- 测试数据初始化完成
-- ==========================================
SELECT '测试数据初始化完成！' AS message;
SELECT 
  (SELECT COUNT(*) FROM users) AS user_count,
  (SELECT COUNT(*) FROM tables) AS table_count,
  (SELECT COUNT(*) FROM columns) AS column_count,
  (SELECT COUNT(*) FROM lineage) AS lineage_count,
  (SELECT COUNT(*) FROM catalog) AS catalog_count,
  (SELECT COUNT(*) FROM table_catalog) AS table_catalog_count,
  (SELECT COUNT(*) FROM quality_metrics) AS quality_metrics_count,
  (SELECT COUNT(*) FROM change_history) AS change_history_count;
