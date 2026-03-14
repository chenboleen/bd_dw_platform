// 认证相关 API
import apiClient from './client'
import type { LoginRequest, TokenResponse, User, RefreshTokenRequest } from '@/types'

/** 用户登录 */
export const login = (data: LoginRequest) =>
  apiClient.post<TokenResponse>('/auth/login', data)

/** 用户登出 */
export const logout = (token: string) =>
  apiClient.post('/auth/logout', null, {
    headers: { Authorization: `Bearer ${token}` }
  })

/** 刷新 Token */
export const refreshToken = (data: RefreshTokenRequest) =>
  apiClient.post<TokenResponse>('/auth/refresh', data)

/** 获取当前用户信息 */
export const getCurrentUser = () =>
  apiClient.get<User>('/auth/me')
