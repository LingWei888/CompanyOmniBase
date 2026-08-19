import { http } from './http'
import { unwrap, type ApiResult } from './types'

export interface RagCitation {
  rank: number
  chunkId: number | null
  documentId: number | null
  documentTitle: string
  chunkIndex: number | null
  content: string
  score: number
}

export interface RagAskPayload {
  /** 空数组 = 不启用 RAG（纯对话）；默认前端会全选 */
  kbIds?: number[]
  /** @deprecated 兼容旧单库字段 */
  kbId?: number
  modelId: number
  question: string
  topK?: number
}

export interface RagAskResult {
  answer: string
  kbIds?: number[]
  kbNames?: string[]
  kbId?: number
  kbName?: string
  modelId: number
  modelName: string
  citations: RagCitation[]
}

export async function askRag(payload: RagAskPayload) {
  try {
    const { data } = await http.post<ApiResult<RagAskResult>>('/api/public/chat/ask', payload, {
      timeout: 120000,
    })
    return unwrap(data)
  } catch (error: unknown) {
    if (error instanceof Error && error.message && !('response' in error)) {
      throw error
    }
    const axiosError = error as { response?: { data?: { message?: string } }; message?: string }
    const message = axiosError.response?.data?.message || axiosError.message || '问答失败'
    throw new Error(message)
  }
}
