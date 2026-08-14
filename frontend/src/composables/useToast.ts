import { reactive } from 'vue'

export type ToastType = 'success' | 'error' | 'info'

export interface ToastItem {
  id: number
  type: ToastType
  message: string
}

const state = reactive({
  items: [] as ToastItem[],
})

let seed = 1

function push(type: ToastType, message: string, duration = 2800) {
  const text = message?.trim()
  if (!text) return
  const id = seed++
  state.items.push({ id, type, message: text })
  window.setTimeout(() => dismiss(id), duration)
}

function dismiss(id: number) {
  const index = state.items.findIndex((item) => item.id === id)
  if (index >= 0) {
    state.items.splice(index, 1)
  }
}

export function useToast() {
  return {
    toasts: state.items,
    success: (message: string, duration?: number) => push('success', message, duration),
    error: (message: string, duration?: number) => push('error', message, duration ?? 3600),
    info: (message: string, duration?: number) => push('info', message, duration),
    dismiss,
  }
}
