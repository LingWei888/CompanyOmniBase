<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import SideDrawer from '@/components/admin/SideDrawer.vue'
import PaginationBar from '@/components/admin/PaginationBar.vue'
import { useToast } from '@/composables/useToast'
import { listKnowledgeBaseOptions, type KnowledgeBase } from '@/api/knowledge'
import {
  deleteDocument,
  fetchChunkDefaults,
  fetchDocumentChunk,
  fetchParsedText,
  listDocumentChunks,
  listDocuments,
  replaceDocument,
  requeueDocument,
  updateDocument,
  uploadDocument,
  type DocumentChunkDetail,
  type DocumentChunkItem,
  type DocumentStatus,
  type KbDocument,
} from '@/api/document'

const toast = useToast()
const loading = ref(false)
const rows = ref<KbDocument[]>([])
const kbs = ref<KnowledgeBase[]>([])
const filterKbId = ref('')
const filterStatus = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const totalPages = ref(0)

const defaults = reactive({
  chunkSize: 800,
  chunkOverlap: 100,
  systemChunkSize: 800,
  systemChunkOverlap: 100,
  kbChunkSize: null as number | null,
  kbChunkOverlap: null as number | null,
})

const drawerOpen = ref(false)
const mode = ref<'create' | 'edit' | 'replace'>('create')
const saving = ref(false)
const editing = ref<KbDocument | null>(null)
const advancedOpen = ref(false)
const form = reactive({
  kbId: '',
  title: '',
  file: null as File | null,
  chunkSize: '' as number | '',
  chunkOverlap: '' as number | '',
})

const parsedOpen = ref(false)
const parsedLoading = ref(false)
const parsedTitle = ref('')
const parsedContent = ref('')
const parsedCharCount = ref(0)

const chunksOpen = ref(false)
const chunksLoading = ref(false)
const chunksDoc = ref<KbDocument | null>(null)
const chunkRows = ref<DocumentChunkItem[]>([])
const chunkPage = ref(1)
const chunkSizePage = ref(10)
const chunkTotal = ref(0)
const chunkTotalPages = ref(0)

const chunkDetailOpen = ref(false)
const chunkDetailLoading = ref(false)
const chunkDetail = ref<DocumentChunkDetail | null>(null)

const drawerTitle = computed(() => {
  if (mode.value === 'create') return '上传文档'
  if (mode.value === 'replace') return '替换上传'
  return '编辑文档'
})

const kbNameMap = computed(() => {
  const map = new Map<number, string>()
  kbs.value.forEach((item) => map.set(item.id, item.name))
  return map
})

async function loadMeta() {
  kbs.value = await listKnowledgeBaseOptions()
  await refreshDefaults()
}

async function refreshDefaults(kbId?: number) {
  const id = kbId ?? (form.kbId ? Number(form.kbId) : undefined)
  const chunk = await fetchChunkDefaults(id)
  defaults.chunkSize = chunk.chunkSize
  defaults.chunkOverlap = chunk.chunkOverlap
  defaults.systemChunkSize = chunk.systemChunkSize ?? chunk.chunkSize
  defaults.systemChunkOverlap = chunk.systemChunkOverlap ?? chunk.chunkOverlap
  defaults.kbChunkSize = chunk.kbChunkSize ?? null
  defaults.kbChunkOverlap = chunk.kbChunkOverlap ?? null
}

const chunkPlaceholderHint = computed(() => {
  const sizeLabel =
    defaults.kbChunkSize != null
      ? `知识库 ${defaults.kbChunkSize}`
      : `系统 ${defaults.systemChunkSize}`
  const overlapLabel =
    defaults.kbChunkOverlap != null
      ? `知识库 ${defaults.kbChunkOverlap}`
      : `系统 ${defaults.systemChunkOverlap}`
  return { sizeLabel, overlapLabel }
})

