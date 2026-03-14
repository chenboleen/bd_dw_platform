// 表元数据相关 API
import apiClient from './client'
import type {
  TableMetadata, TableCreateRequest, TableUpdateRequest,
  ColumnMetadata, ColumnCreateRequest, ColumnUpdateRequest,
  ReorderColumnsRequest, PagedResponse, TableFilter, PaginationParams
} from '@/types'

/** 创建表 */
export const createTable = (data: TableCreateRequest) =>
  apiClient.post<TableMetadata>('/tables', data)

/** 获取表列表 */
export const listTables = (filter: TableFilter = {}, pagination: PaginationParams = { page: 1, pageSize: 20 }) =>
  apiClient.get<PagedResponse<TableMetadata>>('/tables', {
    params: { ...filter, ...pagination }
  })

/** 获取表详情 */
export const getTableById = (id: number) =>
  apiClient.get<TableMetadata>(`/tables/${id}`)

/** 更新表 */
export const updateTable = (id: number, data: TableUpdateRequest) =>
  apiClient.put<TableMetadata>(`/tables/${id}`, data)

/** 删除表 */
export const deleteTable = (id: number) =>
  apiClient.delete(`/tables/${id}`)

/** 获取表的字段列表 */
export const getTableColumns = (tableId: number) =>
  apiClient.get<ColumnMetadata[]>(`/tables/${tableId}/columns`)

/** 创建字段 */
export const createColumn = (data: ColumnCreateRequest) =>
  apiClient.post<ColumnMetadata>('/columns', data)

/** 更新字段 */
export const updateColumn = (id: number, data: ColumnUpdateRequest) =>
  apiClient.put<ColumnMetadata>(`/columns/${id}`, data)

/** 删除字段 */
export const deleteColumn = (id: number) =>
  apiClient.delete(`/columns/${id}`)

/** 字段排序 */
export const reorderColumns = (data: ReorderColumnsRequest) =>
  apiClient.put('/columns/reorder', data)

/** 获取扁平化数据域列表（用于下拉选择） */
export const getCatalogFlat = () =>
  apiClient.get('/catalogs/flat')

/** 将表关联到数据域 */
export const addTableToCatalog = (catalogId: number, tableId: number) =>
  apiClient.post(`/catalogs/${catalogId}/tables/${tableId}`)
