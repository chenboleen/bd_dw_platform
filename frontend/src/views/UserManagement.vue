<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">用户管理</h1>
        <p class="page-desc">管理系统用户账号、角色和权限</p>
      </div>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        新建用户
      </el-button>
    </div>

    <!-- 过滤栏 -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <el-input
          v-model="keyword"
          placeholder="搜索用户名或邮箱"
          clearable
          style="width: 220px"
          @keyup.enter="loadUsers"
          @clear="loadUsers"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="filterRole" placeholder="角色" clearable style="width: 130px" @change="loadUsers">
          <el-option label="管理员" value="ADMIN" />
          <el-option label="开发者" value="DEVELOPER" />
          <el-option label="访客" value="GUEST" />
        </el-select>
        <el-button @click="resetFilter">
          <el-icon><Refresh /></el-icon>
          重置
        </el-button>
      </div>
    </el-card>

    <!-- 用户表格 -->
    <el-card shadow="never">
      <el-table :data="userList" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />

        <el-table-column label="用户名" width="140">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="28" :style="{ backgroundColor: avatarColor(row.username) }">
                {{ row.username.charAt(0).toUpperCase() }}
              </el-avatar>
              <span>{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="email" label="邮箱" min-width="180" />

        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" size="small">{{ roleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'danger'" size="small">
              {{ row.isActive ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="最后登录" width="160">
          <template #default="{ row }">
            <span class="text-muted">{{ row.lastLoginAt ? formatDate(row.lastLoginAt) : '从未登录' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            <span class="text-muted">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="openResetPwdDialog(row)">重置密码</el-button>
            <el-button
              link
              :type="row.isActive ? 'danger' : 'success'"
              size="small"
              @click="toggleActive(row)"
            >
              {{ row.isActive ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination :total="total" :page="currentPage" :page-size="pageSize" @change="handlePageChange" />
    </el-card>

    <!-- 新建/编辑对话框 -->
    <el-dialog
      v-model="showFormDialog"
      :title="editingUser ? '编辑用户' : '新建用户'"
      width="480px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="用户名" prop="username" v-if="!editingUser">
          <el-input v-model="form.username" placeholder="3-50个字符" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!editingUser">
          <el-input v-model="form.password" type="password" placeholder="至少6位" show-password />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="开发者" value="DEVELOPER" />
            <el-option label="访客" value="GUEST" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" v-if="editingUser">
          <el-switch v-model="form.isActive" active-text="正常" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showFormDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog v-model="showResetPwdDialog" title="重置密码" width="400px">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" placeholder="至少6位" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="再次输入密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showResetPwdDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitResetPwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import * as usersApi from '@/api/users'
import type { UserItem } from '@/api/users'
import Pagination from '@/components/Pagination.vue'
import dayjs from 'dayjs'

const loading = ref(false)
const submitting = ref(false)
const userList = ref<UserItem[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const filterRole = ref('')

// 表单
const showFormDialog = ref(false)
const editingUser = ref<UserItem | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({ username: '', email: '', password: '', role: 'GUEST', isActive: true })

const formRules: FormRules = {
  username: [{ required: true, min: 3, max: 50, message: '用户名3-50个字符', trigger: 'blur' }],
  email: [{ required: true, type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '密码至少6位', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

// 重置密码表单
const showResetPwdDialog = ref(false)
const resetTargetUser = ref<UserItem | null>(null)
const pwdFormRef = ref<FormInstance>()
const pwdForm = reactive({ newPassword: '', confirmPassword: '' })
const pwdRules: FormRules = {
  newPassword: [{ required: true, min: 6, message: '密码至少6位', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: Function) => {
        if (value !== pwdForm.newPassword) callback(new Error('两次密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

async function loadUsers() {
  loading.value = true
  try {
    const res = await usersApi.listUsers({
      keyword: keyword.value || undefined,
      role: filterRole.value || undefined,
      page: currentPage.value,
      pageSize: pageSize.value
    })
    const d = res.data?.data
    userList.value = d?.items || []
    total.value = d?.total ?? 0
  } catch {
    userList.value = []
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  keyword.value = ''
  filterRole.value = ''
  currentPage.value = 1
  loadUsers()
}

function handlePageChange(page: number, size: number) {
  currentPage.value = page
  pageSize.value = size
  loadUsers()
}

function openCreateDialog() {
  editingUser.value = null
  Object.assign(form, { username: '', email: '', password: '', role: 'GUEST', isActive: true })
  showFormDialog.value = true
}

function openEditDialog(user: UserItem) {
  editingUser.value = user
  Object.assign(form, { username: user.username, email: user.email, password: '', role: user.role, isActive: user.isActive })
  showFormDialog.value = true
}

function resetForm() {
  formRef.value?.resetFields()
  editingUser.value = null
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (editingUser.value) {
      await usersApi.updateUser(editingUser.value.id, {
        email: form.email,
        role: form.role,
        isActive: form.isActive
      })
      ElMessage.success('用户更新成功')
    } else {
      await usersApi.createUser({ username: form.username, email: form.email, password: form.password, role: form.role })
      ElMessage.success('用户创建成功')
    }
    showFormDialog.value = false
    loadUsers()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

function openResetPwdDialog(user: UserItem) {
  resetTargetUser.value = user
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  showResetPwdDialog.value = true
}

async function submitResetPwd() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await usersApi.resetPassword(resetTargetUser.value!.id, pwdForm.newPassword)
    ElMessage.success('密码重置成功')
    showResetPwdDialog.value = false
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '重置失败')
  } finally {
    submitting.value = false
  }
}

async function toggleActive(user: UserItem) {
  const action = user.isActive ? '停用' : '启用'
  await ElMessageBox.confirm(`确定要${action}用户 "${user.username}" 吗？`, '确认操作', { type: 'warning' })
  try {
    if (user.isActive) {
      await usersApi.deleteUser(user.id)
    } else {
      await usersApi.updateUser(user.id, { isActive: true })
    }
    ElMessage.success(`${action}成功`)
    loadUsers()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

function roleLabel(role: string) {
  return { ADMIN: '管理员', DEVELOPER: '开发者', GUEST: '访客' }[role] || role
}

function roleTagType(role: string) {
  return { ADMIN: 'danger', DEVELOPER: 'warning', GUEST: 'info' }[role] || ''
}

function avatarColor(name: string) {
  const colors = ['#1E40AF', '#065F46', '#92400E', '#7C3AED', '#B91C1C']
  return colors[name.charCodeAt(0) % colors.length]
}

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

onMounted(loadUsers)
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

.filter-card {
  margin-bottom: 12px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.text-muted {
  color: var(--color-text-muted);
  font-size: 12px;
}
</style>
