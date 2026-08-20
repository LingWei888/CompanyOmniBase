<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { generateTestdataScriptStream, stripPythonFences } from '@/api/testdataGen'
import { copyTextPlain } from '@/utils/markdownFragment'
import { normalizeSolutionCode, validateSolutionCode } from '@/utils/solutionCodeValidation'
import { useTestdataGenRecords } from '@/composables/useTestdataGenRecords'
import { useToast } from '@/composables/useToast'
import { useUserAuthStore } from '@/stores/userAuth'

const props = defineProps<{
  modelId: string
  modelName?: string
}>()

const toast = useToast()
const auth = useUserAuthStore()
const { activeDetail, upsertCurrent, ensureLoaded } = useTestdataGenRecords()

const originalText = ref('')
const includeSolution = ref(false)
const solutionCode = ref('')
const resultPython = ref('')
const generating = ref(false)
const streaming = ref(false)

const solutionError = computed(() => {
  if (!includeSolution.value) return ''
  const text = solutionCode.value.trim()
  if (!text) return '已勾选加入题解，请粘贴题解代码'
  return validateSolutionCode(text) ?? ''
})

const canGenerate = computed(() => {
  return (
    !generating.value
    && !!props.modelId
    && !!originalText.value.trim()
    && !solutionError.value
  )
})

watch(
  activeDetail,
  (detail) => {
    originalText.value = detail?.originalText ?? ''
    resultPython.value = detail?.resultPython ?? ''
    const savedSolution = detail?.solutionCode ?? ''
    solutionCode.value = savedSolution
    includeSolution.value = !!savedSolution.trim()
  },
  { immediate: true },
)

void ensureLoaded().catch((error) => {
  const message = error instanceof Error ? error.message : '加载生成记录失败'
  toast.error(message)
})

function getFlushPayload() {
  return {
    originalText: originalText.value.trim(),
    resultPython: resultPython.value,
    solutionCode: includeSolution.value ? normalizeSolutionCode(solutionCode.value) : '',
  }
}

defineExpose({ getFlushPayload })

async function onGenerate() {
  if (!auth.isLoggedIn) {
    toast.error('请先登录后再使用数据生成智能体')
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
  if (!canGenerate.value) return

  const solution = includeSolution.value ? normalizeSolutionCode(solutionCode.value) : ''

  generating.value = true
  streaming.value = true
  resultPython.value = ''
  let streamBuf = ''

  try {
    await generateTestdataScriptStream(
      {
        modelId: Number(props.modelId),
        originalText: originalText.value.trim(),
        ...(solution ? { solutionCode: solution } : {}),
      },
      {
        onDelta(chunk) {
          streamBuf += chunk
          resultPython.value = streamBuf
        },
        async onDone(python) {
          const raw = python || streamBuf
          resultPython.value = stripPythonFences(raw)
          streaming.value = false
          generating.value = false
          try {
            await upsertCurrent({
              originalText: originalText.value.trim(),
              resultPython: resultPython.value,
              solutionCode: solution,
            })
          } catch (error) {
            const message = error instanceof Error ? error.message : '保存生成记录失败'
            toast.error(message)
          }
        },
      },
    )
  } catch (error) {
    const message = error instanceof Error ? error.message : '生成失败'
    toast.error(message)
    streaming.value = false
    generating.value = false
  } finally {
    generating.value = false
    streaming.value = false
  }
}

async function onCopyPython() {
  const text = stripPythonFences(resultPython.value)
  if (!text.trim()) {
    toast.error('暂无可复制内容')
    return
  }
  try {
    await copyTextPlain(text)
    toast.success('已复制 Python 程序')
  } catch {
    toast.error('复制失败，请手动选择复制')
  }
}

function onClearForm() {
  originalText.value = ''
  includeSolution.value = false
  solutionCode.value = ''
  resultPython.value = ''
}

function onToggleSolution(event: Event) {
  includeSolution.value = (event.target as HTMLInputElement).checked
}
</script>

<template>
  <div class="workspace">
    <header class="ws-head">
      <div class="ws-head-main">
        <h1>数据生成智能体</h1>
        <p>根据题面（可选题解）生成 Python 脚本；本机运行后产出成对数据：1.in/1.out … N.in/N.out</p>
      </div>
      <div v-if="modelName" class="model-tag">模型：{{ modelName }}</div>
    </header>

    <div class="ws-body">
      <section class="panel input-panel">
        <div class="field grow">
          <div class="field-head">
            <span class="label">原题全文 <em class="req">*</em></span>
            <label class="solution-toggle" title="勾选后可粘贴题解，用于生成正确 .out">
              <input
                type="checkbox"
                :checked="includeSolution"
                :disabled="generating"
                @change="onToggleSolution"
              />
              <span>加入题解</span>
            </label>
          </div>
          <textarea
            v-model="originalText"
            placeholder="粘贴完整原题（含输入格式与数据范围）…"
            :disabled="generating"
          />
          <div v-if="includeSolution" class="solution-block">
            <textarea
              v-model="solutionCode"
              class="solution-textarea"
              placeholder="粘贴题解 / 标程（建议 ```cpp 代码块）…"
              :disabled="generating"
            />
            <p v-if="solutionError" class="solution-error">{{ solutionError }}</p>
            <p v-else-if="solutionCode.trim() && !solutionError" class="solution-ok">题解格式检查通过</p>
          </div>
        </div>

        <div class="panel-actions">
          <button type="button" class="ghost" :disabled="generating" @click="onClearForm">清空表单</button>
          <button type="button" class="primary" :disabled="!canGenerate" @click="onGenerate">
            {{ generating ? '生成中…' : '生成 Python 程序' }}
          </button>
        </div>
      </section>

      <section class="panel preview-panel">
        <div class="output-head">
          <h2>Python 生成脚本</h2>
          <div class="copy-group">
            <button type="button" class="copy" :disabled="!resultPython.trim() || generating" @click="onCopyPython">
              复制 Python 程序
            </button>
          </div>
        </div>

        <div v-if="generating && !resultPython" class="placeholder">正在生成脚本，请稍候…</div>
        <div v-else-if="!resultPython" class="placeholder">
          填写原题后点击「生成 Python 程序」，脚本将显示在此处
        </div>
        <pre v-else class="code-preview" :class="{ streaming }"><code>{{ resultPython }}</code></pre>
        <p v-if="resultPython" class="preview-hint">
          复制后保存为 <code>gen.py</code>，执行例如
          <code>python gen.py --out testdata --count 20 --seed 1</code>
          → 得到 <code>1.in</code>/<code>1.out</code> … <code>20.in</code>/<code>20.out</code>
        </p>
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

.field-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-shrink: 0;
}

