import { http } from './http'

export interface AdminUser {
  id: number
  username: string
  nickname: string
  role: 'ADMIN'
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: AdminUser
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export async function adminLogin(username: string, password: string) {
  const { data } = await http.post<ApiResult<LoginResult>>('/api/admin/auth/login', {
    username,
    password,
  })
  if (data.code !== 0) {
    throw new Error(data.message || '登录失败')
  }
  return data.data
}

export async function adminLogout() {
  await http.post('/api/admin/auth/logout')
}

export async function fetchAdminMe() {
  const { data } = await http.get<ApiResult<AdminUser>>('/api/admin/auth/me')
  if (data.code !== 0) {
    throw new Error(data.message || '获取用户信息失败')
  }
  return data.data
}

export async function fetchDashboardOverview() {
  const { data } = await http.get<ApiResult<Record<string, string>>>('/api/admin/dashboard/overview')
  if (data.code !== 0) {
    throw new Error(data.message || '加载失败')
  }
  return data.data
}
