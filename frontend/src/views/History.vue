<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">变更历史</h1>
        <p class="page-desc">查看元数据的所有变更记录和操作日志</p>
      </div>
    </div>

    <!-- 过滤条件 -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <el-select v-model="filterEntityType" placeholder="实体类型" clearable style="width: 140px" @change="loadHistory">
          <el-option label="表" value="TABLE" />
          <el-option label="字段" value="COLUMN" />
          <el-option label="血缘" value="LINEAGE" />
          <el-option label="目录" value="CATALOG" />
        </el-select>

        <el-select v-model="filterOperation" placeholder="操作类型" clearable style="width: 130px" @change="loadHistory">
          <el-option label="创建" value="CREATE" />
          <el-option label="更新" value="UPDATE" />
          <el-option label="删除" value="DELETE" />
        </el-select>

        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          style="width: 240px"
          @change="loadHistory"
        />

        <el-button @click="resetFilter">
          <el-icon><Refresh /></el-icon>
          重置
        </el-button>
      </div>
    </el-card>

    <!-- 时间线视图 / 表格视图切换 -->
    <div class="view-toggle">
      <el-radio-group v-model="viewMode" size="small">
        <el-radio-button value="table">
          <el-icon><List /></el-icon>
          表格视图
        </el-radio-button>
        <el-radio-button value="timeline">
          <el-icon><Clock /></el-icon>
          时间线视图
        </el-radio-button>
      </el-radio-group>
    </div>

    <!-- 表格视图 -->
    <el-card v-if="viewMode === 'table'" shadow="never" class="content-card">
      <el-table :data="historyList" v-loading="loading" stripe>
        <el-table-column prop="changedAt" label="时间" width="160">
          <template #default="{ row }">
            <span class="text-muted">{{ formatDate(row.changedAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="entityType" label="实体类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="entityTypeTag(row.entityType)">{{ entityTypeLabel(row.entityType) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="entityId" label="实体 ID" width="100">
          <template #default="{ row }">
            <span class="font-mono text-secondary">{{ row.entityId }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="operation" label="操作" width="90">
          <template #default="{ row }">
            <el-tag :type="operationTag(row.operation)" size="small">{{ operationLabel(row.operation) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="fieldName" label="字段" width="120">
          <template #default="{ row }">
            <span class="font-mono">{{ row.fieldName || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="变更内容" min-width="200">
          <template #default="{ row }">
            <div v-if="row.oldValue || row.newValue" class="change-content">
              <span v-if="row.oldValue" class="old-value">{{ truncate(row.oldValue, 40) }}</span>
              <el-icon v-if="row.oldValue && row.newValue"><ArrowRight /></el-icon>
              <span v-if="row.newValue" class="new-value">{{ truncate(row.newValue, 40) }}</span>
            </div>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>

        <el-table-column prop="changedByName" label="操作人" width="100">
          <template #default="{ row }">
            <span>{{ row.changedByName || `用户 ${row.changedBy}` }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination
        :total="total"
        :page="currentPage"
        :page-size="pageSize"
        @change="handlePageChange"
      />
    </el-card>

    <!-- 时间线视图 -->
    <el-card v-else shadow="never" class="content-card">
      <el-timeline v-loading="loading">
        <el-timeline-item
          v-for="item in historyList"
          :key="item.id"
          :timestamp="formatDate(item.changedAt)"
          placement="top"
          :type="timelineType(item.operation)"
        >
          <div class="timeline-content">
            <div class="timeline-header">
              <el-tag :type="entityTypeTag(item.entityType)" size="small">{{ entityTypeLabel(item.entityType) }}</el-tag>
              <el-tag :type="operationTag(item.operation)" size="small">{{ operationLabel(item.operation) }}</el-tag>
              <span class="timeline-user">by {{ item.changedByName || `用户 ${item.changedBy}` }}</span>
            </div>
            <div v-if="item.fieldName" class="timeline-field">
              字段: <span class="font-mono">{{ item.fieldName }}</span>
            </div>
            <div v-if="item.oldValue || item.newValue" class="timeline-change">
              <span v-if="item.oldValue" class="old-value">{{ truncate(item.oldValue, 60) }}</span>
              <el-icon v-if="item.oldValue && item.newValue"><ArrowRight /></el-icon>
              <span v-if="item.newValue" class="new-value">{{ truncate(item.newValue, 60) }}</span>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>

      <Pagination
        :total="total"
        :page="currentPage"
        :page-size="pageSize"
        @change="handlePageChange"
      />
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="showDetailDialog" title="变更详情" width="600px">
      <div v-if="selectedHistory" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="时间">{{ formatDate(selectedHistory.changedAt) }}</el-descriptions-item>
          <el-descriptions-item label="操作人">{{ selectedHistory.changedByName || `用户 ${selectedHistory.changedBy}` }}</el-descriptions-item>
          <el-descriptions-item label="实体类型">{{ entityTypeLabel(selectedHistory.entityType) }}</el-descriptions-item>
          <el-descriptions-item label="实体 ID">{{ selectedHistory.entityId }}</el-descriptions-item>
          <el-descriptions-item label="操作类型">{{ operationLabel(selectedHistory.operation) }}</el-descriptions-item>
          <el-descriptions-item label="字段名">{{ selectedHistory.fieldName || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="selectedHistory.oldValue" class="value-section">
          <div class="value-label">变更前:</div>
          <pre class="value-content old">{{ formatJson(selectedHistory.oldValue) }}</pre>
        </div>

        <div v-if="selectedHistory.newValue" class="value-section">
          <div class="value-label">变更后:</div>
          <pre class="value-content new">{{ formatJson(selectedHistory.newValue) }}</pre>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as historyApi from '@/api/history'
import type { ChangeHistory } from '@/types'
import Pagination from '@/components/Pagination.vue'
import dayjs from 'dayjs'

const loading = ref(false)
const historyList = ref<ChangeHistory[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const viewMode = ref<'table' | 'timeline'>('table')
const filterEntityType = ref('')
const filterOperation = ref('')
const dateRange = ref<[string, string] | null>(null)
const showDetailDialog = ref(false)
const selectedHistory = ref<ChangeHistory | null>(null)

async function loadHistory() {
  loading.value = true
  try {
    const res = await historyApi.getAllHistory(currentPage.value, pageSize.value, filterEntityType.value || undefined)
    const apiData = res.data  // { success, message, data: [...], total, page, ... }
    historyList.value = apiData?.data || []
    total.value = apiData?.total ?? 0
  } catch {
    historyList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  filterEntityType.value = ''
  filterOperation.value = ''
  dateRange.value = null
  currentPage.value = 1
  loadHistory()
}

function handlePageChange(page: number, size: number) {
  currentPage.value = page
  pageSize.value = size
  loadHistory()
}

function showDetail(item: ChangeHistory) {
  selectedHistory.value = item
  showDetailDialog.value = true
}

function entityTypeLabel(type: string) {
  const map: Record<string, string> = { TABLE: '表', COLUMN: '字段', LINEAGE: '血缘', CATALOG: '目录' }
  return map[type] || type
}

function entityTypeTag(type: string) {
  const map: Record<string, string> = { TABLE: '', COLUMN: 'info', LINEAGE: 'warning', CATALOG: 'success' }
  return map[type] || ''
}

function operationLabel(op: string) {
  const map: Record<string, string> = { CREATE: '创建', UPDATE: '更新', DELETE: '删除' }
  return map[op] || op
}

function operationTag(op: string) {
  const map: Record<string, string> = { CREATE: 'success', UPDATE: '', DELETE: 'danger' }
  return map[op] || ''
}

function timelineType(op: string): any {
  const map: Record<string, string> = { CREATE: 'success', UPDATE: 'primary', DELETE: 'danger' }
  return map[op] || 'info'
}

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

function truncate(str: string, len: number) {
  if (!str) return ''
  return str.length > len ? str.substring(0, len) + '...' : str
}

function formatJson(str: string) {
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

onMounted(loadHistory)
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

.filter-card {
  margin-bottom: 12px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.view-toggle {
  margin-bottom: 12px;
}

.content-card {
  min-height: 400px;
}

.change-content {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.old-value {
  color: #EF4444;
  background: #FEE2E2;
  padding: 1px 6px;
  border-radius: 3px;
  font-family: var(--font-mono);
}

.new-value {
  color: #10B981;
  background: #D1FAE5;
  padding: 1px 6px;
  border-radius: 3px;
  font-family: var(--font-mono);
}

.timeline-content {
  padding: 8px 12px;
  background: white;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.timeline-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.timeline-user {
  font-size: 12px;
  color: var(--color-text-muted);
}

.timeline-field {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}

.timeline-change {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.value-section {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.value-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.value-content {
  font-family: var(--font-mono);
  font-size: 12px;
  padding: 12px;
  border-radius: var(--radius-sm);
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
}

.value-content.old {
  background: #FEF2F2;
  border: 1px solid #FECACA;
  color: #991B1B;
}

.value-content.new {
  background: #F0FDF4;
  border: 1px solid #BBF7D0;
  color: #166534;
}
</style>
