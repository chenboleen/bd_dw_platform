<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">全文搜索</h1>
        <p class="page-desc">搜索表名、字段名、描述等元数据信息</p>
      </div>
    </div>

    <!-- 搜索框 -->
    <div class="search-section">
      <SearchBar
        ref="searchBarRef"
        placeholder="搜索表名、字段名、描述、标签..."
        :loading="searchStore.loading"
        @search="handleSearch"
        @clear="handleClear"
      />
    </div>

    <!-- 高级过滤 -->
    <el-card shadow="never" class="filter-card">
      <el-collapse v-model="showAdvancedFilter">
        <el-collapse-item title="高级过滤" name="filter">
          <div class="advanced-filter">
            <el-form :model="filterForm" inline>
              <el-form-item label="数据库">
                <el-input v-model="filterForm.databaseName" placeholder="数据库名" clearable style="width: 160px" />
              </el-form-item>
              <el-form-item label="表类型">
                <el-select v-model="filterForm.tableType" placeholder="全部" clearable style="width: 130px">
                  <el-option label="普通表" value="TABLE" />
                  <el-option label="视图" value="VIEW" />
                  <el-option label="外部表" value="EXTERNAL" />
                </el-select>
              </el-form-item>
              <el-form-item label="更新时间">
                <el-date-picker
                  v-model="dateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  style="width: 240px"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleAdvancedSearch">应用过滤</el-button>
                <el-button @click="resetFilter">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-collapse-item>
      </el-collapse>
    </el-card>

    <!-- 搜索结果 -->
    <div v-if="searchStore.results">
      <div class="result-header">
        <span class="result-count">
          找到 <strong>{{ searchStore.results.total || 0 }}</strong> 条结果
          <span v-if="searchStore.results.took" class="result-time">
            (耗时 {{ searchStore.results.took }}ms)
          </span>
        </span>
        <div class="result-sort">
          <span class="sort-label">排序:</span>
          <el-radio-group v-model="sortBy" size="small" @change="handleSearch(currentKeyword)">
            <el-radio-button value="relevance">相关性</el-radio-button>
            <el-radio-button value="updatedAt">更新时间</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <!-- 结果列表 -->
      <div class="result-list" v-loading="searchStore.loading">
        <div
          v-for="item in (searchStore.results.items || [])"
          :key="item.id"
          class="result-item"
          @click="viewTable(item.id)"
          role="button"
          tabindex="0"
          @keydown.enter="viewTable(item.id)"
        >
          <div class="result-item-header">
            <div class="result-title">
              <el-icon class="result-icon"><Grid /></el-icon>
              <span
                class="font-mono result-name"
                v-html="getHighlight(item, 'tableName') || `${item.databaseName || ''}.${item.tableName || ''}`"
              ></span>
              <el-tag :type="tableTypeTag(item.tableType)" size="small">
                {{ tableTypeLabel(item.tableType) }}
              </el-tag>
            </div>
            <span class="result-time text-muted">{{ item.updatedAt ? formatDate(item.updatedAt) : '' }}</span>
          </div>

          <div class="result-desc" v-if="item.description || getHighlight(item, 'description')">
            <span
              class="text-secondary"
              v-html="getHighlight(item, 'description') || item.description"
            ></span>
          </div>

          <div class="result-meta">
            <span class="meta-item">
              <el-icon><FolderOpened /></el-icon>
              {{ item.databaseName || '' }}
            </span>
            <span v-if="item.score" class="meta-item">
              <el-icon><Star /></el-icon>
              相关度: {{ (item.score * 100).toFixed(0) }}%
            </span>
          </div>
        </div>

        <el-empty v-if="!searchStore.results.items || searchStore.results.items.length === 0" description="未找到匹配的结果" />
      </div>

      <!-- 分页 -->
      <Pagination
        :total="searchStore.results.total || 0"
        :page="currentPage"
        :page-size="pageSize"
        @change="handlePageChange"
      />
    </div>

    <!-- 初始状态 -->
    <div v-else class="initial-state">
      <div class="search-tips">
        <h3>搜索提示</h3>
        <ul>
          <li>支持搜索表名、字段名、描述等信息</li>
          <li>使用空格分隔多个关键词</li>
          <li>支持模糊匹配，如 "user" 可匹配 "user_info"</li>
          <li>搜索结果按相关性排序</li>
        </ul>
      </div>

      <!-- 搜索历史 -->
      <div v-if="searchStore.searchHistory.length > 0" class="search-history">
        <h3>最近搜索</h3>
        <div class="history-tags">
          <el-tag
            v-for="(item, index) in searchStore.searchHistory"
            :key="index"
            class="history-tag"
            @click="handleSearch(item)"
            style="cursor: pointer"
          >
            {{ item }}
          </el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSearchStore } from '@/stores/search'
