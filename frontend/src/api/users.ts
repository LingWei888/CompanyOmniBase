import { http } from './http'
import { unwrap, type ApiResult } from './types'
import type { PageResult } from './page'
import type { UserPlan } from './userAuth'

export interface ManagedAppUser {
  id: number
  username: string
  nickname: string
  avatarUrl?: string | null
  plan: UserPlan
  enabled: boolean
  createdAt?: string
  updatedAt?: string
}

export async function listAppUsers(params?: { keyword?: string; page?: number; size?: number }) {
  const { data } = await http.get<ApiResult<PageResult<ManagedAppUser>>>('/api/admin/users', { params })
  return unwrap(data)
}

export async function createAppUser(payload: {
  username: string
  password: string
  nickname?: string
  plan?: UserPlan
  enabled?: boolean
}) {
  const { data } = await http.post<ApiResult<ManagedAppUser>>('/api/admin/users', payload)
  return unwrap(data)
}

export async function updateAppUser(
  id: number,
  payload: { nickname?: string; plan?: UserPlan; enabled?: boolean },
) {
  const { data } = await http.put<ApiResult<ManagedAppUser>>(`/api/admin/users/${id}`, payload)
  return unwrap(data)
}

export async function resetAppUserPassword(id: number, password: string) {
  const { data } = await http.put<ApiResult<null>>(`/api/admin/users/${id}/password`, { password })
  unwrap(data)
}

export async function deleteAppUser(id: number) {
  const { data } = await http.delete<ApiResult<null>>(`/api/admin/users/${id}`)
  unwrap(data)
}
