<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">导入导出</h1>
        <p class="page-desc">批量导入元数据或导出数据资产</p>
      </div>
    </div>

    <el-tabs v-model="activeTab" type="border-card">
      <!-- 导入 Tab -->
      <el-tab-pane label="数据导入" name="import">
        <div class="tab-content">
          <el-row :gutter="24">
            <!-- CSV 导入 -->
            <el-col :span="12">
              <div class="import-card">
                <div class="import-icon">
                  <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                    <path d="M14 2H6C5.46957 2 4.96086 2.21071 4.58579 2.58579C4.21071 2.96086 4 3.46957 4 4V20C4 20.5304 4.21071 21.0391 4.58579 21.4142C4.96086 21.7893 5.46957 22 6 22H18C18.5304 22 19.0391 21.7893 19.4142 21.4142C19.7893 21.0391 20 20.5304 20 20V8L14 2Z" stroke="#10B981" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <polyline points="14 2 14 8 20 8" stroke="#10B981" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <line x1="16" y1="13" x2="8" y2="13" stroke="#10B981" stroke-width="2" stroke-linecap="round"/>
                    <line x1="16" y1="17" x2="8" y2="17" stroke="#10B981" stroke-width="2" stroke-linecap="round"/>
                    <polyline points="10 9 9 9 8 9" stroke="#10B981" stroke-width="2" stroke-linecap="round"/>
                  </svg>
                </div>
                <h3>CSV 文件导入</h3>
                <p class="import-desc">支持标准 CSV 格式，包含表名、字段名、数据类型等信息</p>

                <el-upload
                  ref="csvUploadRef"
                  :auto-upload="false"
                  :limit="1"
                  accept=".csv"
                  :on-change="handleCsvFileChange"
                  :on-remove="() => csvFile = null"
                  drag
                  class="upload-area"
                >
                  <el-icon class="upload-icon"><Upload /></el-icon>
                  <div class="upload-text">拖拽 CSV 文件到此处，或 <em>点击上传</em></div>
                  <div class="upload-hint">支持 .csv 格式，最大 100MB</div>
                </el-upload>

                <el-button
                  type="primary"
                  :loading="csvImporting"
                  :disabled="!csvFile"
                  @click="handleCsvImport"
                  style="width: 100%; margin-top: 12px"
                >
                  <el-icon><Upload /></el-icon>
                  开始导入
                </el-button>
              </div>
            </el-col>

            <!-- JSON 导入 -->
            <el-col :span="12">
              <div class="import-card">
                <div class="import-icon json">
                  <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                    <path d="M14 2H6C5.46957 2 4.96086 2.21071 4.58579 2.58579C4.21071 2.96086 4 3.46957 4 4V20C4 20.5304 4.21071 21.0391 4.58579 21.4142C4.96086 21.7893 5.46957 22 6 22H18C18.5304 22 19.0391 21.7893 19.4142 21.4142C19.7893 21.0391 20 20.5304 20 20V8L14 2Z" stroke="#3B82F6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <polyline points="14 2 14 8 20 8" stroke="#3B82F6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </div>
                <h3>JSON 文件导入</h3>
                <p class="import-desc">支持 JSON 格式，可包含完整的表结构和字段信息</p>

                <el-upload
                  ref="jsonUploadRef"
                  :auto-upload="false"
                  :limit="1"
                  accept=".json"
                  :on-change="handleJsonFileChange"
                  :on-remove="() => jsonFile = null"
                  drag
                  class="upload-area"
                >
                  <el-icon class="upload-icon"><Upload /></el-icon>
                  <div class="upload-text">拖拽 JSON 文件到此处，或 <em>点击上传</em></div>
                  <div class="upload-hint">支持 .json 格式，最大 100MB</div>
                </el-upload>

                <el-button
                  type="primary"
                  :loading="jsonImporting"
                  :disabled="!jsonFile"
                  @click="handleJsonImport"
                  style="width: 100%; margin-top: 12px"
                >
                  <el-icon><Upload /></el-icon>
                  开始导入
                </el-button>
              </div>
            </el-col>
          </el-row>

          <!-- 导入结果 -->
          <div v-if="importResult" class="import-result">
            <el-alert
              :type="importResult.failureCount === 0 ? 'success' : 'warning'"
              :title="`导入完成：成功 ${importResult.successCount} 条，失败 ${importResult.failureCount} 条`"
              show-icon
              :closable="false"
            />
            <div v-if="importResult.errors.length > 0" class="import-errors">
              <div class="errors-title">错误详情:</div>
              <el-table :data="importResult.errors" size="small" max-height="200">
                <el-table-column prop="row" label="行号" width="80" />
                <el-table-column prop="message" label="错误信息" />
              </el-table>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 导出 Tab -->
      <el-tab-pane label="数据导出" name="export">
        <div class="tab-content">
          <el-row :gutter="24">
            <!-- 导出配置 -->
            <el-col :span="10">
              <el-card shadow="never" class="export-config-card">
                <template #header>
                  <span class="card-title">导出配置</span>
                </template>

                <el-form :model="exportForm" label-width="90px">
                  <el-form-item label="导出格式">
                    <el-radio-group v-model="exportForm.format">
                      <el-radio-button value="CSV">CSV</el-radio-button>
                      <el-radio-button value="JSON">JSON</el-radio-button>
                    </el-radio-group>
                  </el-form-item>

                  <el-form-item label="数据库">
                    <el-input v-model="exportForm.databaseName" placeholder="留空则导出全部" clearable />
                  </el-form-item>

                  <el-form-item label="表类型">
                    <el-select v-model="exportForm.tableType" placeholder="全部类型" clearable style="width: 100%">
                      <el-option label="普通表" value="TABLE" />
                      <el-option label="视图" value="VIEW" />
                      <el-option label="外部表" value="EXTERNAL" />
                    </el-select>
                  </el-form-item>

                  <el-form-item label="时间范围">
                    <el-date-picker
                      v-model="exportDateRange"
                      type="daterange"
                      range-separator="至"
                      start-placeholder="开始日期"
                      end-placeholder="结束日期"
                      format="YYYY-MM-DD"
                      value-format="YYYY-MM-DD"
                      style="width: 100%"
                    />
                  </el-form-item>

                  <el-form-item>
                    <el-button type="primary" :loading="exporting" @click="handleExport" style="width: 100%">
                      <el-icon><Download /></el-icon>
                      创建导出任务
                    </el-button>
                  </el-form-item>
                </el-form>
              </el-card>
            </el-col>

            <!-- 导出任务列表 -->
            <el-col :span="14">
              <el-card shadow="never" class="export-tasks-card">
                <template #header>
                  <div class="card-header">
                    <span class="card-title">导出任务</span>
                    <el-button link size="small" @click="loadExportTasks">
                      <el-icon><Refresh /></el-icon>
                    </el-button>
                  </div>
                </template>

                <el-table :data="exportTasks" size="small" v-loading="tasksLoading">
                  <el-table-column prop="createdAt" label="创建时间" width="140">
                    <template #default="{ row }">
                      <span class="text-muted">{{ formatDate(row.createdAt) }}</span>
                    </template>
                  </el-table-column>

                  <el-table-column prop="format" label="格式" width="70">
                    <template #default="{ row }">
                      <el-tag size="small">{{ row.format }}</el-tag>
                    </template>
                  </el-table-column>

                  <el-table-column prop="status" label="状态" width="90">
                    <template #default="{ row }">
                      <el-tag :type="taskStatusTag(row.status)" size="small">
                        {{ taskStatusLabel(row.status) }}
                      </el-tag>
                    </template>
                  </el-table-column>

                  <el-table-column prop="recordCount" label="记录数" width="90">
                    <template #default="{ row }">
                      <span class="font-mono">{{ row.recordCount?.toLocaleString() || '-' }}</span>
                    </template>
                  </el-table-column>

                  <el-table-column label="操作" width="80">
                    <template #default="{ row }">
                      <el-button
                        v-if="row.status === 'COMPLETED'"
                        link
                        type="primary"
                        size="small"
                        @click="downloadFile(row.taskId)"
                      >
                        下载
                      </el-button>
                      <span v-else-if="row.status === 'RUNNING'" class="text-muted">处理中...</span>
                      <span v-else-if="row.status === 'FAILED'" class="text-error">失败</span>
                    </template>
                  </el-table-column>
                </el-table>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import * as importExportApi from '@/api/importExport'
