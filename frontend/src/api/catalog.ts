// 数据目录相关 API
import apiClient from './client'
import type { Catalog, CatalogCreateRequest, TableMetadata } from '@/types'

/** 获取目录树 */
export const getCatalogTree = () =>
  apiClient.get<Catalog[]>('/catalogs/tree')

/** 创建目录 */
export const createCatalog = (data: CatalogCreateRequest) =>
  apiClient.post<Catalog>('/catalogs', data)

/** 更新目录 */
export const updateCatalog = (id: number, data: Partial<CatalogCreateRequest>) =>
  apiClient.put<Catalog>(`/catalogs/${id}`, data)

/** 删除目录 */
export const deleteCatalog = (id: number) =>
  apiClient.delete(`/catalogs/${id}`)

/** 移动目录 */
export const moveCatalog = (id: number, newParentId: number | null) =>
  apiClient.put(`/catalogs/${id}/move`, { newParentId })

/** 获取目录下的表 */
export const getTablesInCatalog = (catalogId: number) =>
  apiClient.get<TableMetadata[]>(`/catalogs/${catalogId}/tables`)

/** 添加表到目录 */
export const addTableToCatalog = (catalogId: number, tableId: number) =>
  apiClient.post(`/catalogs/${catalogId}/tables/${tableId}`)

/** 从目录移除表 */
export const removeTableFromCatalog = (catalogId: number, tableId: number) =>
  apiClient.delete(`/catalogs/${catalogId}/tables/${tableId}`)
