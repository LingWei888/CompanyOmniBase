import { http } from './http'
import { unwrap, type ApiResult } from './types'

export type UserPlan = 'FREE'
export type UserRole = 'USER'

export interface AppUser {
  id: number
  username: string
  nickname: string
  avatarUrl?: string | null
  role: UserRole
  plan: UserPlan
}

export interface UserAuthResult {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: AppUser
}

export async function userLogin(username: string, password: string) {
  const { data } = await http.post<ApiResult<UserAuthResult>>('/api/auth/login', { username, password })
  return unwrap(data)
}

export async function userRegister(payload: { username: string; password: string; nickname?: string }) {
  const { data } = await http.post<ApiResult<UserAuthResult>>('/api/auth/register', payload)
  return unwrap(data)
}

export async function userLogout() {
  const { data } = await http.post<ApiResult<null>>('/api/auth/logout')
  unwrap(data)
}

export async function fetchUserMe() {
  const { data } = await http.get<ApiResult<AppUser>>('/api/auth/me')
  return unwrap(data)
}

export async function updateUserProfile(payload: { nickname?: string; avatarUrl?: string }) {
  const { data } = await http.put<ApiResult<AppUser>>('/api/auth/profile', payload)
  return unwrap(data)
}

export async function uploadUserAvatar(file: File) {
  const form = new FormData()
  form.append('file', file)
  const { data } = await http.post<ApiResult<AppUser>>('/api/auth/avatar', form, {
    timeout: 60000,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return unwrap(data)
}

export async function changeUserPassword(payload: { oldPassword: string; newPassword: string }) {
  const { data } = await http.put<ApiResult<null>>('/api/auth/password', payload)
  unwrap(data)
}
