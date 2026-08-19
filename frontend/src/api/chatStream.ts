import type { RagAskPayload, RagCitation } from './chat'

export type { RagAskPayload, RagCitation }

export interface StreamMeta {
  kbIds: number[]
  kbNames: string[]
  modelId: number
  modelName: string
  ragEnabled?: boolean
  agentEnabled?: boolean
}

export interface StreamToolEvent {
  name: string
  message: string
}

export interface AskStreamHandlers {
  onMeta?: (meta: StreamMeta) => void
  onCitations?: (citations: RagCitation[]) => void
  onTool?: (tool: StreamToolEvent) => void
  onDelta?: (content: string) => void
  onDone?: (answer: string) => void
  onError?: (message: string) => void
}

/**
 * 解析 SSE 文本流（支持 event: + data:）。
 */
export async function askRagStream(payload: RagAskPayload, handlers: AskStreamHandlers) {
  const userToken = localStorage.getItem('user_access_token')
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'text/event-stream',
  }
  if (userToken) {
    headers.Authorization = `Bearer ${userToken}`
  }

  const response = await fetch('/api/public/chat/ask/stream', {
    method: 'POST',
    headers,
    body: JSON.stringify(payload),
  })

  if (!response.ok) {
    let message = `问答失败（HTTP ${response.status}）`
    try {
      const json = await response.json()
      if (json?.message) message = json.message
    } catch {
      // ignore
    }
    throw new Error(message)
  }

  if (!response.body) {
    throw new Error('浏览器不支持流式响应')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let currentEvent = 'message'
  let sawDone = false

  const fieldValue = (line: string, prefix: string) => {
    const rest = line.slice(prefix.length)
    return rest.startsWith(' ') ? rest.slice(1) : rest
  }

  const flushBlock = (block: string) => {
    const lines = block.split('\n')
    let eventName = currentEvent
    const dataLines: string[] = []
    for (const raw of lines) {
      const line = raw.replace(/\r$/, '')
      if (line.startsWith(':')) continue
      if (line.startsWith('event:')) {
        eventName = fieldValue(line, 'event:') || 'message'
        continue
      }
      if (line.startsWith('data:')) {
        // 按 SSE 规范保留 data: 后的原文；trimStart 会把纯空白 JSON 弄坏的不是这里，
        // 但也不能丢掉空的 data: 行（规范里表示一个换行）。
        dataLines.push(fieldValue(line, 'data:'))
      }
    }
    if (!dataLines.length) return
    const dataText = dataLines.join('\n')
    let data: unknown = dataText
    try {
      data = JSON.parse(dataText)
    } catch {
      // keep string
    }

    if (eventName === 'meta' && data && typeof data === 'object') {
      handlers.onMeta?.(data as StreamMeta)
    } else if (eventName === 'citations' && Array.isArray(data)) {
      handlers.onCitations?.(data as RagCitation[])
    } else if (eventName === 'tool' && data && typeof data === 'object') {
      const raw = data as { name?: string; message?: string }
      handlers.onTool?.({
        name: String(raw.name || ''),
        message: String(raw.message || '正在调用工具…'),
      })
    } else if (eventName === 'delta') {
      let content: string | undefined
      if (typeof data === 'string') {
        try {
          const parsed = JSON.parse(data) as { content?: string }
          if (typeof parsed?.content === 'string') content = parsed.content
        } catch {
          // ignore
        }
      } else if (data && typeof data === 'object') {
        const raw = (data as { content?: string }).content
        if (typeof raw === 'string') content = raw
      }
      // "\n" 在 JS 里是 truthy，但仍用 typeof 判断，避免漏掉空白片段
      if (typeof content === 'string') {
        handlers.onDelta?.(content)
      }
    } else if (eventName === 'done') {
      sawDone = true
      const answer =
        data && typeof data === 'object' && 'answer' in data
          ? String((data as { answer?: string }).answer || '')
          : ''
      handlers.onDone?.(answer)
    } else if (eventName === 'error') {
      const message =
        data && typeof data === 'object' && 'message' in data
          ? String((data as { message?: string }).message || '问答失败')
          : '问答失败'
      handlers.onError?.(message)
      throw new Error(message)
    }
  }

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    buffer = buffer.replace(/\r\n/g, '\n')
    let idx
    while ((idx = buffer.indexOf('\n\n')) >= 0) {
      const block = buffer.slice(0, idx)
      buffer = buffer.slice(idx + 2)
      flushBlock(block)
      currentEvent = 'message'
    }
  }

  if (buffer.trim()) {
    flushBlock(buffer)
  }
  if (!sawDone) {
    handlers.onDone?.('')
  }
}
