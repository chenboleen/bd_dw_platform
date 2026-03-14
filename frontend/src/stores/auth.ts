// 认证状态管理
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi, getCurrentUser } from '@/api/auth'
import type { User, LoginRequest, TokenResponse } from '@/types'
import { ElMessage } from 'element-plus'

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const user = ref<User | null>(null)
  const accessToken = ref<string | null>(localStorage.getItem('access_token'))
  const refreshToken = ref<string | null>(localStorage.getItem('refresh_token'))
  const loading = ref(false)

  // 计算属性
  const isAuthenticated = computed(() => !!accessToken.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isDeveloper = computed(() => user.value?.role === 'DEVELOPER' || isAdmin.value)
  const userRole = computed(() => user.value?.role)

  /** 登录 */
  async function login(credentials: LoginRequest): Promise<boolean> {
    loading.value = true
    try {
      const response = await loginApi(credentials)
      const tokenData: TokenResponse = response.data

      // 存储 Token
      accessToken.value = tokenData.accessToken
      refreshToken.value = tokenData.refreshToken
      localStorage.setItem('access_token', tokenData.accessToken)
      localStorage.setItem('refresh_token', tokenData.refreshToken)

      // 获取用户信息
      await fetchCurrentUser()

      ElMessage.success('登录成功')
      return true
    } catch (error: any) {
      const msg = error.response?.data?.errorMessage || '登录失败，请检查用户名和密码'
      ElMessage.error(msg)
      return false
    } finally {
      loading.value = false
    }
  }

  /** 登出 */
  async function logout() {
    try {
      if (accessToken.value) {
        await logoutApi(accessToken.value)
      }
    } catch {
      // 忽略登出错误
    } finally {
      clearAuth()
    }
  }

  /** 获取当前用户信息 */
  async function fetchCurrentUser() {
    try {
      const response = await getCurrentUser()
      user.value = response.data
    } catch {
      clearAuth()
    }
  }

  /** 清除认证信息 */
  function clearAuth() {
    user.value = null
    accessToken.value = null
    refreshToken.value = null
    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
  }

  /** 检查权限 */
  function hasPermission(action: 'read' | 'update' | 'delete' | 'create_catalog'): boolean {
    if (!user.value) return false
    switch (action) {
      case 'read': return true
      case 'update': return isDeveloper.value
      case 'delete': return isAdmin.value
      case 'create_catalog': return isAdmin.value
      default: return false
    }
  }

  return {
    user, accessToken, refreshToken, loading,
    isAuthenticated, isAdmin, isDeveloper, userRole,
    login, logout, fetchCurrentUser, clearAuth, hasPermission
  }
}, {
  persist: {
    paths: ['accessToken', 'refreshToken']
  }
})
