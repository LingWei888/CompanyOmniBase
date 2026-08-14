import { http } from './http'
import { unwrap, type ApiResult } from './types'
import type { PageResult } from './page'

export interface KnowledgeBase {
  id: number
  name: string
  description?: string
  enabled: boolean
  createdAt?: string
  updatedAt?: string
}

export interface KnowledgeBasePayload {
  name: string
  description?: string
  enabled?: boolean
}

export async function listKnowledgeBases(page = 1, size = 10) {
  const { data } = await http.get<ApiResult<PageResult<KnowledgeBase>>>('/api/admin/knowledge-bases', {
    params: { page, size },
  })
  return unwrap(data)
}

export async function listKnowledgeBaseOptions() {
  const { data } = await http.get<ApiResult<KnowledgeBase[]>>('/api/admin/knowledge-bases/options')
  return unwrap(data)
}

export async function createKnowledgeBase(payload: KnowledgeBasePayload) {
  const { data } = await http.post<ApiResult<KnowledgeBase>>('/api/admin/knowledge-bases', payload)
  return unwrap(data)
}

export async function updateKnowledgeBase(id: number, payload: KnowledgeBasePayload) {
  const { data } = await http.put<ApiResult<KnowledgeBase>>(`/api/admin/knowledge-bases/${id}`, payload)
  return unwrap(data)
}

export async function deleteKnowledgeBase(id: number) {
  const { data } = await http.delete<ApiResult<null>>(`/api/admin/knowledge-bases/${id}`)
  unwrap(data)
}