async function load() {
  loading.value = true
  try {
    const data = await listDocuments({
      kbId: filterKbId.value ? Number(filterKbId.value) : undefined,
      status: (filterStatus.value || undefined) as DocumentStatus | undefined,
      page: page.value,
      size: size.value,
    })
    rows.value = data.records
    total.value = data.total
    totalPages.value = data.totalPages
    page.value = data.page
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

watch([page, size], load)
watch([filterKbId, filterStatus], () => {
  if (page.value !== 1) {
    page.value = 1
  } else {
    load()
  }
})

function applyDefaultsToForm() {
  // 留空表示使用知识库/系统默认，不预填数字
  form.chunkSize = ''
  form.chunkOverlap = ''
}

function openCreate() {
  mode.value = 'create'
  editing.value = null
  form.kbId = filterKbId.value || (kbs.value[0] ? String(kbs.value[0].id) : '')
  form.title = ''
  form.file = null
  applyDefaultsToForm()
  advancedOpen.value = false
  drawerOpen.value = true
  refreshDefaults(form.kbId ? Number(form.kbId) : undefined).catch(() => undefined)
}

function openEdit(row: KbDocument) {
  mode.value = 'edit'
  editing.value = row
  form.kbId = String(row.kbId)
  form.title = row.title
  form.file = null
  form.chunkSize = row.chunkSize ?? ''
  form.chunkOverlap = row.chunkOverlap ?? ''
  advancedOpen.value = false
  drawerOpen.value = true
  refreshDefaults(row.kbId).catch(() => undefined)
}

function openReplace(row: KbDocument) {
  mode.value = 'replace'
  editing.value = row
  form.kbId = String(row.kbId)
  form.title = row.title
  form.file = null
  applyDefaultsToForm()
  advancedOpen.value = false
  drawerOpen.value = true
  refreshDefaults(row.kbId).catch(() => undefined)
}

watch(
  () => form.kbId,
  (kbId) => {
    if (drawerOpen.value && mode.value === 'create' && kbId) {
      refreshDefaults(Number(kbId)).catch(() => undefined)
    }
  },
)

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  form.file = input.files?.[0] || null
  if (form.file && (mode.value === 'create' || !form.title)) {
    form.title = form.file.name.replace(/\.[^.]+$/, '')
  }
}

function validateChunk() {
  const sizeVal = form.chunkSize === '' ? null : Number(form.chunkSize)
  const overlapVal = form.chunkOverlap === '' ? null : Number(form.chunkOverlap)
  if (sizeVal != null && (sizeVal < 100 || sizeVal > 8000)) {
    throw new Error('切分长度需在 100-8000 之间，或留空使用默认')
  }
  if (overlapVal != null && (overlapVal < 0 || overlapVal > 4000)) {
    throw new Error('重叠长度需在 0-4000 之间，或留空使用默认')
  }
  if (sizeVal != null && overlapVal != null && overlapVal >= sizeVal) {
    throw new Error('重叠长度必须小于切分长度')
  }
  return { sizeVal, overlapVal }
}

async function save() {
  saving.value = true
  try {
    const { sizeVal, overlapVal } = validateChunk()
    if (mode.value === 'create') {
      if (!form.kbId) throw new Error('请选择知识库')
      if (!form.file) throw new Error('请选择文件')
      await uploadDocument(Number(form.kbId), form.file, {
        title: form.title.trim() || undefined,
        chunkSize: sizeVal ?? undefined,
        chunkOverlap: overlapVal ?? undefined,
      })
      toast.success('上传成功')
    } else if (mode.value === 'replace') {
      if (!editing.value) throw new Error('文档不存在')
      if (!form.file) throw new Error('请选择要替换的文件')
      await replaceDocument(editing.value.id, form.file, {
        title: form.title.trim() || undefined,
        chunkSize: sizeVal ?? undefined,
        chunkOverlap: overlapVal ?? undefined,
      })
      toast.success('替换成功，已重新入队')
    } else if (editing.value) {
      if (!form.title.trim()) throw new Error('请填写标题')
      await updateDocument(editing.value.id, {
        title: form.title.trim(),
        chunkSize: sizeVal ?? undefined,
        chunkOverlap: overlapVal ?? undefined,
      })
      toast.success('保存成功')
    }
    drawerOpen.value = false
    await load()
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function remove(row: KbDocument) {
  if (!confirm(`确认删除文档「${row.title}」？将同时删除 MinIO 对象与解析结果。`)) return
  try {
    await deleteDocument(row.id)
    toast.success('删除成功')
    if (rows.value.length === 1 && page.value > 1) {
      page.value -= 1
    } else {
      await load()
    }
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '删除失败')
  }
}

async function requeue(row: KbDocument) {
  if (!confirm(`确认将文档「${row.title}」重新投递入库（重新解析切分）？`)) return
  try {
    await requeueDocument(row.id)
    toast.success('已重新投递')
    await load()
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '投递失败')
  }
}

async function viewParsed(row: KbDocument) {
  parsedLoading.value = true
  parsedOpen.value = true
  parsedTitle.value = row.title
  parsedContent.value = ''
  parsedCharCount.value = 0
  try {
    const data = await fetchParsedText(row.id)
    parsedTitle.value = data.title
    parsedContent.value = data.content
    parsedCharCount.value = data.charCount
  } catch (e) {
    parsedOpen.value = false
    toast.error(e instanceof Error ? e.message : '加载解析正文失败')
  } finally {
    parsedLoading.value = false
  }
}

async function openChunks(row: KbDocument) {
  chunksDoc.value = row
  chunkPage.value = 1
  chunkSizePage.value = 10
  chunksOpen.value = true
  await loadChunks()
}

async function loadChunks() {
  if (!chunksDoc.value) return
  chunksLoading.value = true
  try {
    const data = await listDocumentChunks(chunksDoc.value.id, chunkPage.value, chunkSizePage.value)
    chunkRows.value = data.records
    chunkTotal.value = data.total
    chunkTotalPages.value = data.totalPages
    chunkPage.value = data.page
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '加载切分结果失败')
  } finally {
    chunksLoading.value = false
  }
}

