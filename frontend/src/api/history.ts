// 变更历史相关 API
import apiClient from './client'

/** 获取实体变更历史 */
export const getEntityHistory = (entityType: string, entityId: number, page = 1, pageSize = 20) =>
  apiClient.get(`/history/${entityType}/${entityId}`, {
    params: { page, pageSize }
  })

/** 获取用户操作历史 */
export const getUserActivity = (userId: number, page = 1, pageSize = 20) =>
  apiClient.get(`/history/user/${userId}`, {
    params: { page, pageSize }
  })

/** 获取所有变更历史 */
export const getAllHistory = (page = 1, pageSize = 20, entityType?: string) =>
  apiClient.get('/history', {
    params: { page, pageSize, entityType }
  })