import SearchBar from '@/components/SearchBar.vue'
import Pagination from '@/components/Pagination.vue'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const searchStore = useSearchStore()

const searchBarRef = ref()
const currentKeyword = ref('')
const sortBy = ref('relevance')
const currentPage = ref(1)
const pageSize = ref(20)
const showAdvancedFilter = ref<string[]>([])
const dateRange = ref<[string, string] | null>(null)

const filterForm = reactive({
  databaseName: '',
  tableType: '' as any
})

onMounted(() => {
  // 从路由参数初始化搜索
  if (route.query.keyword) {
    currentKeyword.value = String(route.query.keyword)
    handleSearch(currentKeyword.value)
  }
})

async function handleSearch(keyword: string) {
  currentKeyword.value = keyword
  currentPage.value = 1
  await searchStore.search({
    keyword,
    databaseName: filterForm.databaseName || undefined,
    tableType: filterForm.tableType || undefined,
    startDate: dateRange.value?.[0],
    endDate: dateRange.value?.[1],
    page: 1,
    pageSize: pageSize.value
  })
}

async function handleAdvancedSearch() {
  await handleSearch(currentKeyword.value)
}

function handleClear() {
  searchStore.clearResults()
  currentKeyword.value = ''
}

function resetFilter() {
  filterForm.databaseName = ''
  filterForm.tableType = ''
  dateRange.value = null
}

async function handlePageChange(page: number, size: number) {
  currentPage.value = page
  pageSize.value = size
  await searchStore.search({
    keyword: currentKeyword.value,
    page,
    pageSize: size
  })
}

function viewTable(id: number) {
  router.push(`/tables/${id}`)
}

function getHighlight(item: any, field: string): string {
  if (!item || !item.highlight) return ''
  const highlights = item.highlight[field]
  if (highlights && Array.isArray(highlights) && highlights.length > 0) {
    return highlights[0].replace(/<em>/g, '<mark class="search-highlight">').replace(/<\/em>/g, '</mark>')
  }
  return ''
}

function tableTypeLabel(type: string) {
  const map: Record<string, string> = { TABLE: '普通表', VIEW: '视图', EXTERNAL: '外部表' }
  return map[type] || type
}

function tableTypeTag(type: string) {
  const map: Record<string, string> = { TABLE: '', VIEW: 'success', EXTERNAL: 'warning' }
  return map[type] || ''
}

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}
</script>

<style scoped>
.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.page-desc {
  font-size: 13px;
  color: var(--color-text-muted);
}

.search-section {
  margin-bottom: 16px;
}

.filter-card {
  margin-bottom: 16px;
}

.advanced-filter {
  padding: 8px 0;
}

.result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding: 0 4px;
}

.result-count {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.result-count strong {
  color: var(--color-primary);
  font-size: 16px;
}

.result-time {
  font-size: 12px;
  color: var(--color-text-muted);
}

.result-sort {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sort-label {
  font-size: 13px;
  color: var(--color-text-muted);
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 8px;
}

.result-item {
  background: white;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 16px;
  cursor: pointer;
  transition: all var(--transition-normal);
}

.result-item:hover {
  border-color: var(--color-primary-light);
  box-shadow: var(--shadow-sm);
  transform: translateY(-1px);
}

.result-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.result-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.result-icon {
  color: var(--color-primary-light);
}

.result-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.result-time {
  font-size: 12px;
}

.result-desc {
  font-size: 13px;
  margin-bottom: 8px;
  line-height: 1.5;
}

.result-meta {
  display: flex;
  gap: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.initial-state {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 8px;
}

.search-tips, .search-history {
  background: white;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 20px;
}

.search-tips h3, .search-history h3 {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 12px;
}

.search-tips ul {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.search-tips li {
  font-size: 13px;
  color: var(--color-text-secondary);
  padding-left: 16px;
  position: relative;
}

.search-tips li::before {
  content: '•';
  position: absolute;
  left: 0;
  color: var(--color-primary-light);
}

.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.history-tag:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

:deep(.search-highlight) {
  background-color: #FEF3C7;
  color: #92400E;
  padding: 0 2px;
  border-radius: 2px;
  font-style: normal;
}
</style>
