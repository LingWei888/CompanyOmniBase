export interface TestdataGenPayload {
  modelId: number
  originalText: string
  solutionCode?: string
}

export interface TestdataGenStreamHandlers {
  onDelta?: (content: string) => void
  onDone?: (python: string) => void | Promise<void>
  onError?: (message: string) => void
}

export async function generateTestdataScriptStream(
  payload: TestdataGenPayload,
  handlers: TestdataGenStreamHandlers,
) {
  const userToken = localStorage.getItem('user_access_token')
  if (!userToken) {
    throw new Error('请先登录后再使用数据生成智能体')
  }
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'text/event-stream',
    Authorization: `Bearer ${userToken}`,
  }

  const response = await fetch('/api/auth/agents/testdata-gen/stream', {
    method: 'POST',
    headers,
    body: JSON.stringify(payload),
  })

  if (!response.ok) {
    let message = `生成失败（HTTP ${response.status}）`
    if (response.status === 401) {
      message = '请先登录后再使用数据生成智能体'
    } else {
      try {
        const json = await response.json()
        if (json?.message) message = json.message
      } catch {
        // ignore
      }
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
  let pendingDone: Promise<void> | null = null

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

    if (eventName === 'delta') {
      let content: string | undefined
      if (data && typeof data === 'object') {
        const raw = (data as { content?: string }).content
        if (typeof raw === 'string') content = raw
      }
      if (typeof content === 'string') handlers.onDelta?.(content)
    } else if (eventName === 'done') {
      sawDone = true
      const python =
        data && typeof data === 'object' && 'python' in data
          ? String((data as { python?: string }).python || '')
          : ''
      const result = handlers.onDone?.(python) as void | Promise<void>
      if (result && typeof (result as Promise<void>).then === 'function') {
        pendingDone = result as Promise<void>
      }
    } else if (eventName === 'error') {
      const message =
        data && typeof data === 'object' && 'message' in data
          ? String((data as { message?: string }).message || '生成失败')
          : '生成失败'
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
      if (sawDone) break
    }
    if (sawDone) break
  }

  if (!sawDone && buffer.trim()) flushBlock(buffer)
  if (!sawDone) {
    const result = handlers.onDone?.('') as void | Promise<void>
    if (result && typeof (result as Promise<void>).then === 'function') {
      pendingDone = result as Promise<void>
    }
  }

  try {
    void reader.cancel()
  } catch {
    // ignore
  }

  if (pendingDone) await pendingDone
}

/** 去掉模型可能包上的 markdown 围栏 */
export function stripPythonFences(raw: string): string {
  let text = raw.replace(/\r\n/g, '\n').trim()
  if (text.startsWith('```')) {
    const firstNl = text.indexOf('\n')
    if (firstNl > 0) text = text.slice(firstNl + 1)
    const fence = text.lastIndexOf('```')
    if (fence >= 0) text = text.slice(0, fence)
    text = text.trim()
  }
  return text
}