watch([chunkPage, chunkSizePage], () => {
  if (chunksOpen.value) {
    loadChunks()
  }
})

async function openChunkDetail(item: DocumentChunkItem) {
  if (!chunksDoc.value) return
  chunkDetailLoading.value = true
  chunkDetailOpen.value = true
  chunkDetail.value = null
  try {
    chunkDetail.value = await fetchDocumentChunk(chunksDoc.value.id, item.id)
  } catch (e) {
    chunkDetailOpen.value = false
    toast.error(e instanceof Error ? e.message : '加载片段详情失败')
  } finally {
    chunkDetailLoading.value = false
  }
}

function canRequeue(status: DocumentStatus) {
  return (
    status === 'PENDING' ||
    status === 'FAILED' ||
    status === 'WAITING_EMBEDDING' ||
    status === 'EMBEDDING' ||
    status === 'READY'
  )
}

function canViewParsed(row: KbDocument) {
  return !!row.parsedTextAvailable || (row.parsedCharCount != null && row.parsedCharCount > 0)
}

function canViewChunks(row: KbDocument) {
  return row.chunkCount != null && row.chunkCount > 0
}

function statusLabel(status: DocumentStatus) {
  switch (status) {
    case 'PENDING':
      return '排队中'
    case 'PARSING':
      return '解析中'
    case 'CHUNKING':
      return '切分中'
    case 'WAITING_EMBEDDING':
      return '等待向量化'
    case 'EMBEDDING':
      return '向量化中'
    case 'READY':
      return '已完成'
    case 'FAILED':
      return '失败'
    default:
      return status
  }
}

function formatSize(sizeValue: number) {
  if (sizeValue < 1024) return `${sizeValue} B`
  if (sizeValue < 1024 * 1024) return `${(sizeValue / 1024).toFixed(1)} KB`
  return `${(sizeValue / 1024 / 1024).toFixed(1)} MB`
}

