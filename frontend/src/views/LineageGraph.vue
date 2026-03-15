<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">血缘关系图谱</h1>
        <p class="page-desc">可视化数据流转和依赖关系</p>
      </div>
      <div class="header-actions">
        <PermissionGuard action="update">
          <el-button type="primary" @click="showCreateDialog = true">
            <el-icon><Plus /></el-icon>
            添加血缘
          </el-button>
        </PermissionGuard>
      </div>
    </div>

    <!-- 查询控制面板 -->
    <el-card shadow="never" class="control-panel">
      <div class="control-row">
        <div class="search-group">
          <el-select
            v-model="filterDatabase"
            placeholder="所属库（可选）"
            clearable
            style="width: 160px"
            @change="handleDatabaseFilter"
          >
            <el-option
              v-for="db in databaseOptions"
              :key="db"
              :label="db"
              :value="db"
            />
          </el-select>

          <el-select
            v-model="selectedTableId"
            placeholder="搜索表名（模糊匹配）"
            filterable
            remote
            :remote-method="searchTables"
            :loading="searchLoading"
            style="width: 280px"
            @change="loadGraph"
          >
            <el-option
              v-for="table in tableOptions"
              :key="table.id"
              :label="`${table.databaseName}.${table.tableName}`"
              :value="table.id"
            />
          </el-select>
        </div>

        <el-radio-group v-model="direction" @change="loadGraph">
          <el-radio-button value="upstream">上游</el-radio-button>
          <el-radio-button value="both">双向</el-radio-button>
          <el-radio-button value="downstream">下游</el-radio-button>
        </el-radio-group>

        <div class="depth-control">
          <span class="depth-label">深度:</span>
          <el-slider
            v-model="depth"
            :min="1"
            :max="5"
            :step="1"
            show-stops
            style="width: 120px"
            @change="loadGraph"
          />
          <span class="depth-value">{{ depth }}</span>
        </div>

        <el-button @click="resetGraph">
          <el-icon><Refresh /></el-icon>
          重置
        </el-button>
      </div>
    </el-card>

    <!-- 图谱区域 -->
    <el-card shadow="never" class="graph-card">
      <div v-if="!selectedTableId" class="empty-state">
        <el-empty description="请选择一个表来查看血缘关系图谱">
          <template #image>
            <svg width="80" height="80" viewBox="0 0 80 80" fill="none" aria-hidden="true">
              <circle cx="40" cy="20" r="12" fill="#DBEAFE" stroke="#3B82F6" stroke-width="2"/>
              <circle cx="15" cy="60" r="10" fill="#DBEAFE" stroke="#3B82F6" stroke-width="2"/>
              <circle cx="65" cy="60" r="10" fill="#DBEAFE" stroke="#3B82F6" stroke-width="2"/>
              <line x1="40" y1="32" x2="20" y2="50" stroke="#94A3B8" stroke-width="2" stroke-dasharray="4"/>
              <line x1="40" y1="32" x2="60" y2="50" stroke="#94A3B8" stroke-width="2" stroke-dasharray="4"/>
            </svg>
          </template>
        </el-empty>
      </div>

      <div v-else-if="graphLoading" class="loading-state">
        <el-skeleton :rows="6" animated />
      </div>

      <div v-else-if="graphData" class="graph-wrapper">
        <!-- 图谱统计 -->
        <div class="graph-stats">
          <span class="stat-item">
            <el-icon><Grid /></el-icon>
            {{ graphData.nodes.length }} 个节点
          </span>
          <span class="stat-item">
            <el-icon><Share /></el-icon>
            {{ graphData.edges.length }} 条边
          </span>
        </div>

        <!-- ECharts 图谱 -->
        <div ref="chartRef" class="echarts-container" role="img" :aria-label="`血缘关系图谱，包含 ${graphData.nodes.length} 个节点`"></div>
      </div>
    </el-card>

    <!-- 影响分析 -->
    <el-card v-if="selectedTableId" shadow="never" class="impact-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">影响分析</span>
          <el-button size="small" @click="loadImpact" :loading="impactLoading">
            <el-icon><DataAnalysis /></el-icon>
            分析影响范围
          </el-button>
        </div>
      </template>

      <div v-if="impactReport">
        <div class="impact-summary">
          <div class="impact-stat">
            <div class="impact-value">{{ impactReport.totalCount }}</div>
            <div class="impact-label">受影响表数</div>
          </div>
          <div class="impact-stat">
            <div class="impact-value">{{ impactReport.maxDepth }}</div>
            <div class="impact-label">最大影响深度</div>
          </div>
        </div>

        <el-table :data="impactReport.affectedTables" size="small" max-height="300">
          <el-table-column prop="databaseName" label="数据库" width="140" />
          <el-table-column prop="tableName" label="表名">
            <template #default="{ row }">
              <span class="font-mono">{{ row.tableName }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="tableType" label="类型" width="100">
            <template #default="{ row }">
              <el-tag size="small">{{ row.tableType }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <el-empty v-else description="点击「分析影响范围」查看结果" :image-size="60" />
    </el-card>

    <!-- SQL 解析血缘 -->
    <el-card shadow="never" class="sql-card">
      <template #header>
        <span class="card-title">SQL 解析血缘</span>
      </template>
      <div class="sql-input-area">
        <el-input
          v-model="sqlText"
          type="textarea"
          :rows="5"
          placeholder="输入 SQL 语句，自动提取血缘关系..."
          class="font-mono"
        />
        <el-button type="primary" :loading="sqlParsing" @click="parseSql" style="margin-top: 8px">
          <el-icon><MagicStick /></el-icon>
          解析血缘
        </el-button>
      </div>
      <div v-if="parsedLineages.length > 0" class="parsed-result">
        <el-divider>解析结果</el-divider>
        <el-table :data="parsedLineages" size="small">
          <el-table-column prop="sourceTableId" label="源表 ID" width="100" />
          <el-table-column prop="targetTableId" label="目标表 ID" width="100" />
          <el-table-column prop="lineageType" label="类型" width="100">
            <template #default="{ row }">
              <el-tag size="small">{{ row.lineageType }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- 添加血缘对话框 -->
    <el-dialog v-model="showCreateDialog" title="添加血缘关系" width="500px" destroy-on-close>
      <el-form :model="lineageForm" label-width="90px">
        <el-form-item label="源表" required>
          <el-select
            v-model="lineageForm.sourceTableId"
            filterable
            remote
            :remote-method="searchTables"
            placeholder="选择源表"
            style="width: 100%"
          >
            <el-option
              v-for="table in tableOptions"
              :key="table.id"
              :label="`${table.databaseName}.${table.tableName}`"
              :value="table.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="目标表" required>
          <el-select
            v-model="lineageForm.targetTableId"
            filterable
            remote
            :remote-method="searchTables"
            placeholder="选择目标表"
            style="width: 100%"
          >
            <el-option
              v-for="table in tableOptions"
              :key="table.id"
              :label="`${table.databaseName}.${table.tableName}`"
              :value="table.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="血缘类型">
          <el-radio-group v-model="lineageForm.lineageType">
            <el-radio value="DIRECT">直接血缘</el-radio>
            <el-radio value="INDIRECT">间接血缘</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="转换逻辑">
          <el-input v-model="lineageForm.transformationLogic" type="textarea" :rows="3" placeholder="描述数据转换逻辑..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreateLineage">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import * as lineageApi from '@/api/lineage'
import * as tableApi from '@/api/tables'
import * as searchApi from '@/api/search'
import type { LineageGraph, ImpactReport, LineageCreateRequest, TableMetadata } from '@/types'
import { ElMessage } from 'element-plus'
import PermissionGuard from '@/components/PermissionGuard.vue'

const route = useRoute()

const selectedTableId = ref<number | null>(null)
const direction = ref('both')
const depth = ref(3)
const graphData = ref<LineageGraph | null>(null)
const graphLoading = ref(false)
const searchLoading = ref(false)
const tableOptions = ref<TableMetadata[]>([])
const databaseOptions = ref<string[]>([])
const filterDatabase = ref<string>('')
const impactReport = ref<ImpactReport | null>(null)
const impactLoading = ref(false)
const sqlText = ref('')
const sqlParsing = ref(false)
const parsedLineages = ref<any[]>([])
const showCreateDialog = ref(false)
const submitting = ref(false)
const chartRef = ref<HTMLElement | null>(null)
let chartInstance: echarts.ECharts | null = null

const lineageForm = reactive<LineageCreateRequest>({
  sourceTableId: 0,
  targetTableId: 0,
  lineageType: 'DIRECT',
  transformationLogic: ''
})

// 从路由参数初始化
onMounted(async () => {
  loadDatabases()
  if (route.query.tableId) {
    const tableId = Number(route.query.tableId)
    selectedTableId.value = tableId
    // 预加载该表信息到 tableOptions，确保搜索框显示表名而非 ID
    try {
      const res = await tableApi.getTableById(tableId)
      const t = res.data?.data ?? res.data
      if (t) tableOptions.value = [t]
    } catch { /* 忽略 */ }
    loadGraph()
  }
})

async function loadDatabases() {
  try {
    const res = await tableApi.getDatabases()
    databaseOptions.value = res.data?.data || res.data || []
  } catch {
    // 加载失败不影响主功能
  }
}

async function searchTables(query: string) {
  if (!query) return
  searchLoading.value = true
  try {
    const filter: import('@/types').TableFilter = { tableName: query }
    if (filterDatabase.value) {
      filter.databaseName = filterDatabase.value
    }
    const res = await tableApi.listTables(filter, { page: 1, pageSize: 20 })
    tableOptions.value = res.data?.data?.items ?? res.data?.items ?? []
  } finally {
    searchLoading.value = false
  }
}

function handleDatabaseFilter() {
  // 切换数据库筛选时清空已选表和搜索结果
  selectedTableId.value = null
  tableOptions.value = []
  graphData.value = null
}

async function loadGraph() {
  if (!selectedTableId.value) return
  graphLoading.value = true
  graphData.value = null
  try {
    const res = await lineageApi.getLineageGraph(selectedTableId.value, direction.value, depth.value)
    // 后端响应结构：{ data: { nodes, edges }, success, message }
    const payload = res.data?.data ?? res.data
    graphData.value = (payload?.nodes && payload?.edges) ? payload : null
    if (!graphData.value) {
      ElMessage.warning('暂无血缘数据')
      return
    }
    // 等待 DOM 渲染完成后再初始化图表
    await nextTick()
    // 额外等待一帧，确保容器完成布局
    setTimeout(renderChart, 50)
  } catch {
    ElMessage.error('加载血缘图谱失败')
  } finally {
    graphLoading.value = false
  }
}

async function loadImpact() {
  if (!selectedTableId.value) return
  impactLoading.value = true
  try {
    const res = await lineageApi.analyzeImpact(selectedTableId.value)
    impactReport.value = res.data?.data || res.data
  } finally {
    impactLoading.value = false
  }
}

async function parseSql() {
  if (!sqlText.value.trim()) {
    ElMessage.warning('请输入 SQL 语句')
    return
  }
  sqlParsing.value = true
  try {
    const res = await lineageApi.parseSqlLineage(sqlText.value)
    parsedLineages.value = res.data?.data || res.data || []
    const lineagesData = res.data?.data || res.data || []
    ElMessage.success(`解析完成，发现 ${lineagesData.length} 条血缘关系`)
  } finally {
    sqlParsing.value = false
  }
}

async function handleCreateLineage() {
  if (!lineageForm.sourceTableId || !lineageForm.targetTableId) {
    ElMessage.warning('请选择源表和目标表')
    return
  }
  submitting.value = true
  try {
    await lineageApi.createLineage(lineageForm)
    ElMessage.success('血缘关系创建成功')
    showCreateDialog.value = false
    if (selectedTableId.value) loadGraph()
  } finally {
    submitting.value = false
  }
}

function resetGraph() {
  selectedTableId.value = null
  filterDatabase.value = ''
  graphData.value = null
  impactReport.value = null
  tableOptions.value = []
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
}

function renderChart() {
  if (!chartRef.value || !graphData.value) return

  // 确保容器有实际尺寸
  const container = chartRef.value
  if (container.clientWidth === 0 || container.clientHeight === 0) {
    // 容器还没有尺寸，再等一帧
    setTimeout(renderChart, 100)
    return
  }

  if (chartInstance) {
    chartInstance.dispose()
  }
  chartInstance = echarts.init(container)

  const { nodes, edges } = graphData.value

  // 找到中心节点
  const centerNodeId = selectedTableId.value

  // 颜色定义：中心表=橙色，上游表=绿色，下游表=蓝色
  const COLOR_CENTER = '#F59E0B'
  const COLOR_UPSTREAM = '#10B981'
  const COLOR_DOWNSTREAM = '#3B82F6'

  // 通过边的 type 字段判断节点角色
  // upstream 边：source=上游节点, target=当前表（上游节点指向中心）
  // downstream 边：source=当前表, target=下游节点（中心指向下游节点）
  const upstreamNodeIds = new Set<number>()
  const downstreamNodeIds = new Set<number>()
  edges.forEach((edge: any) => {
    if (edge.type === 'upstream') {
      upstreamNodeIds.add(Number(edge.source))
    } else if (edge.type === 'downstream') {
      downstreamNodeIds.add(Number(edge.target))
    }
  })

  const getNodeCategory = (node: any) => {
    if (node.id === centerNodeId) return 0  // 中心表
    if (upstreamNodeIds.has(node.id)) return 1  // 上游表
    return 2  // 下游表
  }

  const getNodeColor = (node: any) => {
    if (node.id === centerNodeId) return COLOR_CENTER
    if (upstreamNodeIds.has(node.id)) return COLOR_UPSTREAM
    return COLOR_DOWNSTREAM
  }

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        if (params.dataType === 'node') {
          return `<div style="font-family: 'Fira Code', monospace; padding: 4px 0">
            <strong>${params.data.fullName}</strong><br/>
            数据库: ${params.data.databaseName || '-'}<br/>
            深度: ${params.data.depth ?? 0}
          </div>`
        }
        return `${params.data.source} → ${params.data.target}`
      }
    },
    legend: {
      data: ['中心表', '上游表', '下游表'],
      top: 10,
      textStyle: { color: '#475569', fontSize: 12 }
    },
    series: [
      {
        type: 'graph',
        layout: 'force',
        data: nodes.map(node => ({
          id: String(node.id),
          // name 用于 ECharts 内部标识，设为 id 字符串避免重名冲突
          name: String(node.id),
          // 自定义字段，用于 tooltip 和 label
          fullName: node.name || `${node.databaseName}.${node.tableName}`,
          databaseName: node.databaseName,
          depth: node.depth ?? 0,
          symbolSize: node.id === centerNodeId ? 52 : 36,
          itemStyle: {
            color: getNodeColor(node),
            borderColor: '#fff',
            borderWidth: 2,
            shadowBlur: node.id === centerNodeId ? 12 : 0,
            shadowColor: 'rgba(245, 158, 11, 0.5)'
          },
          label: {
            show: true,
            position: 'bottom',
            fontSize: 11,
            color: '#1E3A8A',
            fontFamily: "'Fira Code', monospace",
            // 显示表全名（db.table）
            formatter: () => node.name || `${node.databaseName}.${node.tableName}`
          },
          category: getNodeCategory(node)
        })),
        links: edges.map(edge => ({
          source: String(edge.source),
          target: String(edge.target),
          lineStyle: {
            color: '#94A3B8',
            width: 1.5,
            curveness: 0.1
          },
          symbol: ['none', 'arrow'],
          symbolSize: [0, 8]
        })),
        categories: [
          { name: '中心表', itemStyle: { color: COLOR_CENTER } },
          { name: '上游表', itemStyle: { color: COLOR_UPSTREAM } },
          { name: '下游表', itemStyle: { color: COLOR_DOWNSTREAM } }
        ],
        roam: true,
        draggable: true,
        force: {
          repulsion: 220,
          gravity: 0.1,
          edgeLength: 130,
          layoutAnimation: true
        },
        emphasis: {
          focus: 'adjacency',
          lineStyle: { width: 3 }
        }
      }
    ]
  }

  chartInstance.setOption(option)

  // 点击节点跳转（name 字段存的是 id 字符串）
  chartInstance.on('click', async (params: any) => {
    if (params.dataType === 'node') {
      const newId = Number(params.data.id)
      selectedTableId.value = newId
      // 更新搜索框显示：确保 tableOptions 包含该节点对应的表信息
      const existing = tableOptions.value.find(t => t.id === newId)
      if (!existing) {
        try {
          const res = await tableApi.getTableById(newId)
          const t = res.data?.data ?? res.data
          if (t) tableOptions.value = [t, ...tableOptions.value]
        } catch { /* 忽略 */ }
      }
      loadGraph()
    }
  })
}

