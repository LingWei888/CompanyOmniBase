<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { renderMarkdown } from '@/utils/markdown'

const props = withDefaults(
  defineProps<{
    content: string
    pending?: boolean
    streaming?: boolean
  }>(),
  {
    pending: false,
    streaming: false,
  },
)

const emit = defineEmits<{
  rendered: []
}>()

const rootEl = ref<HTMLElement | null>(null)
const html = ref('')
const copiedTimers = new WeakMap<HTMLElement, number>()

const STREAM_INTERVAL_MS = 40
let rafId = 0
let timerId = 0
let pendingContent = ''
let pendingStreaming = false

function paint(content: string, streaming: boolean) {
  html.value = renderMarkdown(content || '', { streaming })
  queueMicrotask(() => emit('rendered'))
}

function flush() {
  timerId = 0
  if (rafId) cancelAnimationFrame(rafId)
  rafId = requestAnimationFrame(() => {
    rafId = 0
    paint(pendingContent, pendingStreaming)
  })
}

function scheduleRender(content: string, streaming: boolean) {
  pendingContent = content
  pendingStreaming = streaming
  if (!streaming) {
    if (timerId) {
      window.clearTimeout(timerId)
      timerId = 0
    }
    if (rafId) {
      cancelAnimationFrame(rafId)
      rafId = 0
    }
    paint(content, false)
    return
  }
  if (timerId) return
  timerId = window.setTimeout(flush, STREAM_INTERVAL_MS)
}

watch(
  () => [props.content, props.pending, props.streaming] as const,
  ([content, pending, streaming]) => {
    if (pending) {
      html.value = ''
      return
    }
    scheduleRender(content || '', !!streaming)
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  if (timerId) window.clearTimeout(timerId)
  if (rafId) cancelAnimationFrame(rafId)
})

async function copyText(text: string) {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text)
    return
  }
  const area = document.createElement('textarea')
  area.value = text
  area.setAttribute('readonly', '')
  area.style.position = 'fixed'
  area.style.left = '-9999px'
  document.body.appendChild(area)
  area.select()
  document.execCommand('copy')
  document.body.removeChild(area)
}

async function onClick(event: MouseEvent) {
  const target = event.target as HTMLElement | null
  const btn = target?.closest('.md-code-copy') as HTMLElement | null
  if (!btn || !rootEl.value?.contains(btn)) return

  event.preventDefault()
  event.stopPropagation()

  const block = btn.closest('.md-code-block')
  const code = block?.querySelector('pre code')?.textContent ?? ''
  if (!code) return

  try {
    await copyText(code)
    btn.classList.add('is-copied')
    btn.setAttribute('title', '已复制')
    const prev = copiedTimers.get(btn)
    if (prev) window.clearTimeout(prev)
    const timer = window.setTimeout(() => {
      btn.classList.remove('is-copied')
      btn.setAttribute('title', '复制代码')
      copiedTimers.delete(btn)
    }, 1600)
    copiedTimers.set(btn, timer)
  } catch {
    btn.setAttribute('title', '复制失败')
  }
}
</script>

<template>
  <div v-if="pending" class="md-body pending">{{ content }}</div>
  <div
    v-else
    ref="rootEl"
    class="md-body"
    :class="{ streaming }"
    v-html="html"
    @click="onClick"
  />
</template>

<style scoped>
.md-body {
  font-size: 15px;
  line-height: 1.7;
  color: #1f1f1f;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.md-body.pending {
  color: #888;
  white-space: pre-wrap;
}

.md-body.streaming::after {
  content: '▍';
  display: inline-block;
  margin-left: 2px;
  color: #888;
  animation: md-caret-blink 1s step-end infinite;
}

.md-body.streaming :deep(.md-code-block:last-child)::after {
  content: none;
}

@keyframes md-caret-blink {
  50% { opacity: 0; }
}

.md-body :deep(p) {
  margin: 0 0 0.85em;
}

.md-body :deep(p:last-child) {
  margin-bottom: 0;
}

.md-body :deep(h1),
.md-body :deep(h2),
.md-body :deep(h3),
.md-body :deep(h4),
.md-body :deep(h5),
.md-body :deep(h6) {
  margin: 1.15em 0 0.55em;
  line-height: 1.35;
  font-weight: 700;
  color: #111;
  letter-spacing: -0.01em;
}

.md-body :deep(h1:first-child),
.md-body :deep(h2:first-child),
.md-body :deep(h3:first-child),
.md-body :deep(h4:first-child),
.md-body :deep(h5:first-child),
.md-body :deep(h6:first-child) {
  margin-top: 0.2em;
}

