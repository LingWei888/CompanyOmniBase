import { http } from './http'
import { unwrap, type ApiResult } from './types'
import type { PageResult } from './page'

export type DocumentStatus = 'PENDING' | 'PROCESSING' | 'READY' | 'FAILED'

export interface KbDocument {
  id: number
  kbId: number
  title: string
  originalFilename: string
  objectKey: string
  contentType?: string
  fileSize: number
  status: DocumentStatus
  errorMessage?: string
  createdAt?: string
  updatedAt?: string
}

export async function listDocuments(params?: {
  kbId?: number
  status?: DocumentStatus
  page?: number
  size?: number
}) {
  const { data } = await http.get<ApiResult<PageResult<KbDocument>>>('/api/admin/documents', { params })
  return unwrap(data)
}

export async function uploadDocument(kbId: number, file: File, title?: string) {
  const form = new FormData()
  form.append('kbId', String(kbId))
  form.append('file', file)
  if (title) form.append('title', title)
  const { data } = await http.post<ApiResult<KbDocument>>('/api/admin/documents/upload', form, {
    timeout: 120000,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return unwrap(data)
}

export async function updateDocument(id: number, title: string) {
  const { data } = await http.put<ApiResult<KbDocument>>(`/api/admin/documents/${id}`, { title })
  return unwrap(data)
}

export async function deleteDocument(id: number) {
  const { data } = await http.delete<ApiResult<null>>(`/api/admin/documents/${id}`)
  unwrap(data)
}
