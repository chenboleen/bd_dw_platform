<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">数据质量监控</h1>
        <p class="page-desc">监控数据仓库各表的质量指标和趋势</p>
      </div>
    </div>

    <!-- 表选择 -->
    <el-card shadow="never" class="selector-card">
      <div class="selector-row">
        <el-select
          v-model="selectedTableId"
          filterable
          remote
          :remote-method="searchTables"
          :loading="searchLoading"
          placeholder="选择要查看的表"
          style="width: 320px"
          @change="loadQualityData"
        >
          <el-option
            v-for="table in tableOptions"
            :key="table.id"
            :label="`${table.databaseName}.${table.tableName}`"
            :value="table.id"
          />
        </el-select>

        <el-select v-model="trendDays" style="width: 120px" @change="loadTrend">
          <el-option label="近 7 天" :value="7" />
          <el-option label="近 30 天" :value="30" />
          <el-option label="近 90 天" :value="90" />
        </el-select>
      </div>
    </el-card>

    <div v-if="selectedTableId">
      <!-- 质量指标卡片 -->
      <div class="metrics-grid" v-loading="metricsLoading">
        <div class="metric-card" v-for="metric in metricCards" :key="metric.key">
          <div class="metric-icon" :style="{ backgroundColor: metric.bgColor }">
            <el-icon :style="{ color: metric.color }">
              <component :is="metric.icon" />
            </el-icon>
          </div>
          <div class="metric-info">
            <div class="metric-value" :style="{ color: metric.color }">{{ metric.value }}</div>
            <div class="metric-label">{{ metric.label }}</div>
          </div>
          <div class="metric-status">
            <el-tag :type="metric.status" size="small">{{ metric.statusText }}</el-tag>
          </div>
        </div>
      </div>

      <!-- 趋势图表 -->
      <el-row :gutter="16">
        <el-col :span="12">
          <el-card shadow="never" class="chart-card">
            <template #header>
              <span class="card-title">记录数趋势</span>
            </template>
            <div ref="recordCountChartRef" class="chart-container" role="img" aria-label="记录数趋势图"></div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="never" class="chart-card">
            <template #header>
              <span class="card-title">空值率趋势</span>
            </template>
            <div ref="nullRateChartRef" class="chart-container" role="img" aria-label="空值率趋势图"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 质量历史记录 -->
      <el-card shadow="never" class="history-card">
        <template #header>
          <span class="card-title">质量历史记录</span>
        </template>
        <el-table :data="trendData" size="small" v-loading="trendLoading">
          <el-table-column prop="measuredAt" label="测量时间" width="180">
            <template #default="{ row }">{{ formatDate(row.measuredAt) }}</template>
          </el-table-column>
          <el-table-column prop="recordCount" label="记录数" width="120">
            <template #default="{ row }">
              <span class="font-mono">{{ row.recordCount?.toLocaleString() }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="nullRate" label="空值率" width="120">
            <template #default="{ row }">
              <el-progress
                :percentage="Math.round((row.nullRate || 0) * 100)"
                :color="nullRateColor(row.nullRate)"
                :stroke-width="6"
                style="width: 100px"
              />
            </template>
          </el-table-column>
          <el-table-column prop="dataFreshnessHours" label="数据新鲜度" width="140">
            <template #default="{ row }">
              {{ row.dataFreshnessHours ? `${row.dataFreshnessHours} 小时前` : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="updateFrequency" label="更新频率" />
        </el-table>
      </el-card>
    </div>

    <el-empty v-else description="请选择一个表来查看数据质量指标" :image-size="100" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import * as qualityApi from '@/api/quality'
import * as tableApi from '@/api/tables'
import type { QualityMetrics, TableMetadata } from '@/types'
import { DataAnalysis, Warning, Timer, Histogram } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const selectedTableId = ref<number | null>(null)
const trendDays = ref(30)
const metricsLoading = ref(false)
const trendLoading = ref(false)
const searchLoading = ref(false)
const tableOptions = ref<TableMetadata[]>([])
const currentMetrics = ref<QualityMetrics | null>(null)
const trendData = ref<QualityMetrics[]>([])

const recordCountChartRef = ref<HTMLElement | null>(null)
const nullRateChartRef = ref<HTMLElement | null>(null)
let recordCountChart: echarts.ECharts | null = null
let nullRateChart: echarts.ECharts | null = null

// 质量指标卡片
const metricCards = computed(() => {
  const m = currentMetrics.value
  if (!m) return []

  const nullRate = m.nullRate || 0
  const freshnessHours = m.dataFreshnessHours || 0

  return [
    {
      key: 'recordCount',
      icon: Histogram,
      label: '总记录数',
      value: m.recordCount?.toLocaleString() || '-',
      color: '#1E40AF',
      bgColor: '#DBEAFE',
      status: 'info' as const,
      statusText: '正常'
    },
    {
      key: 'nullRate',
      icon: Warning,
      label: '空值率',
      value: `${(nullRate * 100).toFixed(1)}%`,
      color: nullRate < 0.1 ? '#10B981' : nullRate < 0.3 ? '#F59E0B' : '#EF4444',
      bgColor: nullRate < 0.1 ? '#D1FAE5' : nullRate < 0.3 ? '#FEF3C7' : '#FEE2E2',
      status: (nullRate < 0.1 ? 'success' : nullRate < 0.3 ? 'warning' : 'danger') as any,
      statusText: nullRate < 0.1 ? '优秀' : nullRate < 0.3 ? '一般' : '较差'
    },
    {
      key: 'freshness',
      icon: Timer,
      label: '数据新鲜度',
      value: freshnessHours ? `${freshnessHours}h` : '-',
      color: freshnessHours < 24 ? '#10B981' : freshnessHours < 72 ? '#F59E0B' : '#EF4444',
      bgColor: freshnessHours < 24 ? '#D1FAE5' : freshnessHours < 72 ? '#FEF3C7' : '#FEE2E2',
      status: (freshnessHours < 24 ? 'success' : freshnessHours < 72 ? 'warning' : 'danger') as any,
      statusText: freshnessHours < 24 ? '新鲜' : freshnessHours < 72 ? '一般' : '过期'
    },
    {
      key: 'frequency',
      icon: DataAnalysis,
      label: '更新频率',
      value: m.updateFrequency || '-',
      color: '#3B82F6',
      bgColor: '#DBEAFE',
      status: 'info' as const,
      statusText: '正常'
    }
  ]
})

async function searchTables(query: string) {
  if (!query) return
  searchLoading.value = true
  try {
    const res = await tableApi.listTables({ tableName: query }, { page: 1, pageSize: 20 })
    tableOptions.value = res.data?.data?.items ?? res.data?.items ?? []
  } finally {
    searchLoading.value = false
  }
}

async function loadQualityData() {
  if (!selectedTableId.value) return
  metricsLoading.value = true
  try {
    const res = await qualityApi.getQualityMetrics(selectedTableId.value)
    currentMetrics.value = res.data?.data || res.data
  } catch {
    currentMetrics.value = null
  } finally {
    metricsLoading.value = false
  }
  await loadTrend()
}

async function loadTrend() {
  if (!selectedTableId.value) return
  trendLoading.value = true
  try {
    const res = await qualityApi.getQualityTrend(selectedTableId.value, trendDays.value)
    trendData.value = res.data?.data ?? []
    await nextTick()
    renderCharts()
  } finally {
    trendLoading.value = false
  }
}

function renderCharts() {
  renderRecordCountChart()
  renderNullRateChart()
}

function renderRecordCountChart() {
  if (!recordCountChartRef.value) return
  if (!recordCountChart) {
    recordCountChart = echarts.init(recordCountChartRef.value)
  }

  const dates = trendData.value.map(d => dayjs(d.measuredAt).format('MM-DD'))
  const counts = trendData.value.map(d => d.recordCount || 0)

  recordCountChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: dates, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', axisLabel: { fontSize: 11 } },
    series: [{
      type: 'line',
      data: counts,
      smooth: true,
      lineStyle: { color: '#1E40AF', width: 2 },
      areaStyle: { color: 'rgba(30, 64, 175, 0.1)' },
      itemStyle: { color: '#1E40AF' },
      symbol: 'circle',
      symbolSize: 4
    }]
  })
}

function renderNullRateChart() {
  if (!nullRateChartRef.value) return
  if (!nullRateChart) {
    nullRateChart = echarts.init(nullRateChartRef.value)
  }

  const dates = trendData.value.map(d => dayjs(d.measuredAt).format('MM-DD'))
  const rates = trendData.value.map(d => ((d.nullRate || 0) * 100).toFixed(2))

  nullRateChart.setOption({
    tooltip: { trigger: 'axis', formatter: '{b}: {c}%' },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: dates, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', axisLabel: { formatter: '{value}%', fontSize: 11 }, max: 100 },
    series: [{
      type: 'bar',
      data: rates,
      itemStyle: {
        color: (params: any) => {
          const val = Number(params.value)
          return val < 10 ? '#10B981' : val < 30 ? '#F59E0B' : '#EF4444'
        }
      }
    }]
  })
}

function handleResize() {
  recordCountChart?.resize()
  nullRateChart?.resize()
}

function nullRateColor(rate?: number) {
  if (!rate) return '#10B981'
  if (rate < 0.1) return '#10B981'
  if (rate < 0.3) return '#F59E0B'
  return '#EF4444'
}

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  recordCountChart?.dispose()
  nullRateChart?.dispose()
})
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

.selector-card {
  margin-bottom: 16px;
}

.selector-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.metric-card {
  background: white;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  transition: box-shadow var(--transition-normal);
}

.metric-card:hover {
  box-shadow: var(--shadow-md);
}

.metric-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.metric-info {
  flex: 1;
}

.metric-value {
  font-size: 22px;
  font-weight: 700;
  font-family: var(--font-mono);
  line-height: 1;
  margin-bottom: 4px;
}

.metric-label {
  font-size: 12px;
  color: var(--color-text-muted);
}

.chart-card {
  margin-bottom: 16px;
}

.card-title {
  font-weight: 600;
  color: var(--color-text-primary);
}

.chart-container {
  height: 240px;
}

.history-card {
  margin-bottom: 16px;
}

@media (max-width: 1024px) {
  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
