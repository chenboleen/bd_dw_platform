// 搜索状态管理
import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as searchApi from '@/api/search'
import type { SearchRequest, SearchResponse } from '@/types'

export const useSearchStore = defineStore('search', () => {
  const results = ref<SearchResponse | null>(null)
  const suggestions = ref<string[]>([])
  const loading = ref(false)
  const searchHistory = ref<string[]>([])

  /** 执行搜索 */
  async function search(params: SearchRequest) {
    loading.value = true
    try {
      const response = await searchApi.searchTables(params)
      // 后端返回的数据格式是: { success: true, message: "搜索成功", data: { results: [...], total: ... } }
      // 前端期望的格式是: { items: [...], total: ... }
      const apiData = response.data?.data ?? response.data
      results.value = {
        items: apiData?.items || [],
        total: apiData?.total || 0,
        page: apiData?.page || 1,
        pageSize: apiData?.pageSize || 20,
        totalPages: apiData?.totalPages || 1,
        took: apiData?.took
      } as SearchResponse

      // 记录搜索历史
      if (params.keyword && !searchHistory.value.includes(params.keyword)) {
        searchHistory.value.unshift(params.keyword)
        if (searchHistory.value.length > 10) {
          searchHistory.value.pop()
        }
      }
    } finally {
      loading.value = false
    }
  }

  /** 获取搜索建议 */
  async function fetchSuggestions(prefix: string) {
    if (!prefix || prefix.length < 2) {
      suggestions.value = []
      return
    }
    try {
      const response = await searchApi.getSuggestions(prefix)
      suggestions.value = response.data
    } catch {
      suggestions.value = []
    }
  }

  /** 清除搜索结果 */
  function clearResults() {
    results.value = null
  }

  return {
    results, suggestions, loading, searchHistory,
    search, fetchSuggestions, clearResults
  }
}, {
  persist: {
    paths: ['searchHistory']
  }
})