import type { ExportRequest, ExportStatusResponse, ImportResult } from '@/types'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const activeTab = ref('import')
const csvFile = ref<File | null>(null)
const jsonFile = ref<File | null>(null)
const csvImporting = ref(false)
const jsonImporting = ref(false)
const importResult = ref<ImportResult | null>(null)
const exporting = ref(false)
const tasksLoading = ref(false)
const exportTasks = ref<ExportStatusResponse[]>([])
const exportDateRange = ref<[string, string] | null>(null)

const exportForm = reactive<ExportRequest>({
  format: 'CSV',
  databaseName: '',
  tableType: undefined
})

function handleCsvFileChange(file: any) {
  csvFile.value = file.raw
}

function handleJsonFileChange(file: any) {
  jsonFile.value = file.raw
}

async function handleCsvImport() {
  if (!csvFile.value) return
  csvImporting.value = true
  importResult.value = null
  try {
    const res = await importExportApi.importFromCsv(csvFile.value)
    importResult.value = res.data
    ElMessage.success(`导入完成：成功 ${res.data.successCount} 条`)
  } finally {
    csvImporting.value = false
  }
}

async function handleJsonImport() {
  if (!jsonFile.value) return
  jsonImporting.value = true
  importResult.value = null
  try {
    const res = await importExportApi.importFromJson(jsonFile.value)
    importResult.value = res.data
    ElMessage.success(`导入完成：成功 ${res.data.successCount} 条`)
  } finally {
    jsonImporting.value = false
  }
}

