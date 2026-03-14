<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">数据目录</h1>
        <p class="page-desc">组织和管理数据资产的分类目录（最多 5 级）</p>
      </div>
      <PermissionGuard action="create_catalog">
        <el-button type="primary" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>
          新建目录
        </el-button>
      </PermissionGuard>
    </div>

    <el-row :gutter="16">
      <!-- 左侧目录树 -->
      <el-col :span="8">
        <el-card shadow="never" class="tree-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">目录结构</span>
              <el-button link size="small" @click="loadCatalogTree">
                <el-icon><Refresh /></el-icon>
              </el-button>
            </div>
          </template>

          <div v-loading="treeLoading">
            <el-tree
              v-if="catalogTree.length > 0"
              :data="catalogTree"
              :props="treeProps"
              node-key="id"
              highlight-current
              default-expand-all
              @node-click="handleNodeClick"
            >
              <template #default="{ node, data }">
                <div class="tree-node">
                  <el-icon class="node-icon">
                    <FolderOpened v-if="node.expanded" />
                    <Folder v-else />
                  </el-icon>
                  <span class="node-label">{{ data.name }}</span>
                  <span class="node-level">L{{ data.level }}</span>
                  <div class="node-actions" @click.stop>
                    <PermissionGuard action="create_catalog">
                      <el-button
                        v-if="data.level < 5"
                        link
                        size="small"
                        @click="addSubCatalog(data)"
                        title="添加子目录"
                      >
                        <el-icon><Plus /></el-icon>
                      </el-button>
                      <el-button link size="small" @click="editCatalog(data)" title="编辑">
                        <el-icon><Edit /></el-icon>
                      </el-button>
                      <el-popconfirm title="确认删除此目录？" @confirm="handleDeleteCatalog(data.id)">
                        <template #reference>
                          <el-button link type="danger" size="small" title="删除">
                            <el-icon><Delete /></el-icon>
                          </el-button>
                        </template>
                      </el-popconfirm>
                    </PermissionGuard>
                  </div>
                </div>
              </template>
            </el-tree>
            <el-empty v-else description="暂无目录，点击「新建目录」创建" :image-size="60" />
          </div>
        </el-card>
      </el-col>

      <!-- 右侧目录内容 -->
      <el-col :span="16">
        <el-card shadow="never" class="content-card">
          <template #header>
            <div class="card-header">
              <div v-if="selectedCatalog">
                <span class="card-title">{{ selectedCatalog.name }}</span>
                <span class="catalog-path text-muted">{{ selectedCatalog.path }}</span>
              </div>
              <span v-else class="card-title">请选择目录</span>

              <PermissionGuard v-if="selectedCatalog" action="update">
                <el-button size="small" @click="showAddTableDialog = true">
                  <el-icon><Plus /></el-icon>
                  关联表
                </el-button>
              </PermissionGuard>
            </div>
          </template>

          <div v-if="selectedCatalog">
            <!-- 目录信息 -->
            <div class="catalog-info">
              <el-descriptions :column="3" size="small">
                <el-descriptions-item label="层级">L{{ selectedCatalog.level }}</el-descriptions-item>
                <el-descriptions-item label="路径">
                  <span class="font-mono text-secondary">{{ selectedCatalog.path }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ formatDate(selectedCatalog.createdAt) }}</el-descriptions-item>
                <el-descriptions-item label="描述" :span="3">
                  {{ selectedCatalog.description || '暂无描述' }}
                </el-descriptions-item>
              </el-descriptions>
            </div>

            <!-- 关联的表 -->
            <div class="catalog-tables">
              <div class="section-title">关联的表 ({{ catalogTables.length }})</div>
              <el-table :data="catalogTables" size="small" v-loading="tablesLoading">
                <el-table-column prop="databaseName" label="数据库" width="140">
                  <template #default="{ row }">
                    <el-tag size="small" type="info">{{ row.databaseName }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="tableName" label="表名" min-width="160">
                  <template #default="{ row }">
                    <span class="font-mono">{{ row.tableName }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="tableType" label="类型" width="100">
                  <template #default="{ row }">
                    <el-tag size="small">{{ row.tableType }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="80">
                  <template #default="{ row }">
                    <PermissionGuard action="update">
                      <el-popconfirm title="从目录中移除此表？" @confirm="removeTable(row.id)">
                        <template #reference>
                          <el-button link type="danger" size="small">移除</el-button>
                        </template>
                      </el-popconfirm>
                    </PermissionGuard>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>

          <el-empty v-else description="请从左侧选择一个目录" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 创建/编辑目录对话框 -->
    <el-dialog v-model="showCreateDialog" :title="editingCatalog ? '编辑目录' : '新建目录'" width="460px" destroy-on-close>
      <el-form :model="catalogForm" label-width="80px">
        <el-form-item label="目录名称" required>
          <el-input v-model="catalogForm.name" placeholder="请输入目录名称" />
        </el-form-item>
        <el-form-item label="父目录">
          <el-tree-select
            v-model="catalogForm.parentId"
            :data="catalogTree"
            :props="treeProps"
            node-key="id"
            placeholder="选择父目录（不选则为根目录）"
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="catalogForm.description" type="textarea" :rows="3" placeholder="目录描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitCatalog">
          {{ editingCatalog ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 关联表对话框 -->
    <el-dialog v-model="showAddTableDialog" title="关联表到目录" width="500px" destroy-on-close>
      <el-select
        v-model="selectedTableToAdd"
        filterable
        remote
        :remote-method="searchTablesForAdd"
        :loading="tableSearchLoading"
        placeholder="搜索并选择表"
        style="width: 100%"
      >
        <el-option
          v-for="table in tableSearchResults"
          :key="table.id"
          :label="`${table.databaseName}.${table.tableName}`"
          :value="table.id"
        />
      </el-select>
      <template #footer>
        <el-button @click="showAddTableDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleAddTable">关联</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import * as catalogApi from '@/api/catalog'
import * as tableApi from '@/api/tables'
import type { Catalog, CatalogCreateRequest, TableMetadata } from '@/types'
import { ElMessage } from 'element-plus'
import PermissionGuard from '@/components/PermissionGuard.vue'
import dayjs from 'dayjs'

const catalogTree = ref<Catalog[]>([])
const selectedCatalog = ref<Catalog | null>(null)
const catalogTables = ref<TableMetadata[]>([])
const treeLoading = ref(false)
const tablesLoading = ref(false)
const showCreateDialog = ref(false)
const showAddTableDialog = ref(false)
const editingCatalog = ref<Catalog | null>(null)
const submitting = ref(false)
const tableSearchLoading = ref(false)
const tableSearchResults = ref<TableMetadata[]>([])
const selectedTableToAdd = ref<number | null>(null)

const treeProps = {
  label: 'name',
  children: 'children'
}

const catalogForm = reactive<CatalogCreateRequest>({
  name: '',
  description: '',
  parentId: undefined
})

async function loadCatalogTree() {
  treeLoading.value = true
  try {
    const res = await catalogApi.getCatalogTree()
    catalogTree.value = res.data
  } finally {
    treeLoading.value = false
  }
}

async function handleNodeClick(data: Catalog) {
  selectedCatalog.value = data
  tablesLoading.value = true
  try {
    const res = await catalogApi.getTablesInCatalog(data.id)
    catalogTables.value = res.data
  } finally {
    tablesLoading.value = false
  }
}

function addSubCatalog(parent: Catalog) {
  editingCatalog.value = null
  catalogForm.name = ''
  catalogForm.description = ''
  catalogForm.parentId = parent.id
  showCreateDialog.value = true
}

function editCatalog(catalog: Catalog) {
  editingCatalog.value = catalog
  catalogForm.name = catalog.name
  catalogForm.description = catalog.description || ''
  catalogForm.parentId = catalog.parentId
  showCreateDialog.value = true
}

async function handleSubmitCatalog() {
  if (!catalogForm.name) {
    ElMessage.warning('请输入目录名称')
    return
  }
  submitting.value = true
  try {
    if (editingCatalog.value) {
      await catalogApi.updateCatalog(editingCatalog.value.id, catalogForm)
      ElMessage.success('目录更新成功')
    } else {
      await catalogApi.createCatalog(catalogForm)
      ElMessage.success('目录创建成功')
    }
    showCreateDialog.value = false
    editingCatalog.value = null
    await loadCatalogTree()
  } finally {
    submitting.value = false
  }
}

async function handleDeleteCatalog(id: number) {
  await catalogApi.deleteCatalog(id)
  ElMessage.success('目录删除成功')
  if (selectedCatalog.value?.id === id) {
    selectedCatalog.value = null
    catalogTables.value = []
  }
  await loadCatalogTree()
}

async function searchTablesForAdd(query: string) {
  if (!query) return
  tableSearchLoading.value = true
  try {
    const res = await tableApi.listTables({ tableName: query }, { page: 1, pageSize: 20 })
    tableSearchResults.value = res.data.items
  } finally {
    tableSearchLoading.value = false
  }
}

async function handleAddTable() {
  if (!selectedCatalog.value || !selectedTableToAdd.value) {
    ElMessage.warning('请选择要关联的表')
    return
  }
  submitting.value = true
  try {
    await catalogApi.addTableToCatalog(selectedCatalog.value.id, selectedTableToAdd.value)
    ElMessage.success('表关联成功')
    showAddTableDialog.value = false
    selectedTableToAdd.value = null
    await handleNodeClick(selectedCatalog.value)
  } finally {
    submitting.value = false
  }
}

async function removeTable(tableId: number) {
  if (!selectedCatalog.value) return
  await catalogApi.removeTableFromCatalog(selectedCatalog.value.id, tableId)
  ElMessage.success('已从目录移除')
  await handleNodeClick(selectedCatalog.value)
}

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

onMounted(loadCatalogTree)
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

.tree-card, .content-card {
  height: calc(100vh - 200px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-weight: 600;
  color: var(--color-text-primary);
}

.catalog-path {
  font-size: 12px;
  font-family: var(--font-mono);
  margin-left: 8px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding-right: 8px;
}

.node-icon {
  color: #F59E0B;
  font-size: 14px;
}

.node-label {
  flex: 1;
  font-size: 13px;
  color: var(--color-text-primary);
}

.node-level {
  font-size: 11px;
  color: var(--color-text-muted);
  background: #F1F5F9;
  padding: 1px 5px;
  border-radius: 3px;
}

.node-actions {
  display: none;
  align-items: center;
  gap: 2px;
}

.tree-node:hover .node-actions {
  display: flex;
}

.catalog-info {
  margin-bottom: 16px;
  padding: 12px;
  background: #F8FAFC;
  border-radius: var(--radius-md);
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
}
</style>
