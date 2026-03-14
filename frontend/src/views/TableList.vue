<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div>
        <h1 class="page-title">表元数据管理</h1>
        <p class="page-desc">管理数据仓库中的表和字段元数据信息</p>
      </div>
      <PermissionGuard action="update">
        <el-button type="primary" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>
          新建表
        </el-button>
      </PermissionGuard>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="kpi-card" v-for="stat in stats" :key="stat.label">
        <div class="kpi-value">{{ stat.value }}</div>
        <div class="kpi-label">{{ stat.label }}</div>
        <div class="kpi-trend" :class="stat.trend > 0 ? 'up' : 'down'" v-if="stat.trend !== undefined">
          <el-icon><CaretTop v-if="stat.trend > 0" /><CaretBottom v-else /></el-icon>
          {{ Math.abs(stat.trend) }}%
        </div>
      </div>
    </div>

    <!-- 过滤和搜索 -->
    <el-card class="filter-card" shadow="never">
      <div class="filter-row">
        <el-input
          v-model="filterForm.tableName"
          placeholder="搜索表名..."
          prefix-icon="Search"
          clearable
          style="width: 240px"
          @input="debouncedFetch"
        />
        <el-select
          v-model="filterForm.databaseName"
          placeholder="选择数据库"
          clearable
          style="width: 180px"
          @change="handleFilterChange"
        >
          <el-option v-for="db in databases" :key="db" :label="db" :value="db" />
        </el-select>
        <el-select
          v-model="filterForm.tableType"
          placeholder="表类型"
          clearable
          style="width: 140px"
          @change="handleFilterChange"
        >
          <el-option label="普通表" value="TABLE" />
          <el-option label="视图" value="VIEW" />
          <el-option label="外部表" value="EXTERNAL" />
        </el-select>
        <el-button @click="resetFilter">
          <el-icon><Refresh /></el-icon>
          重置
        </el-button>
      </div>
    </el-card>

    <!-- 数据表格 -->
    <el-card shadow="never" class="table-card">
      <el-table
        :data="tableStore.tables"
        v-loading="tableStore.loading"
        stripe
        highlight-current-row
        @row-click="handleRowClick"
        style="width: 100%"
        :row-class-name="tableRowClassName"
      >
        <el-table-column type="index" width="50" label="#" />

        <el-table-column prop="databaseName" label="数据库" width="140" sortable>
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.databaseName }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="tableName" label="表名" min-width="180" sortable>
          <template #default="{ row }">
            <div class="table-name-cell">
              <el-icon class="table-icon"><Grid /></el-icon>
              <span class="font-mono">{{ row.tableName }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="tableType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="tableTypeTag(row.tableType)" size="small">
              {{ tableTypeLabel(row.tableType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-secondary">{{ row.description || '暂无描述' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="ownerName" label="所有者" width="110">
          <template #default="{ row }">
            <span class="text-secondary">{{ row.ownerName || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="catalogName" label="数据域" width="130" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-if="row.catalogName" size="small" type="info">{{ row.catalogName }}</el-tag>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>

        <el-table-column prop="dataSizeBytes" label="数据大小" width="120">
          <template #default="{ row }">
            <span class="font-mono text-secondary">{{ formatSize(row.dataSizeBytes) }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="updatedAt" label="更新时间" width="160" sortable>
          <template #default="{ row }">
            <span class="text-muted">{{ formatDate(row.updatedAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button link type="primary" size="small" @click.stop="viewTable(row)">
                查看
              </el-button>
              <PermissionGuard action="update">
                <el-button link type="primary" size="small" @click.stop="editTable(row)">
                  编辑
                </el-button>
              </PermissionGuard>
              <PermissionGuard action="delete">
                <el-popconfirm
                  title="确认删除此表？此操作不可撤销。"
                  @confirm="handleDelete(row.id)"
                  confirm-button-text="确认删除"
                  cancel-button-text="取消"
                  confirm-button-type="danger"
                >
                  <template #reference>
                    <el-button link type="danger" size="small" @click.stop>
                      删除
                    </el-button>
                  </template>
                </el-popconfirm>
              </PermissionGuard>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <Pagination
        :total="tableStore.total"
        :page="tableStore.pagination.page"
        :page-size="tableStore.pagination.pageSize"
        @change="handlePageChange"
      />
    </el-card>

    <!-- 创建/编辑表对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      :title="editingTable ? '编辑表信息' : '新建表'"
      width="600px"
      destroy-on-close
    >
      <el-form
        ref="tableFormRef"
        :model="tableForm"
        :rules="tableFormRules"
        label-width="100px"
        label-position="left"
      >
        <el-form-item label="数据库名" prop="databaseName">
          <el-input v-model="tableForm.databaseName" placeholder="例如: dw_ods" :disabled="!!editingTable" />
        </el-form-item>
        <el-form-item label="表名" prop="tableName">
          <el-input v-model="tableForm.tableName" placeholder="例如: user_info" :disabled="!!editingTable" class="font-mono" />
        </el-form-item>
        <el-form-item label="表类型" prop="tableType">
          <el-select v-model="tableForm.tableType" style="width: 100%">
            <el-option label="普通表" value="TABLE" />
            <el-option label="视图" value="VIEW" />
            <el-option label="外部表" value="EXTERNAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="tableForm.description" type="textarea" :rows="3" placeholder="请输入表的描述信息" />
        </el-form-item>
        <el-form-item label="存储格式">
          <el-select v-model="tableForm.storageFormat" placeholder="选择存储格式" clearable style="width: 100%">
            <el-option label="ORC" value="ORC" />
            <el-option label="Parquet" value="PARQUET" />
            <el-option label="TextFile" value="TEXTFILE" />
            <el-option label="Avro" value="AVRO" />
          </el-select>
        </el-form-item>
        <el-form-item label="存储路径">
          <el-input v-model="tableForm.storageLocation" placeholder="例如: hdfs://namenode/warehouse/dw_ods/user_info" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ editingTable ? '保存修改' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useTableStore } from '@/stores/table'
import { useAuthStore } from '@/stores/auth'
import * as tableApi from '@/api/tables'
import type { TableMetadata, TableCreateRequest, TableUpdateRequest } from '@/types'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useDebounceFn } from '@vueuse/core'
import Pagination from '@/components/Pagination.vue'
import PermissionGuard from '@/components/PermissionGuard.vue'
import dayjs from 'dayjs'

const router = useRouter()
const tableStore = useTableStore()
const authStore = useAuthStore()

const showCreateDialog = ref(false)
const editingTable = ref<TableMetadata | null>(null)
const submitting = ref(false)
const tableFormRef = ref<FormInstance>()

const filterForm = reactive({
  tableName: '',
  databaseName: '',
  tableType: '' as any
})

const tableForm = reactive<TableCreateRequest>({
  databaseName: '',
  tableName: '',
  tableType: 'TABLE',
  description: '',
  storageFormat: '',
  storageLocation: ''
})

const tableFormRules: FormRules = {
  databaseName: [{ required: true, message: '请输入数据库名', trigger: 'blur' }],
  tableName: [{ required: true, message: '请输入表名', trigger: 'blur' }],
  tableType: [{ required: true, message: '请选择表类型', trigger: 'change' }]
}

// 统计数据
const stats = computed(() => {
  const safeTables = (tableStore.tables || []) as any[]
  return [
    { label: '总表数', value: tableStore.total || 0, trend: undefined as number | undefined },
    { label: '普通表', value: safeTables.filter((t: any) => t?.tableType === 'TABLE').length, trend: undefined as number | undefined },
    { label: '视图', value: safeTables.filter((t: any) => t?.tableType === 'VIEW').length, trend: undefined as number | undefined },
    { label: '外部表', value: safeTables.filter((t: any) => t?.tableType === 'EXTERNAL').length, trend: undefined as number | undefined }
  ]
})

// 数据库列表
const databases = computed(() => {
  const safeTables = (tableStore.tables || []) as any[]
  const dbs = new Set(safeTables.map((t: any) => t?.databaseName).filter(Boolean))
  return Array.from(dbs).sort()
})

// 防抖搜索
const debouncedFetch = useDebounceFn(() => {
  tableStore.fetchTables({ tableName: filterForm.tableName || undefined })
}, 400)

function handleFilterChange() {
  tableStore.fetchTables({
    databaseName: filterForm.databaseName || undefined,
    tableType: filterForm.tableType || undefined,
    tableName: filterForm.tableName || undefined
  }, { page: 1 })
}

function resetFilter() {
  filterForm.tableName = ''
  filterForm.databaseName = ''
  filterForm.tableType = ''
  tableStore.fetchTables({}, { page: 1 })
}

function handlePageChange(page: number, pageSize: number) {
  tableStore.fetchTables(undefined, { page, pageSize })
}

function handleRowClick(row: TableMetadata) {
  router.push(`/tables/${row.id}`)
}

function viewTable(row: TableMetadata) {
  router.push(`/tables/${row.id}`)
}

function editTable(row: TableMetadata) {
  editingTable.value = row
  Object.assign(tableForm, {
    databaseName: row.databaseName,
    tableName: row.tableName,
    tableType: row.tableType,
    description: row.description || '',
    storageFormat: row.storageFormat || '',
    storageLocation: row.storageLocation || ''
  })
  showCreateDialog.value = true
}

async function handleDelete(id: number) {
  await tableStore.deleteTable(id)
}

async function handleSubmit() {
  if (!tableFormRef.value) return
  const valid = await tableFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (editingTable.value) {
      const updateData: TableUpdateRequest = {
        description: tableForm.description,
        storageFormat: tableForm.storageFormat,
        storageLocation: tableForm.storageLocation
      }
      await tableApi.updateTable(editingTable.value.id, updateData)
      ElMessage.success('更新成功')
    } else {
      await tableApi.createTable(tableForm)
      ElMessage.success('创建成功')
    }
    showCreateDialog.value = false
    editingTable.value = null
    await tableStore.fetchTables()
  } finally {
    submitting.value = false
  }
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
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`
  return `${(bytes / 1024 / 1024 / 1024).toFixed(1)} GB`
}

function formatDate(date: string) {
  return dayjs(date).format('MM-DD HH:mm')
}

function tableRowClassName({ row }: { row: TableMetadata }) {
  return ''
}

onMounted(() => {
  tableStore.fetchTables()
})
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
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

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.kpi-card {
  padding: 16px;
}

.kpi-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-primary);
  font-family: var(--font-mono);
  line-height: 1;
  margin-bottom: 4px;
}

.kpi-label {
  font-size: 12px;
  color: var(--color-text-muted);
}

.kpi-trend {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  margin-top: 4px;
}

.kpi-trend.up { color: var(--color-success); }
.kpi-trend.down { color: var(--color-error); }

.filter-card {
  margin-bottom: 12px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.table-card {
  flex: 1;
}

.table-name-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.table-icon {
  color: var(--color-primary-light);
  font-size: 14px;
}

.action-btns {
  display: flex;
  align-items: center;
  gap: 4px;
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
