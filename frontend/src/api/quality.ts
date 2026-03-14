// 数据质量相关 API
import apiClient from './client'
import type { QualityMetrics } from '@/types'

/** 获取表的质量指标 */
export const getQualityMetrics = (tableId: number) =>
  apiClient.get<QualityMetrics>(`/quality/${tableId}`)

/** 获取质量趋势 */
export const getQualityTrend = (tableId: number, days: number = 30) =>
  apiClient.get<QualityMetrics[]>(`/quality/${tableId}/trend`, { params: { days } })

/** 记录质量指标 */
export const recordQualityMetrics = (data: Omit<QualityMetrics, 'id' | 'measuredAt'>) =>
  apiClient.post<QualityMetrics>('/quality', data)
