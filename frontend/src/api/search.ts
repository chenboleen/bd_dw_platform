// 搜索相关 API
import apiClient from './client'
import type { SearchRequest, SearchResponse, TableMetadata, PagedResponse } from '@/types'

/** 全文搜索 */
export const searchTables = (params: SearchRequest) =>
  apiClient.get<SearchResponse>('/search', { params })

/** 搜索建议 */
export const getSuggestions = (prefix: string, limit: number = 10) =>
  apiClient.get<string[]>('/search/suggest', { params: { prefix, limit } })

/** 高级过滤 */
export const filterTables = (filters: Record<string, unknown>, page = 1, pageSize = 20) =>
  apiClient.post<PagedResponse<TableMetadata>>('/search/filter', { ...filters, page, pageSize })
