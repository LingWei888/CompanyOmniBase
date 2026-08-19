import { http } from './http'
import { unwrap, type ApiResult } from './types'
import type { DocumentStatus } from './document'

export interface IngestQueueItem {
  id: number
  kbId: number
  title: string
  status: DocumentStatus
  chunkCount: number | null
  parsedCharCount: number | null
  errorMessage: string | null
  updatedAt: string
}

export interface IngestOpsOverview {
  documentStatusCounts: Record<string, number>
  waitingEmbeddingCount: number
  embeddingCount: number
  failedCount: number
  readyCount: number
  waitingEmbedding: IngestQueueItem[]
  embedding: IngestQueueItem[]
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export async function fetchIngestOpsOverview() {
  const { data } = await http.get<ApiResult<IngestOpsOverview>>('/api/admin/ingest/ops/overview')
  return unwrap(data)
}

export async function fetchWaitingEmbeddingQueue(page = 1, size = 20) {
  const { data } = await http.get<ApiResult<PageResult<IngestQueueItem>>>(
    '/api/admin/ingest/embedding/waiting',
    { params: { page, size } },
  )
  return unwrap(data)
}

export async function fetchRunningEmbeddingQueue(page = 1, size = 20) {
  const { data } = await http.get<ApiResult<PageResult<IngestQueueItem>>>(
    '/api/admin/ingest/embedding/running',
    { params: { page, size } },
  )
  return unwrap(data)
}

export async function startEmbedding(documentId: number) {
  const { data } = await http.post<ApiResult<unknown>>(`/api/admin/ingest/embedding/start/${documentId}`)
  return unwrap(data)
}

export async function startEmbeddingBatch(documentIds: number[]) {
  const { data } = await http.post<ApiResult<unknown>>('/api/admin/ingest/embedding/start-batch', {
    documentIds,
  })
  return unwrap(data)
}