// 窗口大小变化时重绘
function handleResize() {
  chartInstance?.resize()
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
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

.header-actions {
  display: flex;
  gap: 8px;
}

.control-panel {
  margin-bottom: 16px;
}

.control-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.search-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.depth-control {
  display: flex;
  align-items: center;
  gap: 8px;
}

.depth-label {
  font-size: 13px;
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.depth-value {
  font-family: var(--font-mono);
  font-weight: 600;
  color: var(--color-primary);
  min-width: 16px;
}

.graph-card {
  margin-bottom: 16px;
}

.empty-state {
  padding: 40px;
  display: flex;
  justify-content: center;
}

.loading-state {
  padding: 24px;
}

.graph-wrapper {
  position: relative;
}

.graph-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #F8FAFC;
  border-radius: var(--radius-sm);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.echarts-container {
  width: 100%;
  height: 500px;
}

.impact-card, .sql-card {
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

.impact-summary {
  display: flex;
  gap: 32px;
  margin-bottom: 16px;
  padding: 16px;
  background: #EFF6FF;
  border-radius: var(--radius-md);
}

.impact-stat {
  text-align: center;
}

.impact-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--color-primary);
  font-family: var(--font-mono);
}

.impact-label {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 4px;
}

.sql-input-area {
  display: flex;
  flex-direction: column;
}

.parsed-result {
  margin-top: 8px;
}
</style>
