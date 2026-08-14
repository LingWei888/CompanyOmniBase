import { http } from './http'
import { unwrap, type ApiResult } from './types'
import type { PageResult } from './page'

export type LlmProtocol = 'OPENAI'

export interface LlmModel {
  id: number
  name: string
  protocol: LlmProtocol
  baseUrl: string
  apiKey: string
  modelName?: string
  enabled: boolean
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export interface LlmModelPayload {
  name: string
  protocol: LlmProtocol
  baseUrl: string
  apiKey: string
  modelName?: string
  enabled?: boolean
  remark?: string
}

export async function listModels(page = 1, size = 10) {
  const { data } = await http.get<ApiResult<PageResult<LlmModel>>>('/api/admin/models', {
    params: { page, size },
  })
  return unwrap(data)
}

export async function createModel(payload: LlmModelPayload) {
  const { data } = await http.post<ApiResult<LlmModel>>('/api/admin/models', payload)
  return unwrap(data)
}

export async function updateModel(id: number, payload: LlmModelPayload) {
  const { data } = await http.put<ApiResult<LlmModel>>(`/api/admin/models/${id}`, payload)
  return unwrap(data)
}

export async function deleteModel(id: number) {
  const { data } = await http.delete<ApiResult<null>>(`/api/admin/models/${id}`)
  unwrap(data)
}
