import { http } from './http'
import { unwrap, type ApiResult } from './types'
import type { RagCitation } from './chat'

export interface ChatSessionItem {
  id: number
  title: string
  modelId: number | null
  kbIds: number[]
  updatedAt: string
}

export interface ChatMessageItem {
  id: number
  role: 'user' | 'assistant'
  content: string
  citations?: RagCitation[]
  createdAt: string
}

export interface ChatSessionDetail extends ChatSessionItem {
  messages: ChatMessageItem[]
}

export async function listChatSessions() {
  const { data } = await http.get<ApiResult<ChatSessionItem[]>>('/api/auth/chat/sessions')
  return unwrap(data)
}

export async function createChatSession(payload?: {
  title?: string
  modelId?: number
  kbIds?: number[]
}) {
  const { data } = await http.post<ApiResult<ChatSessionItem>>('/api/auth/chat/sessions', payload ?? {})
  return unwrap(data)
}

export async function getChatSession(id: number) {
  const { data } = await http.get<ApiResult<ChatSessionDetail>>(`/api/auth/chat/sessions/${id}`)
  return unwrap(data)
}

export async function updateChatSession(id: number, payload: {
  title?: string
  kbIds?: number[]
  modelId?: number
}) {
  const { data } = await http.put<ApiResult<ChatSessionItem>>(`/api/auth/chat/sessions/${id}`, payload)
  return unwrap(data)
}

export async function deleteChatSession(id: number) {
  const { data } = await http.delete<ApiResult<void>>(`/api/auth/chat/sessions/${id}`)
  return unwrap(data)
}
