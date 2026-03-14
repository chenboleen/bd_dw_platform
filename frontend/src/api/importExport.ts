// 导入导出相关 API
import apiClient from './client'
import type { ExportRequest, ExportStatusResponse, ImportResult } from '@/types'

/** 创建导出任务 */
export const createExportTask = (data: ExportRequest) =>
  apiClient.post('/import-export/export', data)

/** 获取导出状态 */
export const getExportStatus = (taskId: number) =>
  apiClient.get(`/import-export/export/${taskId}/status`)

/** 下载导出文件 */
export const downloadExportFile = (taskId: number) =>
  apiClient.get(`/import-export/export/${taskId}/download`, { responseType: 'blob' })

/** 获取导出任务列表 */
export const listExportTasks = (page = 1, pageSize = 20) =>
  apiClient.get('/import-export/export/tasks', { params: { page, pageSize } })

/** 从 CSV 导入 */
export const importFromCsv = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return apiClient.post('/import-export/import/csv', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 从 JSON 导入 */
export const importFromJson = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return apiClient.post('/import-export/import/json', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
