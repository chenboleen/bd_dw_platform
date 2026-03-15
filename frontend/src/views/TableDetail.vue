<template>
  <div class="page-container">
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="8" animated />
    </div>

    <template v-else-if="table">
      <!-- 页面标题 -->
      <div class="page-header">
        <div class="header-left">
          <el-button link @click="$router.back()">
            <el-icon><ArrowLeft /></el-icon>
            返回
          </el-button>
          <div class="title-info">
            <h1 class="page-title font-mono">{{ table.databaseName }}.{{ table.tableName }}</h1>
            <div class="title-meta">
              <el-tag :type="tableTypeTag(table.tableType)" size="small">{{ tableTypeLabel(table.tableType) }}</el-tag>
              <span class="text-muted">更新于 {{ formatDate(table.updatedAt) }}</span>
            </div>
          </div>
        </div>
        <div class="header-actions">
          <el-button @click="showLineage(table.id)">
            <el-icon><Share /></el-icon>
            查看血缘
          </el-button>
          <PermissionGuard action="update">
            <el-button type="primary" @click="showEditDialog = true">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
          </PermissionGuard>
        </div>
      </div>

      <!-- 基本信息 -->
      <el-row :gutter="16" class="info-row">
        <el-col :span="16">
          <el-card shadow="never" class="info-card">
            <template #header>
              <span class="card-title">基本信息</span>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="数据库">
                <span class="font-mono">{{ table.databaseName }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="表名">
                <span class="font-mono">{{ table.tableName }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="表类型">
                <el-tag :type="tableTypeTag(table.tableType)" size="small">{{ tableTypeLabel(table.tableType) }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="数据大小">
                <span class="font-mono">{{ formatSize(table.dataSizeBytes) }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="存储格式">
                {{ table.storageFormat || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">
                {{ formatDate(table.createdAt) }}
              </el-descriptions-item>
              <el-descriptions-item label="所有者">
                {{ (table as any).ownerName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="所属数据域">
                <template v-if="(table as any).catalogName">
                  <el-tag size="small" type="info">{{ (table as any).catalogName }}</el-tag>
                  <span class="text-muted" style="margin-left: 6px; font-size: 12px;">
                    层级 {{ (table as any).catalogLevel }}
                  </span>
                </template>
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item label="存储路径" :span="2">
                <span class="font-mono text-secondary">{{ table.storageLocation || '-' }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="描述" :span="2">
                {{ table.description || '暂无描述' }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>

        <el-col :span="8">
          <!-- 质量指标 -->
          <el-card shadow="never" class="quality-card">
            <template #header>
              <span class="card-title">数据质量</span>
            </template>
            <div v-if="qualityMetrics" class="quality-metrics">
              <div class="metric-item">
                <div class="metric-label">记录数</div>
                <div class="metric-value font-mono">{{ qualityMetrics.recordCount?.toLocaleString() || '-' }}</div>
              </div>
              <div class="metric-item">
                <div class="metric-label">空值率</div>
                <div class="metric-value">
                  <el-progress
                    :percentage="Math.round((qualityMetrics.nullRate || 0) * 100)"
                    :color="nullRateColor(qualityMetrics.nullRate)"
                    :stroke-width="8"
                  />
                </div>
              </div>
              <div class="metric-item">
                <div class="metric-label">数据新鲜度</div>
                <div class="metric-value">{{ qualityMetrics.dataFreshnessHours ? `${qualityMetrics.dataFreshnessHours}小时前` : '-' }}</div>
              </div>
            </div>
            <el-empty v-else description="暂无质量数据" :image-size="60" />
          </el-card>
        </el-col>
      </el-row>

      <!-- 字段列表 -->
      <el-card shadow="never" class="columns-card">
        <template #header>
          <div class="card-header">
            <span class="card-title">字段列表 ({{ columns.length }})</span>
            <PermissionGuard action="update">
              <el-button size="small" type="primary" @click="showAddColumnDialog = true">
                <el-icon><Plus /></el-icon>
                添加字段
              </el-button>
            </PermissionGuard>
          </div>
        </template>

        <el-table :data="columns" stripe size="small">
          <el-table-column prop="columnOrder" label="序号" width="60" />
          <el-table-column prop="columnName" label="字段名" min-width="160">
            <template #default="{ row }">
              <span class="font-mono">{{ row.columnName }}</span>
              <el-tag v-if="row.isPartitionKey" size="small" type="warning" style="margin-left: 6px">分区键</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="dataType" label="数据类型" width="140">
            <template #default="{ row }">
              <span class="font-mono text-primary">{{ row.dataType }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="isNullable" label="可空" width="70">
            <template #default="{ row }">
              <el-icon :color="row.isNullable ? '#10B981' : '#EF4444'">
                <CircleCheck v-if="row.isNullable" />
                <CircleClose v-else />
              </el-icon>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="text-secondary">{{ row.description || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <PermissionGuard action="update">
                <el-button link type="primary" size="small" @click="editColumn(row)">编辑</el-button>
              </PermissionGuard>
              <PermissionGuard action="delete">
                <el-popconfirm title="确认删除此字段？" @confirm="handleDeleteColumn(row.id)">
                  <template #reference>
                    <el-button link type="danger" size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </PermissionGuard>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <!-- 编辑对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑表信息" width="560px" destroy-on-close>
      <el-form :model="editForm" label-width="100px" label-position="left">
        <el-form-item label="数据库名">
          <el-input :value="table?.databaseName" disabled />
        </el-form-item>
        <el-form-item label="表名">
          <el-input :value="table?.tableName" disabled class="font-mono" />
        </el-form-item>
        <el-form-item label="表类型">
          <el-select :model-value="table?.tableType" disabled style="width: 100%">
            <el-option label="普通表" value="TABLE" />
            <el-option label="视图" value="VIEW" />
            <el-option label="外部表" value="EXTERNAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="请输入表的描述信息" />
        </el-form-item>
        <el-form-item label="存储格式">
          <el-select v-model="editForm.storageFormat" clearable style="width: 100%">
            <el-option label="ORC" value="ORC" />
            <el-option label="Parquet" value="PARQUET" />
            <el-option label="TextFile" value="TEXTFILE" />
            <el-option label="Avro" value="AVRO" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据域">
          <el-select
            v-model="selectedCatalogId"
            placeholder="选择数据域（可选）"
            clearable
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="item in catalogFlatList"
              :key="item.id"
              :label="item.path || item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleUpdateTable">保存修改</el-button>
      </template>
    </el-dialog>

    <!-- 添加字段对话框 -->
    <el-dialog v-model="showAddColumnDialog" :title="editingColumn ? '编辑字段' : '添加字段'" width="500px" destroy-on-close>
      <el-form :model="columnForm" label-width="90px">
        <el-form-item label="字段名" required>
          <el-input v-model="columnForm.columnName" class="font-mono" :disabled="!!editingColumn" />
        </el-form-item>
        <el-form-item label="数据类型" required>
          <el-select v-model="columnForm.dataType" allow-create filterable style="width: 100%">
            <el-option label="STRING" value="STRING" />
            <el-option label="INT" value="INT" />
            <el-option label="BIGINT" value="BIGINT" />
            <el-option label="DOUBLE" value="DOUBLE" />
            <el-option label="DECIMAL" value="DECIMAL(18,2)" />
            <el-option label="TIMESTAMP" value="TIMESTAMP" />
            <el-option label="DATE" value="DATE" />
            <el-option label="BOOLEAN" value="BOOLEAN" />
            <el-option label="ARRAY" value="ARRAY<STRING>" />
            <el-option label="MAP" value="MAP<STRING,STRING>" />
          </el-select>
        </el-form-item>
        <el-form-item label="可空">
          <el-switch v-model="columnForm.isNullable" />
        </el-form-item>
        <el-form-item label="分区键">
          <el-switch v-model="columnForm.isPartitionKey" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="columnForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddColumnDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitColumn">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as tableApi from '@/api/tables'
import * as qualityApi from '@/api/quality'
import type { TableMetadata, ColumnMetadata, TableUpdateRequest, ColumnCreateRequest, ColumnUpdateRequest } from '@/types'
import { ElMessage } from 'element-plus'
import PermissionGuard from '@/components/PermissionGuard.vue'
import dayjs from 'dayjs'

interface CatalogFlatItem {
  id: number
  name: string
  path: string
  level: number
}

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const table = ref<TableMetadata | null>(null)
const columns = ref<ColumnMetadata[]>([])
const qualityMetrics = ref<any>(null)
const showEditDialog = ref(false)
const showAddColumnDialog = ref(false)
const editingColumn = ref<ColumnMetadata | null>(null)
const submitting = ref(false)
const catalogFlatList = ref<CatalogFlatItem[]>([])
const selectedCatalogId = ref<number | null>(null)

const editForm = reactive<TableUpdateRequest>({
  description: '',
  storageFormat: '',
  storageLocation: ''
})

const columnForm = reactive<ColumnCreateRequest>({
  tableId: 0,
  columnName: '',
  dataType: 'STRING',
  columnOrder: 0,
  isNullable: true,
  isPartitionKey: false,
  description: ''
})

async function loadData() {
  const id = Number(route.params.id)
  loading.value = true
  try {
    const [tableRes, columnsRes] = await Promise.all([
      tableApi.getTableById(id),
      tableApi.getTableColumns(id)
    ])
    table.value = tableRes.data?.data || tableRes.data
    columns.value = columnsRes.data?.data || columnsRes.data || []

    // 加载质量指标
    try {
      const qualityRes = await qualityApi.getQualityMetrics(id)
      qualityMetrics.value = qualityRes.data?.data || qualityRes.data
    } catch { /* 忽略质量数据加载失败 */ }

    // 初始化编辑表单
    if (table.value) {
      Object.assign(editForm, {
        description: table.value.description || '',
        storageFormat: table.value.storageFormat || '',
        storageLocation: table.value.storageLocation || ''
      })
      selectedCatalogId.value = (table.value as any).catalogId ?? null
    }
  } finally {
    loading.value = false
  }
}

async function handleUpdateTable() {
  if (!table.value) return
  submitting.value = true
  try {
    // 自动生成存储路径
    const storageLocation = table.value.storageLocation ||
      `hdfs://namenode/warehouse/${table.value.databaseName}/${table.value.tableName}`
    await tableApi.updateTable(table.value.id, {
      ...editForm,
      storageLocation
    })
    // 处理数据域关联变更
    const oldCatalogId = (table.value as any).catalogId ?? null
    if (selectedCatalogId.value !== oldCatalogId) {
      if (oldCatalogId) {
        await tableApi.removeTableFromCatalog(oldCatalogId, table.value.id).catch(() => {})
      }
      if (selectedCatalogId.value) {
        await tableApi.addTableToCatalog(selectedCatalogId.value, table.value.id)
      }
    }
    ElMessage.success('更新成功')
    showEditDialog.value = false
    await loadData()
  } finally {
    submitting.value = false
  }
}

function editColumn(col: ColumnMetadata) {
  editingColumn.value = col
  Object.assign(columnForm, {
    tableId: col.tableId,
    columnName: col.columnName,
    dataType: col.dataType,
    columnOrder: col.columnOrder,
    isNullable: col.isNullable,
    isPartitionKey: col.isPartitionKey,
    description: col.description || ''
  })
  showAddColumnDialog.value = true
}

async function handleSubmitColumn() {
  submitting.value = true
  try {
    if (editingColumn.value) {
      const updateData: ColumnUpdateRequest = {
        dataType: columnForm.dataType,
        isNullable: columnForm.isNullable,
        isPartitionKey: columnForm.isPartitionKey,
        description: columnForm.description
      }
      await tableApi.updateColumn(editingColumn.value.id, updateData)
      ElMessage.success('字段更新成功')
    } else {
      columnForm.tableId = table.value!.id
      columnForm.columnOrder = columns.value.length + 1
      await tableApi.createColumn(columnForm)
      ElMessage.success('字段添加成功')
    }
    showAddColumnDialog.value = false
    editingColumn.value = null
    await loadData()
  } finally {
    submitting.value = false
  }
}

async function handleDeleteColumn(id: number) {
  await tableApi.deleteColumn(id)
  ElMessage.success('字段删除成功')
  await loadData()
}

function showLineage(tableId: number) {
  router.push(`/lineage?tableId=${tableId}`)
}

function tableTypeLabel(type: string) {
  const map: Record<string, string> = { TABLE: '普通表', VIEW: '视图', EXTERNAL: '外部表' }
  return map[type] || type
}

function tableTypeTag(type: string) {
  const map: Record<string, string> = { TABLE: '', VIEW: 'success', EXTERNAL: 'warning' }
  return map[type] || ''
}

function formatSize(bytes?: number) {
  if (!bytes) return '-'
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`
  return `${(bytes / 1024 / 1024 / 1024).toFixed(1)} GB`
}

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

function nullRateColor(rate?: number) {
  if (!rate) return '#10B981'
  if (rate < 0.1) return '#10B981'
  if (rate < 0.3) return '#F59E0B'
  return '#EF4444'
}

onMounted(async () => {
  await loadData()
  try {
    const res = await tableApi.getCatalogFlat()
    catalogFlatList.value = (res as any)?.data?.data ?? (res as any)?.data ?? []
  } catch {
    catalogFlatList.value = []
  }
})
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.title-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.title-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.info-row {
  margin-bottom: 16px;
}

.info-card, .quality-card, .columns-card {
  margin-bottom: 16px;
}

.card-title {
  font-weight: 600;
  color: var(--color-text-primary);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.quality-metrics {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.metric-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.metric-label {
  font-size: 12px;
  color: var(--color-text-muted);
}

.metric-value {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.loading-state {
  padding: 24px;
}
</style>
