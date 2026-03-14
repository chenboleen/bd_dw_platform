// 血缘关系相关 API
import apiClient from './client'
import type {
  Lineage, LineageCreateRequest, LineageGraph,
  ImpactReport, TableMetadata
} from '@/types'

/** 创建血缘关系 */
export const createLineage = (data: LineageCreateRequest) =>
  apiClient.post<Lineage>('/lineage', data)

/** 删除血缘关系 */
export const deleteLineage = (id: number) =>
  apiClient.delete(`/lineage/${id}`)

/** 获取上游表 */
export const getUpstreamTables = (tableId: number) =>
  apiClient.get<TableMetadata[]>(`/lineage/upstream/${tableId}`)

/** 获取下游表 */
export const getDownstreamTables = (tableId: number) =>
  apiClient.get<TableMetadata[]>(`/lineage/downstream/${tableId}`)

/** 获取血缘图谱 */
export const getLineageGraph = (tableId: number, direction: string = 'both', depth: number = 3) =>
  apiClient.get<LineageGraph>(`/lineage/graph/${tableId}`, {
    params: { direction, depth }
  })

/** 影响分析 */
export const analyzeImpact = (tableId: number) =>
  apiClient.post<ImpactReport>('/lineage/impact', { tableId })

/** SQL 解析血缘 */
export const parseSqlLineage = (sql: string) =>
  apiClient.post<Lineage[]>('/lineage/parse-sql', { sql })
