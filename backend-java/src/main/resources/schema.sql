-- 数据仓库元数据管理系统 - 数据库初始化脚本
-- 字符集: utf8mb4, 排序规则: utf8mb4_unicode_ci

-- 创建数据库
CREATE DATABASE IF NOT EXISTS metadata_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE metadata_db;

-- 1. 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    password VARCHAR(255) NOT NULL COMMENT '密码(明文)',
    role VARCHAR(20) NOT NULL COMMENT '角色: ADMIN/DEVELOPER/GUEST',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否激活',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_login_at DATETIME COMMENT '最后登录时间',
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 2. 表元数据
CREATE TABLE IF NOT EXISTS tables (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '表ID',
    database_name VARCHAR(100) NOT NULL COMMENT '数据库名',
    table_name VARCHAR(100) NOT NULL COMMENT '表名',
    table_type VARCHAR(20) NOT NULL COMMENT '表类型: TABLE/VIEW/EXTERNAL',
    description VARCHAR(1000) COMMENT '表描述',
    storage_format VARCHAR(50) COMMENT '存储格式: PARQUET/ORC/CSV',
    storage_location VARCHAR(500) COMMENT '存储位置',
    data_size_bytes BIGINT COMMENT '数据大小(字节)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_accessed_at DATETIME COMMENT '最后访问时间',
    owner_id BIGINT NOT NULL COMMENT '所有者ID',
    UNIQUE KEY uk_database_table (database_name, table_name),
    INDEX idx_updated_at (updated_at),
    INDEX idx_database_name (database_name),
    INDEX idx_table_name (table_name),
    INDEX idx_owner (owner_id),
    FOREIGN KEY (owner_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表元数据';

-- 3. 字段元数据
CREATE TABLE IF NOT EXISTS columns (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '字段ID',
    table_id BIGINT NOT NULL COMMENT '表ID',
    column_name VARCHAR(100) NOT NULL COMMENT '字段名',
    data_type VARCHAR(50) NOT NULL COMMENT '数据类型',
    column_order INT NOT NULL COMMENT '字段顺序',
    is_nullable BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否可为空',
    is_partition_key BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否分区键',
    description VARCHAR(1000) COMMENT '字段描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_table_order (table_id, column_order),
    INDEX idx_table_name (table_id, column_name),
    FOREIGN KEY (table_id) REFERENCES tables(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字段元数据';

-- 4. 血缘关系
CREATE TABLE IF NOT EXISTS lineage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '血缘ID',
    source_table_id BIGINT NOT NULL COMMENT '上游表ID',
    target_table_id BIGINT NOT NULL COMMENT '下游表ID',
    lineage_type VARCHAR(20) NOT NULL COMMENT '血缘类型: DIRECT/INDIRECT',
    transformation_logic TEXT COMMENT '转换逻辑',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT NOT NULL COMMENT '创建人ID',
    UNIQUE KEY uk_source_target (source_table_id, target_table_id),
    INDEX idx_source (source_table_id),
    INDEX idx_target (target_table_id),
    FOREIGN KEY (source_table_id) REFERENCES tables(id) ON DELETE CASCADE,
    FOREIGN KEY (target_table_id) REFERENCES tables(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='血缘关系';

-- 5. 数据目录
CREATE TABLE IF NOT EXISTS catalog (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '目录ID',
    name VARCHAR(100) NOT NULL COMMENT '目录名称',
    description VARCHAR(1000) COMMENT '目录描述',
    parent_id BIGINT COMMENT '父目录ID',
    level INT NOT NULL COMMENT '层级(1-5)',
    path VARCHAR(500) NOT NULL COMMENT '路径',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT NOT NULL COMMENT '创建人ID',
    INDEX idx_parent (parent_id),
    INDEX idx_path (path),
    INDEX idx_level (level),
    FOREIGN KEY (parent_id) REFERENCES catalog(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id),
    CHECK (level >= 1 AND level <= 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据目录';

-- 6. 表与目录关联表
CREATE TABLE IF NOT EXISTS table_catalog (
    table_id BIGINT NOT NULL COMMENT '表ID',
    catalog_id BIGINT NOT NULL COMMENT '目录ID',
    PRIMARY KEY (table_id, catalog_id),
    FOREIGN KEY (table_id) REFERENCES tables(id) ON DELETE CASCADE,
    FOREIGN KEY (catalog_id) REFERENCES catalog(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表与目录关联';

-- 7. 数据质量指标
CREATE TABLE IF NOT EXISTS quality_metrics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '指标ID',
    table_id BIGINT NOT NULL COMMENT '表ID',
    record_count BIGINT COMMENT '记录数',
    null_rate DECIMAL(5,4) COMMENT '空值率(0-1)',
    update_frequency VARCHAR(20) COMMENT '更新频率: DAILY/WEEKLY/MONTHLY',
    data_freshness_hours INT COMMENT '数据新鲜度(小时)',
    measured_at DATETIME NOT NULL COMMENT '测量时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_table_measured (table_id, measured_at),
    FOREIGN KEY (table_id) REFERENCES tables(id) ON DELETE CASCADE,
    CHECK (null_rate >= 0 AND null_rate <= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据质量指标';

-- 8. 变更历史
CREATE TABLE IF NOT EXISTS change_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '历史ID',
    entity_type VARCHAR(50) NOT NULL COMMENT '实体类型: TABLE/COLUMN/CATALOG',
    entity_id BIGINT NOT NULL COMMENT '实体ID',
    operation VARCHAR(20) NOT NULL COMMENT '操作类型: CREATE/UPDATE/DELETE',
    field_name VARCHAR(100) COMMENT '字段名',
    old_value TEXT COMMENT '旧值(JSON)',
    new_value TEXT COMMENT '新值(JSON)',
    changed_at DATETIME NOT NULL COMMENT '变更时间',
    changed_by BIGINT NOT NULL COMMENT '变更人ID',
    INDEX idx_entity_changed (entity_type, entity_id, changed_at),
    INDEX idx_changed_by (changed_by),
    FOREIGN KEY (changed_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='变更历史';

-- 9. 导出任务
CREATE TABLE IF NOT EXISTS export_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    task_type VARCHAR(20) NOT NULL COMMENT '任务类型: CSV/JSON',
    filters JSON COMMENT '过滤条件(JSON)',
    status VARCHAR(20) NOT NULL COMMENT '状态: PENDING/RUNNING/COMPLETED/FAILED',
    file_path VARCHAR(500) COMMENT '文件路径',
    record_count INT COMMENT '记录数',
    error_message TEXT COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by BIGINT NOT NULL COMMENT '创建人ID',
    started_at DATETIME COMMENT '开始时间',
    completed_at DATETIME COMMENT '完成时间',
    INDEX idx_created (created_by, created_at),
    INDEX idx_status (status),
    FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导出任务';

-- 插入默认管理员用户 (密码: admin123, 明文存储)
INSERT INTO users (username, email, password, role, is_active) 
VALUES (
    'admin',
    'admin@kiro.com',
    'admin123',
    'ADMIN',
    TRUE
) ON DUPLICATE KEY UPDATE username=username;
