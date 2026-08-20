<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import MarkdownContent from '@/components/MarkdownContent.vue'
import { convertProblemStream } from '@/api/problemConvert'
import { normalizeProblemMarkdown } from '@/utils/problemMarkdown'
import { copyHtmlSource, copyTextPlain, markdownToExportHtml } from '@/utils/markdownFragment'
import { normalizeSolutionCode, validateSolutionCode } from '@/utils/solutionCodeValidation'
import { useProblemConvertRecords } from '@/composables/useProblemConvertRecords'
import { useToast } from '@/composables/useToast'
import { useUserAuthStore } from '@/stores/userAuth'

const props = defineProps<{
  modelId: string
  modelName?: string
}>()

const toast = useToast()
const auth = useUserAuthStore()
const previewRef = ref<InstanceType<typeof MarkdownContent> | null>(null)

const { activeDetail, upsertCurrent, ensureLoaded } = useProblemConvertRecords()

const referenceNickname = ref('')
const originalText = ref('')
const resultMarkdown = ref('')
const includeSolution = ref(false)
const solutionCode = ref('')
const converting = ref(false)
const streaming = ref(false)

const solutionError = computed(() => {
  if (!includeSolution.value) return ''
  const text = solutionCode.value.trim()
  if (!text) return '已勾选加入题解，请粘贴题解代码'
  return validateSolutionCode(text) ?? ''
})

const canConvert = computed(() => {
  return (
    !converting.value
    && !!props.modelId
    && !!referenceNickname.value.trim()
    && !!originalText.value.trim()
    && !solutionError.value
  )
})

watch(
  activeDetail,
  (detail) => {
    referenceNickname.value = detail?.referenceNickname ?? ''
    originalText.value = detail?.originalText ?? ''
    resultMarkdown.value = detail?.resultMarkdown ?? ''
    const savedSolution = detail?.solutionCode ?? ''
    solutionCode.value = savedSolution
    includeSolution.value = !!savedSolution.trim()
  },
  { immediate: true },
)

void ensureLoaded().catch((error) => {
  const message = error instanceof Error ? error.message : '加载转换记录失败'
  toast.error(message)
})

async function onConvert() {
  if (!auth.isLoggedIn) {
    toast.error('请先登录后再使用题意修改智能体')
    return
  }
  if (!props.modelId) {
    toast.error('请先在侧栏或顶部选择对话模型')
    return
  }
  if (solutionError.value) {
    toast.error(solutionError.value)
    return
  }
  if (!canConvert.value) return

  const solution = includeSolution.value ? normalizeSolutionCode(solutionCode.value) : ''

  converting.value = true
  streaming.value = true
  resultMarkdown.value = ''
  let streamBuf = ''

  try {
    await convertProblemStream(
      {
        modelId: Number(props.modelId),
        referenceNickname: referenceNickname.value.trim() || undefined,
        originalText: originalText.value.trim(),
        ...(solution ? { solutionCode: solution } : {}),
      },
      {
        onDelta(chunk) {
          streamBuf += chunk
          resultMarkdown.value = streamBuf
        },
        async onDone(markdown) {
          const title = referenceNickname.value.trim()
          const raw = markdown || streamBuf
          resultMarkdown.value = title ? normalizeProblemMarkdown(raw, title) : raw
          // 一收到完成事件就结束「转换中」，保存记录放到后面，避免 SSE 未关闭时按钮一直卡住
          streaming.value = false
          converting.value = false
          try {
            await upsertCurrent({
              referenceNickname: referenceNickname.value.trim(),
              originalText: originalText.value.trim(),
              resultMarkdown: resultMarkdown.value,
              solutionCode: solution,
            })
          } catch (error) {
            const message = error instanceof Error ? error.message : '保存转换记录失败'
            toast.error(message)
          }
        },
      },
    )
  } catch (error) {
    const message = error instanceof Error ? error.message : '转换失败'
    toast.error(message)
    streaming.value = false
    converting.value = false
  } finally {
    converting.value = false
    streaming.value = false
  }
}

async function onCopyMarkdown() {
  const text = resultMarkdown.value.trim()
  if (!text) {
    toast.error('暂无可复制内容')
    return
  }
  try {
    await copyTextPlain(text)
    toast.success('已复制 Markdown')
  } catch {
    toast.error('复制失败，请手动选择复制')
  }
}

