// HTTP 客户端配置 - 基于 axios 封装
import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse, AxiosError } from 'axios'
import { ElMessage } from 'element-plus'

const client: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

// 请求拦截器 - 附加 JWT Token
client.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('accessToken') || localStorage.getItem('access_token')
    if (token) {
      config.headers.Authorization = 'Bearer ' + token
    }
    return config
  },
  (error: AxiosError) => Promise.reject(error)
)

// 响应拦截器 - 统一错误处理
client.interceptors.response.use(
  (response: AxiosResponse) => response,
  (error: AxiosError<{ errorMessage?: string; message?: string }>) => {
    if (error.response) {
      const { status, data } = error.response
      if (status === 401) {
        localStorage.removeItem('accessToken')
        localStorage.removeItem('access_token')
        window.location.href = '/login'
      } else if (status === 403) {
        ElMessage.error('权限不足，无法执行此操作')
      } else if (status === 404) {
        ElMessage.error('请求的资源不存在')
      } else if (status === 500) {
        ElMessage.error('服务器内部错误，请稍后重试')
      } else {
        ElMessage.error(data?.errorMessage || data?.message || '请求失败')
      }
    } else if (error.request) {
      ElMessage.error('网络连接失败，请检查网络设置')
    }
    return Promise.reject(error)
  }
)

export default client