.label {
  font-size: 13px;
  font-weight: 600;
  color: #333;
}

.req {
  color: #c0392b;
  font-style: normal;
}

.field textarea {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #e3e3e3;
  border-radius: 12px;
  padding: 10px 12px;
  font: inherit;
  font-size: 14px;
  background: #fff;
  flex: 1;
  min-height: 160px;
  resize: none;
  line-height: 1.55;
}

.solution-block {
  margin-top: 8px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.solution-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 600;
  color: #555;
  cursor: pointer;
  user-select: none;
  white-space: nowrap;
  padding: 4px 8px;
  border-radius: 999px;
  border: 1px solid #e8e8e8;
  background: #fff;
}

.solution-toggle input {
  width: 14px;
  height: 14px;
  margin: 0;
  accent-color: #111;
  flex-shrink: 0;
}

.solution-textarea {
  width: 100%;
  box-sizing: border-box;
  min-height: 100px;
  max-height: 180px;
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

.code-preview {
  flex: 1;
  min-height: 0;
  overflow: auto;
  margin: 0;
  background: #1e1e1e;
  color: #d4d4d4;
  border: 1px solid #333;
  border-radius: 12px;
  padding: 14px 16px;
  font-size: 12.5px;
  line-height: 1.55;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  white-space: pre;
}

.code-preview.streaming {
  outline: 1px solid #555;
}

.code-preview code {
  font: inherit;
  color: inherit;
}

.preview-hint {
  margin: 10px 0 0;
  font-size: 12px;
  color: #999;
  line-height: 1.5;
  flex-shrink: 0;
}

.preview-hint code {
  font-size: 11px;
  background: #f0f0f0;
  padding: 1px 5px;
  border-radius: 4px;
}

@media (max-width: 1100px) {
  .workspace {
    overflow: auto;
    -webkit-overflow-scrolling: touch;
    height: auto;
    min-height: 100%;
    padding: 12px 12px 20px;
  }

  .ws-head {
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
    padding-bottom: 10px;
  }

  .ws-head h1 {
    font-size: 18px;
  }

  .ws-head p {
    font-size: 12px;
  }

  .model-tag {
    padding: 4px 10px;
  }

  .ws-body {
    grid-template-columns: 1fr;
    grid-template-rows: none;
    gap: 12px;
    padding-top: 12px;
    overflow: visible;
  }

  .panel {
    overflow: visible;
    min-height: auto;
    height: auto;
    padding: 12px;
  }

  .field.grow {
    flex: none;
  }

  .field textarea {
    flex: none;
    min-height: 180px;
    height: 42vh;
    max-height: 360px;
    resize: vertical;
  }

  .solution-textarea {
    min-height: 120px;
    max-height: 240px;
  }

  .placeholder,
  .code-preview {
    min-height: 220px;
    height: 42vh;
    max-height: 420px;
  }

  .output-head {
    flex-wrap: wrap;
    align-items: flex-start;
  }

  .panel-actions {
    flex-wrap: wrap;
  }

  .panel-actions .primary,
  .panel-actions .ghost {
    flex: 1 1 auto;
    min-width: 120px;
  }
}

@media (max-width: 560px) {
  .field textarea {
    height: 36vh;
    min-height: 160px;
  }

  .solution-toggle {
    font-size: 11px;
    padding: 3px 7px;
    gap: 5px;
  }

  .copy-group {
    width: 100%;
  }

  .copy {
    flex: 1 1 auto;
    text-align: center;
  }
}
</style>