async function onCopyHtml() {
  const md = resultMarkdown.value.trim()
  if (!md) {
    toast.error('暂无可复制内容')
    return
  }
  try {
    const html = markdownToExportHtml(md)
    await copyHtmlSource(html)
    toast.success('已复制 HTML 源码')
  } catch {
    toast.error('复制失败，请手动选择复制')
  }
}

function onFragmentCopied(format: 'html' | 'markdown') {
  toast.success(format === 'html' ? '已复制选中 HTML 源码' : '已复制选中 Markdown 片段')
}

function onFragmentCopyFailed() {
  toast.error('片段复制失败，请重试')
}

function onClearForm() {
  referenceNickname.value = ''
  originalText.value = ''
  resultMarkdown.value = ''
  includeSolution.value = false
  solutionCode.value = ''
}

function onToggleSolution(event: Event) {
  const checked = (event.target as HTMLInputElement).checked
  includeSolution.value = checked
  if (!checked) {
    // 取消勾选时保留文本，方便再次打开；不强制清空
  }
}
</script>

<template>
  <div class="workspace">
    <header class="ws-head">
      <div class="ws-head-main">
        <h1>题意修改智能体</h1>
        <p>按目标标题改写题面叙事；输入输出、样例与数据范围与原题保持一致</p>
      </div>
      <div v-if="modelName" class="model-tag">模型：{{ modelName }}</div>
    </header>

    <div class="ws-body">
      <section class="panel input-panel">
        <label class="field">
          <span class="label">目标标题 <em class="req">*</em></span>
          <input
            v-model="referenceNickname"
            type="text"
            placeholder="如：小码的暴击游戏（将替换原题首行 # 标题）"
            :disabled="converting"
          />
          <small>必填。输出题面的一级标题将使用此名称。</small>
        </label>

        <label class="field grow">
          <span class="label">原题全文 <em class="req">*</em></span>
          <textarea
            v-model="originalText"
            placeholder="粘贴完整原题 Markdown…"
            :disabled="converting"
          />
        </label>

        <div class="solution-block">
          <label class="solution-toggle">
            <input
              type="checkbox"
              :checked="includeSolution"
              :disabled="converting"
              @change="onToggleSolution"
            />
            <span>加入题解代码（可选）</span>
          </label>
          <small class="solution-hint">
            用于锚定算法意图，防止换皮跑偏；不会写入输出题面。建议粘贴带语言标记的代码块，如 <code>```cpp</code>。
          </small>
          <textarea
            v-if="includeSolution"
            v-model="solutionCode"
            class="solution-textarea"
            placeholder="粘贴题解 / 标程源码…"
            :disabled="converting"
          />
          <p v-if="includeSolution && solutionError" class="solution-error">{{ solutionError }}</p>
          <p v-else-if="includeSolution && solutionCode.trim() && !solutionError" class="solution-ok">
            题解格式检查通过
          </p>
        </div>

        <div class="panel-actions">
          <button type="button" class="ghost" :disabled="converting" @click="onClearForm">清空表单</button>
          <button type="button" class="primary" :disabled="!canConvert" @click="onConvert">
            {{ converting ? '转换中…' : '一键转换' }}
          </button>
        </div>
      </section>

      <section class="panel preview-panel">
        <div class="output-head">
          <h2>转换结果预览</h2>
          <div class="copy-group">
            <button type="button" class="copy" :disabled="!resultMarkdown || converting" @click="onCopyMarkdown">
              复制 Markdown
            </button>
            <button type="button" class="copy" :disabled="!resultMarkdown || converting" @click="onCopyHtml">
              复制 HTML 源码
            </button>
          </div>
        </div>

        <div v-if="converting && !resultMarkdown" class="placeholder">正在转换题面，请稍候…</div>
        <div v-else-if="!resultMarkdown" class="placeholder">
          填写原题后点击「一键转换」，此处预览转换结果
        </div>
        <div v-else class="preview">
          <MarkdownContent
            ref="previewRef"
            :content="resultMarkdown"
            :source-markdown="resultMarkdown"
            :fragment-copy="!streaming"
            :streaming="streaming"
            @fragment-copied="onFragmentCopied"
            @fragment-copy-failed="onFragmentCopyFailed"
          />
          <p class="preview-hint">
            选中预览区内容后<strong>右键</strong>，可复制 HTML 源码或 Markdown 片段
          </p>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.workspace {
  flex: 1;
  min-width: 0;
  min-height: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 16px 20px 20px;
  box-sizing: border-box;
}

.ws-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 14px;
  flex-shrink: 0;
  border-bottom: 1px solid #f0f0f0;
}

.ws-head-main {
  min-width: 0;
}

.ws-head h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 650;
}

.ws-head p {
  margin: 4px 0 0;
  color: #777;
  font-size: 13px;
}

.model-tag {
  flex-shrink: 0;
  font-size: 12px;
  color: #666;
  border: 1px solid #e8e8e8;
  background: #fafafa;
  border-radius: 999px;
  padding: 6px 12px;
  white-space: nowrap;
}

.ws-body {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
  padding-top: 16px;
}

.panel {
  border: 1px solid #ececec;
  border-radius: 16px;
  background: #fafafa;
  padding: 16px;
  min-height: 0;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
  flex-shrink: 0;
}

.field.grow {
  flex: 1;
  min-height: 0;
  margin-bottom: 0;
  display: flex;
  flex-direction: column;
}

.label {
  font-size: 13px;
  font-weight: 600;
  color: #333;
}

.field small {
  font-size: 12px;
  color: #999;
}

.req {
  color: #c0392b;
  font-style: normal;
}

.field input,
.field textarea {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #e3e3e3;
  border-radius: 12px;
  padding: 10px 12px;
  font: inherit;
  font-size: 14px;
  background: #fff;
}

.field textarea {
  flex: 1;
  min-height: 180px;
  resize: none;
  line-height: 1.55;
}

.solution-block {
  margin-top: 12px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.solution-toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #333;
  cursor: pointer;
  user-select: none;
}

.solution-toggle input {
  width: 15px;
  height: 15px;
  accent-color: #111;
}

.solution-hint {
  font-size: 12px;
  color: #999;
  line-height: 1.45;
}

.solution-hint code {
  font-size: 11px;
  background: #f0f0f0;
  padding: 1px 5px;
  border-radius: 4px;
}

.solution-textarea {
  width: 100%;
  box-sizing: border-box;
  min-height: 140px;
  max-height: 220px;
  resize: vertical;
  border: 1px solid #e3e3e3;
  border-radius: 12px;
  padding: 10px 12px;
  font: inherit;
  font-size: 13px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  line-height: 1.5;
  background: #fff;
}

.solution-error {
  margin: 0;
  font-size: 12px;
  color: #c0392b;
  line-height: 1.4;
}

.solution-ok {
  margin: 0;
  font-size: 12px;
  color: #2f7d32;
  line-height: 1.4;
}

.panel-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 12px;
  flex-shrink: 0;
}

.ghost,
.primary,
.copy {
  border-radius: 10px;
  padding: 8px 16px;
  font: inherit;
  cursor: pointer;
}

.ghost {
  border: 1px solid #e5e5e5;
  background: #fff;
  color: #444;
}

.primary {
  border: 0;
  background: #111;
  color: #fff;
}

.primary:disabled,
.ghost:disabled,
.copy:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.output-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
  flex-shrink: 0;
}

.output-head h2 {
  margin: 0;
  font-size: 15px;
  font-weight: 650;
}

.copy-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.copy {
  border: 1px solid #ddd;
  background: #fff;
  color: #333;
  padding: 6px 12px;
  font-size: 13px;
  white-space: nowrap;
}

.preview-hint {
  margin: 10px 0 0;
  font-size: 12px;
  color: #999;
  line-height: 1.5;
}

.preview-hint strong {
  color: #666;
  font-weight: 600;
}

.placeholder {
  flex: 1;
  min-height: 0;
  display: grid;
  place-items: center;
  color: #aaa;
  font-size: 14px;
  text-align: center;
  padding: 24px;
  border: 1px dashed #e0e0e0;
  border-radius: 12px;
  background: #fff;
}

.preview {
  flex: 1;
  min-height: 0;
  overflow: auto;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 14px 16px;
}

@media (max-width: 1100px) {
  .ws-body {
    grid-template-columns: 1fr;
    grid-template-rows: minmax(260px, 1fr) minmax(260px, 1fr);
    overflow: auto;
  }

  .workspace {
    overflow: auto;
  }
}
</style>
