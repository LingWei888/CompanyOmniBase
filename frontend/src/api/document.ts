import { http } from './http'
import { unwrap, type ApiResult } from './types'
import type { PageResult } from './page'

export type DocumentStatus =
  | 'PENDING'
  | 'PARSING'
  | 'CHUNKING'
  | 'WAITING_EMBEDDING'
  | 'EMBEDDING'
  | 'READY'
  | 'FAILED'

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
  chunkSize: number
  chunkOverlap: number
  parsedCharCount: number
  chunkCount: number
  parsedTextAvailable?: boolean
  createdAt?: string
  updatedAt?: string
}

export interface ChunkDefaults {
  chunkSize: number
  chunkOverlap: number
  systemChunkSize?: number
  systemChunkOverlap?: number
  kbChunkSize?: number | null
  kbChunkOverlap?: number | null
}

export interface DocumentParsedText {
  documentId: number
  title: string
  content: string
  charCount: number
}

export interface DocumentChunkItem {
  id: number
  documentId: number
  chunkIndex: number
  charCount: number
  preview: string
}

export interface DocumentChunkDetail {
  id: number
  documentId: number
  documentTitle: string
  chunkIndex: number
  charCount: number
  content: string
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

export async function fetchChunkDefaults(kbId?: number) {
  const { data } = await http.get<ApiResult<ChunkDefaults>>('/api/admin/documents/chunk-defaults', {
    params: kbId != null ? { kbId } : undefined,
  })
  return unwrap(data)
}

export async function fetchParsedText(id: number) {
  const { data } = await http.get<ApiResult<DocumentParsedText>>(`/api/admin/documents/${id}/parsed-text`)
  return unwrap(data)
}

export async function listDocumentChunks(documentId: number, page = 1, size = 10) {
  const { data } = await http.get<ApiResult<PageResult<DocumentChunkItem>>>(
    `/api/admin/documents/${documentId}/chunks`,
    { params: { page, size } },
  )
  return unwrap(data)
}

export async function fetchDocumentChunk(documentId: number, chunkId: number) {
  const { data } = await http.get<ApiResult<DocumentChunkDetail>>(
    `/api/admin/documents/${documentId}/chunks/${chunkId}`,
  )
  return unwrap(data)
}

export async function uploadDocument(
  kbId: number,
  file: File,
  options?: { title?: string; chunkSize?: number; chunkOverlap?: number },
) {
  const form = new FormData()
  form.append('kbId', String(kbId))
  form.append('file', file)
  if (options?.title) form.append('title', options.title)
  if (options?.chunkSize != null) form.append('chunkSize', String(options.chunkSize))
  if (options?.chunkOverlap != null) form.append('chunkOverlap', String(options.chunkOverlap))
  const { data } = await http.post<ApiResult<KbDocument>>('/api/admin/documents/upload', form, {
    timeout: 120000,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return unwrap(data)
}

export async function updateDocument(
  id: number,
  payload: { title: string; chunkSize?: number; chunkOverlap?: number },
) {
  const { data } = await http.put<ApiResult<KbDocument>>(`/api/admin/documents/${id}`, payload)
  return unwrap(data)
}

export async function deleteDocument(id: number) {
  const { data } = await http.delete<ApiResult<null>>(`/api/admin/documents/${id}`)
  unwrap(data)
}

export async function requeueDocument(id: number) {
  const { data } = await http.post<ApiResult<KbDocument>>(`/api/admin/documents/${id}/requeue`)
  return unwrap(data)
}

export async function replaceDocument(
  id: number,
  file: File,
  options?: { title?: string; chunkSize?: number; chunkOverlap?: number },
) {
  const form = new FormData()
  form.append('file', file)
  if (options?.title) form.append('title', options.title)
  if (options?.chunkSize != null) form.append('chunkSize', String(options.chunkSize))
  if (options?.chunkOverlap != null) form.append('chunkOverlap', String(options.chunkOverlap))
  const { data } = await http.post<ApiResult<KbDocument>>(`/api/admin/documents/${id}/replace`, form, {
    timeout: 120000,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return unwrap(data)
}
