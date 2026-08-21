import { http } from './http'
import { unwrap, type ApiResult } from './types'

export interface UserMemoryItem {
  id: number
  content: string
  category: string | null
  importance: number
  createdAt: string
  updatedAt: string
  lastUsedAt: string | null
}

export async function listUserMemories() {
  const { data } = await http.get<ApiResult<UserMemoryItem[]>>('/api/auth/memory')
  return unwrap(data)
}

export async function deleteUserMemory(id: number) {
  const { data } = await http.delete<ApiResult<void>>(`/api/auth/memory/${id}`)
  return unwrap(data)
}

export async function clearUserMemories() {
  const { data } = await http.delete<ApiResult<void>>('/api/auth/memory')
  return unwrap(data)
}
