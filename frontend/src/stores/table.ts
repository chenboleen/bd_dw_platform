// 表元数据状态管理
import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as tableApi from '@/api/tables'
import type { TableMetadata, TableFilter, PaginationParams } from '@/types'
import { ElMessage } from 'element-plus'

export const useTableStore = defineStore('table', () => {
  // 状态
  const tables = ref<TableMetadata[]>([])
  const currentTable = ref<TableMetadata | null>(null)
  const total = ref(0)
  const loading = ref(false)
  const pagination = ref<PaginationParams>({ page: 1, pageSize: 20, sortBy: 'updatedAt', sortOrder: 'desc' })
  const filter = ref<TableFilter>({})

  /** 获取表列表 */
  async function fetchTables(newFilter?: TableFilter, newPagination?: Partial<PaginationParams>) {
    if (newFilter !== undefined) filter.value = newFilter
    if (newPagination) pagination.value = { ...pagination.value, ...newPagination }

    loading.value = true
    try {
      const response = await tableApi.listTables(filter.value, pagination.value)
      const apiData: any = response.data?.data || response.data
      const rawTables = apiData?.items || apiData?.data || []
      tables.value = Array.isArray(rawTables) ? rawTables : []
      total.value = apiData?.total || 0
      if (apiData?.page) pagination.value.page = apiData.page
      if (apiData?.pageSize || apiData?.size) pagination.value.pageSize = apiData?.pageSize || apiData?.size
    } catch {
      tables.value = []
      total.value = 0
      ElMessage.error('获取表列表失败')
    } finally {
      loading.value = false
    }
  }

  /** 获取表详情 */
  async function fetchTableById(id: number) {
    loading.value = true
    try {
      const response = await tableApi.getTableById(id)
      const apiData: any = response.data?.data || response.data
      currentTable.value = apiData
      return apiData
    } catch {
      ElMessage.error('获取表详情失败')
      return null
    } finally {
      loading.value = false
    }
  }

  /** 删除表 */
  async function deleteTable(id: number): Promise<boolean> {
    try {
      await tableApi.deleteTable(id)
      tables.value = tables.value.filter(t => t.id !== id)
      total.value--
      ElMessage.success('删除成功')
      return true
    } catch {
      return false
    }
  }

  return {
    tables, currentTable, total, loading, pagination, filter,
    fetchTables, fetchTableById, deleteTable
  }
})