onMounted(async () => {
  try {
    await loadMeta()
    await load()
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '初始化失败')
  }
})
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>文档管理</h2>
        <p>上传后异步入库：排队中 → 解析中 → 切分中 → 等待向量化 → 向量化中 → 已完成</p>
      </div>
      <button type="button" class="primary" @click="openCreate">上传文档</button>
    </div>

    <div class="filters">
      <select v-model="filterKbId">
        <option value="">全部知识库</option>
        <option v-for="kb in kbs" :key="kb.id" :value="String(kb.id)">{{ kb.name }}</option>
      </select>
      <select v-model="filterStatus">
        <option value="">全部状态</option>
        <option value="PENDING">排队中</option>
        <option value="PARSING">解析中</option>
        <option value="CHUNKING">切分中</option>
        <option value="WAITING_EMBEDDING">等待向量化</option>
        <option value="EMBEDDING">向量化中</option>
        <option value="READY">已完成</option>
        <option value="FAILED">失败</option>
      </select>
    </div>

    <p v-if="loading">加载中…</p>

    <template v-else>
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>标题</th>
              <th>知识库</th>
              <th>文件</th>
              <th>大小</th>
              <th>状态</th>
              <th>片段</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td>{{ row.id }}</td>
              <td>{{ row.title }}</td>
              <td>{{ kbNameMap.get(row.kbId) || row.kbId }}</td>
              <td>{{ row.originalFilename }}</td>
              <td>{{ formatSize(row.fileSize) }}</td>
              <td>
                <span class="tag" :class="row.status.toLowerCase()">{{ statusLabel(row.status) }}</span>
                <p v-if="row.status === 'FAILED' && row.errorMessage" class="hint err">{{ row.errorMessage }}</p>
              </td>
              <td>{{ row.chunkCount || 0 }}</td>
              <td class="actions">
                <button type="button" @click="openEdit(row)">编辑</button>
                <button v-if="canViewParsed(row)" type="button" @click="viewParsed(row)">查看正文</button>
                <button v-if="canViewChunks(row)" type="button" @click="openChunks(row)">切分结果</button>
                <button type="button" @click="openReplace(row)">替换上传</button>
                <button v-if="canRequeue(row.status)" type="button" @click="requeue(row)">重新入库</button>
                <button type="button" class="danger" @click="remove(row)">删除</button>
              </td>
            </tr>
            <tr v-if="!rows.length">
              <td colspan="8" class="empty">暂无文档</td>
            </tr>
          </tbody>
        </table>
      </div>

      <PaginationBar
        v-model:page="page"
        v-model:size="size"
        :total="total"
        :total-pages="totalPages"
      />
    </template>

    <SideDrawer :open="drawerOpen" :title="drawerTitle" @close="drawerOpen = false">
      <label v-if="mode === 'create'">
        知识库
        <select v-model="form.kbId">
          <option disabled value="">请选择</option>
          <option v-for="kb in kbs" :key="kb.id" :value="String(kb.id)">{{ kb.name }}</option>
        </select>
      </label>
      <p v-if="mode === 'replace' && editing" class="hint">
        将替换文档 #{{ editing.id }}「{{ editing.originalFilename }}」，并重新入队入库。
      </p>
      <label>
        标题
        <input v-model="form.title" maxlength="256" placeholder="默认取文件名" />
      </label>
      <label v-if="mode === 'create' || mode === 'replace'">
        文件
        <input type="file" accept=".pdf,.doc,.docx,.md,.txt,.markdown" @change="onFileChange" />
      </label>

      <button type="button" class="advanced-toggle" @click="advancedOpen = !advancedOpen">
        {{ advancedOpen ? '收起高级设置' : '展开高级设置' }}
      </button>
      <div v-if="advancedOpen" class="advanced-box">
        <p class="hint">
          留空则使用知识库默认；知识库未配置时再使用系统默认
          （当前生效：{{ defaults.chunkSize }} / {{ defaults.chunkOverlap }}）。
        </p>
        <label>
          切分长度（字符）
          <input
            v-model="form.chunkSize"
            type="number"
            min="100"
            max="8000"
            :placeholder="`留空 → ${chunkPlaceholderHint.sizeLabel}`"
          />
        </label>
        <label>
          重叠长度（字符）
          <input
            v-model="form.chunkOverlap"
            type="number"
            min="0"
            max="4000"
            :placeholder="`留空 → ${chunkPlaceholderHint.overlapLabel}`"
          />
        </label>
      </div>

      <template #footer>
        <button type="button" @click="drawerOpen = false">取消</button>
        <button type="button" class="primary" :disabled="saving" @click="save">
          {{ saving ? '提交中…' : '确定' }}
        </button>
      </template>
    </SideDrawer>

    <SideDrawer
      :open="parsedOpen"
      :title="`解析正文 - ${parsedTitle}`"
      width="640px"
      @close="parsedOpen = false"
    >
      <p v-if="parsedLoading">加载中…</p>
      <template v-else>
        <p class="hint">共 {{ parsedCharCount }} 字符</p>
        <pre class="parsed-text">{{ parsedContent }}</pre>
      </template>
      <template #footer>
        <button type="button" class="primary" @click="parsedOpen = false">关闭</button>
      </template>
    </SideDrawer>

    <SideDrawer
      :open="chunksOpen"
      :title="`切分结果 - ${chunksDoc?.title || ''}`"
      width="560px"
      @close="chunksOpen = false"
    >
      <p class="hint">共 {{ chunkTotal }} 个片段</p>
      <p v-if="chunksLoading">加载中…</p>
      <template v-else>
        <div class="chunk-list">
          <button
            v-for="item in chunkRows"
            :key="item.id"
            type="button"
            class="chunk-item"
            @click="openChunkDetail(item)"
          >
            <div class="chunk-item-head">
              <strong>#{{ item.chunkIndex + 1 }}</strong>
              <span>{{ item.charCount }} 字符</span>
            </div>
            <p>{{ item.preview || '（空片段）' }}</p>
            <span class="chunk-item-action">查看详情</span>
          </button>
          <p v-if="!chunkRows.length" class="empty-inline">暂无切分片段</p>
        </div>
        <PaginationBar
          v-model:page="chunkPage"
          v-model:size="chunkSizePage"
          :total="chunkTotal"
          :total-pages="chunkTotalPages"
        />
      </template>
      <template #footer>
        <button type="button" class="primary" @click="chunksOpen = false">关闭</button>
      </template>
    </SideDrawer>

    <Teleport to="body">
      <div v-if="chunkDetailOpen" class="modal-root">
        <div class="mask" @click="chunkDetailOpen = false" />
        <div class="dialog" role="dialog" aria-modal="true">
          <header>
            <h3>
              片段详情
              <template v-if="chunkDetail">#{{ chunkDetail.chunkIndex + 1 }}</template>
            </h3>
            <button type="button" class="x" @click="chunkDetailOpen = false">×</button>
          </header>
          <p v-if="chunkDetailLoading">加载中…</p>
          <template v-else-if="chunkDetail">
            <p class="hint">
              文档：{{ chunkDetail.documentTitle }} · {{ chunkDetail.charCount }} 字符
            </p>
            <pre class="parsed-text modal-text">{{ chunkDetail.content }}</pre>
          </template>
          <div class="dialog-actions">
            <button type="button" class="primary" @click="chunkDetailOpen = false">关闭</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.advanced-toggle {
  width: 100%;
  margin: 4px 0 12px;
  border: 1px dashed #d4d4d4;
  background: #fafafa;
  border-radius: 8px;
  padding: 10px 12px;
  cursor: pointer;
  color: #525252;
  text-align: left;
}