async function handleExport() {
  exporting.value = true
  try {
    const data: ExportRequest = {
      format: exportForm.format,
      databaseName: exportForm.databaseName || undefined,
      tableType: exportForm.tableType || undefined,
      startDate: exportDateRange.value?.[0],
      endDate: exportDateRange.value?.[1]
    }
    await importExportApi.createExportTask(data)
    ElMessage.success('导出任务已创建，请稍后查看任务列表')
    await loadExportTasks()
  } finally {
    exporting.value = false
  }
}

async function loadExportTasks() {
  tasksLoading.value = true
  try {
    const res = await importExportApi.listExportTasks()
    exportTasks.value = res.data.items || res.data
  } finally {
    tasksLoading.value = false
  }
}

async function downloadFile(taskId: number) {
  try {
    const res = await importExportApi.downloadExportFile(taskId)
    const blob = new Blob([res.data])
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `export_${taskId}.${exportForm.format.toLowerCase()}`
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('下载失败')
  }
}

function taskStatusLabel(status: string) {
  const map: Record<string, string> = { PENDING: '等待中', RUNNING: '处理中', COMPLETED: '已完成', FAILED: '失败' }
  return map[status] || status
}

function taskStatusTag(status: string) {
  const map: Record<string, string> = { PENDING: 'info', RUNNING: 'warning', COMPLETED: 'success', FAILED: 'danger' }
  return map[status] || ''
}

function formatDate(date: string) {
  return dayjs(date).format('MM-DD HH:mm')
}

onMounted(loadExportTasks)
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

.tab-content {
  padding: 16px 0;
}

.import-card {
  background: white;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 24px;
  text-align: center;
}

.import-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 16px;
}

.import-icon svg {
  width: 100%;
  height: 100%;
}

.import-card h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 8px;
}

.import-desc {
  font-size: 13px;
  color: var(--color-text-muted);
  margin-bottom: 16px;
}

.upload-area {
  width: 100%;
}

.upload-icon {
  font-size: 40px;
  color: #94A3B8;
  margin-bottom: 8px;
}

.upload-text {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.upload-text em {
  color: var(--color-primary);
  font-style: normal;
}

.upload-hint {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 4px;
}

.import-result {
  margin-top: 20px;
}

.import-errors {
  margin-top: 12px;
}

.errors-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
}

.export-config-card, .export-tasks-card {
  height: 100%;
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
</style>
