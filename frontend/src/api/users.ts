import client from './client'

export interface UserItem {
  id: number
  username: string
  email: string
  role: 'ADMIN' | 'DEVELOPER' | 'GUEST'
  isActive: boolean
  createdAt: string
  updatedAt: string
  lastLoginAt: string | null
}

export interface CreateUserPayload {
  username: string
  email: string
  password: string
  role: string
}

export interface UpdateUserPayload {
  email?: string
  role?: string
  isActive?: boolean
  newPassword?: string
}

// baseURL 已经是 /api/v1，这里只写相对路径
export const listUsers = (params: { keyword?: string; role?: string; page?: number; pageSize?: number }) =>
  client.get('/users', { params })

export const getUser = (id: number) =>
  client.get(`/users/${id}`)

export const createUser = (data: CreateUserPayload) =>
  client.post('/users', data)

export const updateUser = (id: number, data: UpdateUserPayload) =>
  client.put(`/users/${id}`, data)

export const deleteUser = (id: number) =>
  client.delete(`/users/${id}`)

export const resetPassword = (id: number, newPassword: string) =>
  client.post(`/users/${id}/reset-password`, { newPassword })