.advanced-box {
  border: 1px solid #eee;
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 8px;
  background: #fcfcfc;
}

.hint.err {
  margin: 6px 0 0;
  color: #b91c1c;
  font-size: 12px;
  max-width: 220px;
  word-break: break-word;
}

.parsed-text {
  white-space: pre-wrap;
  word-break: break-word;
  background: #f8fafc;
  border: 1px solid #e5e5e5;
  border-radius: 10px;
  padding: 14px;
  max-height: 70vh;
  overflow: auto;
  font-size: 13px;
  line-height: 1.6;
  margin: 0;
  color: #262626;
}

.chunk-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.chunk-item {
  text-align: left;
  border: 1px solid #e5e5e5;
  background: #fff;
  border-radius: 10px;
  padding: 12px;
  cursor: pointer;
  width: 100%;
  font: inherit;
}

.chunk-item:hover {
  border-color: #a3a3a3;
  background: #fafafa;
}

.chunk-item-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
  color: #525252;
  font-size: 13px;
}

.chunk-item p {
  margin: 0;
  color: #404040;
  font-size: 13px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.chunk-item-action {
  display: inline-block;
  margin-top: 8px;
  color: #2563eb;
  font-size: 12px;
}

.empty-inline {
  margin: 20px 0;
  text-align: center;
  color: #a3a3a3;
}

.modal-root {
  position: fixed;
  inset: 0;
  z-index: 3200;
  display: grid;
  place-items: center;
  padding: 16px;
}

.mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
}

.dialog {
  position: relative;
  width: min(720px, 100%);
  max-height: 90vh;
  overflow: auto;
  background: #fff;
  border-radius: 16px;
  border: 1px solid #e5e5e5;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.16);
  padding: 18px;
}

.dialog header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  gap: 12px;
}

.dialog h3 {
  margin: 0;
  font-size: 16px;
}

.x {
  border: 0;
  background: transparent;
  font-size: 22px;
  cursor: pointer;
  color: #737373;
  line-height: 1;
}

.modal-text {
  max-height: 60vh;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