.md-body :deep(h1) { font-size: 1.55em; }
.md-body :deep(h2) { font-size: 1.35em; }
.md-body :deep(h3) { font-size: 1.2em; }
.md-body :deep(h4) { font-size: 1.08em; }
.md-body :deep(h5) { font-size: 1em; }
.md-body :deep(h6) { font-size: 0.95em; color: #333; font-weight: 650; }

.md-body :deep(.md-cite) {
  display: inline-block;
  margin-left: 2px;
  padding: 0 5px;
  border-radius: 999px;
  background: #eef2ff;
  color: #3730a3;
  font-size: 0.78em;
  font-weight: 650;
  line-height: 1.4;
  vertical-align: super;
}

.md-body :deep(ul),
.md-body :deep(ol) {
  margin: 0.4em 0 0.9em;
  padding-left: 1.4em;
}

.md-body :deep(li) {
  margin: 0.25em 0;
}

.md-body :deep(li > p) {
  margin: 0.2em 0;
}

.md-body :deep(blockquote) {
  margin: 0.8em 0;
  padding: 0.35em 0 0.35em 0.9em;
  border-left: 3px solid #d0d0d0;
  color: #555;
}

.md-body :deep(hr) {
  border: 0;
  border-top: 1px solid #e8e8e8;
  margin: 1.1em 0;
}

.md-body :deep(a) {
  color: #1a5fb4;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.md-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 0.8em 0 1em;
  font-size: 0.95em;
  display: block;
  overflow-x: auto;
}

.md-body :deep(th),
.md-body :deep(td) {
  border: 1px solid #e2e2e2;
  padding: 8px 10px;
  text-align: left;
  vertical-align: top;
}

.md-body :deep(th) {
  background: #f6f6f6;
  font-weight: 600;
}

.md-body :deep(img) {
  max-width: 100%;
  border-radius: 10px;
}

.md-body :deep(.md-inline-code) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 0.9em;
  background: #f2f2f2;
  border: 1px solid #e6e6e6;
  border-radius: 6px;
  padding: 0.1em 0.4em;
  color: #b42318;
}

.md-body :deep(.md-code-block) {
  margin: 0.85em 0 1em;
  border: 1px solid #e4e4e4;
  border-radius: 12px;
  overflow: hidden;
  background: #0f1419;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.04);
}

.md-body :deep(.md-code-header) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 8px 6px 12px;
  background: #1a222c;
  border-bottom: 1px solid #2a3441;
}

.md-body :deep(.md-code-lang) {
  font-size: 12px;
  color: #9aa4b2;
  text-transform: lowercase;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}

.md-body :deep(.md-code-copy) {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #9aa4b2;
  display: inline-grid;
  place-items: center;
  cursor: pointer;
  padding: 0;
}

.md-body :deep(.md-code-copy:hover) {
  background: rgba(255, 255, 255, 0.08);
  color: #e7ecf3;
}

.md-body :deep(.md-code-copy .md-code-copied-icon) {
  display: none;
}

.md-body :deep(.md-code-copy.is-copied) {
  color: #7ee787;
}

.md-body :deep(.md-code-copy.is-copied .md-code-copy-icon) {
  display: none;
}

.md-body :deep(.md-code-copy.is-copied .md-code-copied-icon) {
  display: block;
}

.md-body :deep(.md-code-block pre) {
  margin: 0;
  padding: 14px 16px;
  overflow-x: auto;
  background: transparent;
  white-space: pre;
  line-height: 1.55;
}

.md-body :deep(.md-code-block code) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 13px;
  color: #e7ecf3;
  background: transparent;
  border: 0;
  padding: 0;
}

.md-body :deep(.hljs-comment),
.md-body :deep(.hljs-quote) { color: #8b949e; font-style: italic; }
.md-body :deep(.hljs-keyword),
.md-body :deep(.hljs-selector-tag),
.md-body :deep(.hljs-addition) { color: #ff7b72; }
.md-body :deep(.hljs-string),
.md-body :deep(.hljs-meta .hljs-string),
.md-body :deep(.hljs-attr) { color: #a5d6ff; }
.md-body :deep(.hljs-number),
.md-body :deep(.hljs-literal),
.md-body :deep(.hljs-variable),
.md-body :deep(.hljs-template-variable) { color: #79c0ff; }
.md-body :deep(.hljs-title),
.md-body :deep(.hljs-section),
.md-body :deep(.hljs-attribute) { color: #d2a8ff; }
.md-body :deep(.hljs-type),
.md-body :deep(.hljs-built_in),
.md-body :deep(.hljs-name) { color: #7ee787; }
.md-body :deep(.hljs-symbol),
.md-body :deep(.hljs-bullet),
.md-body :deep(.hljs-link) { color: #ffa657; }
.md-body :deep(.hljs-deletion) { color: #ffa198; }
.md-body :deep(.hljs-meta) { color: #d2a8ff; }
</style>
