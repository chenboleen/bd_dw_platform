// 数据仓库元数据管理系统 - TypeScript 类型定义

// ==================== 通用类型 ====================

/** 分页响应 */
export interface PagedResponse<T> {
  data: T[]
  total: number
  page: number
  size: number
  totalPages: number
}

/** 通用 API 响应 */
export interface ApiResponse<T = void> {
  code: number
  message: string
  data: T
  timestamp: string
  requestId?: string
}

/** 错误响应 */
export interface ErrorResponse {
  errorCode: string
  errorMessage: string
  details?: string
  timestamp: string
  requestId?: string
}

// ==================== 用户认证 ====================

/** 用户角色 */
export type UserRole = 'ADMIN' | 'DEVELOPER' | 'GUEST'

/** 用户信息 */
export interface User {
  id: number
  username: string
  email: string
  role: UserRole
  isActive: boolean
  createdAt: string
  updatedAt?: string
}

/** 登录请求 */
export interface LoginRequest {
  username: string
  password: string
}

/** Token 响应 */
export interface TokenResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

/** 刷新 Token 请求 */
export interface RefreshTokenRequest {
  refreshToken: string
}

// ==================== 表元数据 ====================

/** 表类型 */
export type TableType = 'TABLE' | 'VIEW' | 'EXTERNAL'

/** 表元数据 */
export interface TableMetadata {
  id: number
  databaseName: string
  tableName: string
  tableType: TableType
  description?: string
  storageFormat?: string
  storageLocation?: string
  dataSizeBytes?: number
  ownerId?: number
  ownerName?: string
  createdAt: string
  updatedAt: string
  columns?: ColumnMetadata[]
}

/** 创建表请求 */
export interface TableCreateRequest {
  databaseName: string
  tableName: string
  tableType: TableType
  description?: string
  storageFormat?: string
  storageLocation?: string
  dataSizeBytes?: number
}

/** 更新表请求 */
export interface TableUpdateRequest {
  description?: string
  storageFormat?: string
  storageLocation?: string
  dataSizeBytes?: number
}

// ==================== 字段元数据 ====================

/** 字段元数据 */
export interface ColumnMetadata {
  id: number
  tableId: number
  columnName: string
  dataType: string
  columnOrder: number
  isNullable: boolean
  isPartitionKey: boolean
  description?: string
  createdAt: string
  updatedAt: string
}

/** 创建字段请求 */
export interface ColumnCreateRequest {
  tableId: number
  columnName: string
  dataType: string
  columnOrder: number
  isNullable: boolean
  isPartitionKey: boolean
  description?: string
}

/** 更新字段请求 */
export interface ColumnUpdateRequest {
  columnName?: string
  dataType?: string
  isNullable?: boolean
  isPartitionKey?: boolean
  description?: string
}

/** 字段排序请求 */
export interface ReorderColumnsRequest {
  tableId: number
  columnOrders: { columnId: number; order: number }[]
}

// ==================== 血缘关系 ====================

/** 血缘类型 */
export type LineageType = 'DIRECT' | 'INDIRECT'

/** 血缘关系 */
export interface Lineage {
  id: number
  sourceTableId: number
  targetTableId: number
  lineageType: LineageType
  transformationLogic?: string
  createdAt: string
}

/** 创建血缘请求 */
export interface LineageCreateRequest {
  sourceTableId: number
  targetTableId: number
  lineageType: LineageType
  transformationLogic?: string
}

/** 血缘图节点 */
export interface LineageNode {
  id: number
  name: string
  depth: number
  type?: string
  databaseName?: string
  tableName?: string
  tableType?: string
}

/** 血缘图边 */
export interface LineageEdge {
  source: number
  target: number
  type: string
}

/** 血缘图 */
export interface LineageGraph {
  nodes: LineageNode[]
  edges: LineageEdge[]
}

/** 影响分析报告 */
export interface ImpactReport {
  tableId: number
  tableName: string
  affectedTables: TableMetadata[]
  maxDepth: number
  totalCount: number
}

// ==================== 数据目录 ====================

/** 数据目录节点 */
export interface Catalog {
  id: number
  name: string
  description?: string
  parentId?: number
  level: number
  path: string
  children?: Catalog[]
  tableCount?: number
  createdAt: string
  updatedAt: string
}

/** 创建目录请求 */
export interface CatalogCreateRequest {
  name: string
  description?: string
  parentId?: number
}

// ==================== 数据质量 ====================

/** 数据质量指标 */
export interface QualityMetrics {
  id: number
  tableId: number
  tableName?: string
  recordCount: number
  nullRate: number
  updateFrequency?: string
  dataFreshnessHours?: number
  qualityScore?: number
  measuredAt: string
}

// ==================== 变更历史 ====================

/** 操作类型 */
export type OperationType = 'CREATE' | 'UPDATE' | 'DELETE'

/** 变更历史 */
export interface ChangeHistory {
  id: number
  entityType: string
  entityId: number
  operation: OperationType
  fieldName?: string
  oldValue?: string
  newValue?: string
  changedAt: string
  changedBy?: number
  changedByName?: string
}

// ==================== 搜索 ====================

/** 搜索请求 */
export interface SearchRequest {
  keyword: string
  databaseName?: string
  tableType?: TableType
  startDate?: string
  endDate?: string
  page?: number
  pageSize?: number
}

/** 搜索结果项 */
export interface SearchResultItem {
  id: number
  databaseName: string
  tableName: string
  tableType: TableType
  description?: string
  highlight?: {
    tableName?: string[]
    description?: string[]
  }
  score?: number
  updatedAt: string
}

/** 搜索响应 */
export interface SearchResponse {
  items: SearchResultItem[]
  total: number
  page: number
  pageSize: number
  totalPages: number
  took?: number
}

// ==================== 导入导出 ====================

/** 导出格式 */
export type ExportFormat = 'CSV' | 'JSON'

/** 任务状态 */
export type TaskStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED'

/** 导出请求 */
export interface ExportRequest {
  format: ExportFormat
  databaseName?: string
  tableType?: TableType
  startDate?: string
  endDate?: string
}

/** 导出状态响应 */
export interface ExportStatusResponse {
  taskId: number
  status: TaskStatus
  format: ExportFormat
  recordCount?: number
  filePath?: string
  errorMessage?: string
  createdAt: string
  completedAt?: string
}

/** 导入结果 */
export interface ImportResult {
  successCount: number
  failureCount: number
  errors: { row: number; message: string }[]
}

// ==================== 过滤条件 ====================

/** 表格过滤条件 */
export interface TableFilter {
  databaseName?: string
  tableType?: TableType
  ownerId?: number
  tableName?: string
  startDate?: string
  endDate?: string
}

/** 分页参数 */
export interface PaginationParams {
  page: number
  pageSize: number
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}
