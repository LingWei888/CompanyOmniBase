<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import SideDrawer from '@/components/admin/SideDrawer.vue'
import PaginationBar from '@/components/admin/PaginationBar.vue'
import { useToast } from '@/composables/useToast'
import { listKnowledgeBaseOptions, type KnowledgeBase } from '@/api/knowledge'
import {
  deleteDocument,
  listDocuments,
  replaceDocument,
  requeueDocument,
  updateDocument,
  uploadDocument,
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

const drawerOpen = ref(false)
const mode = ref<'create' | 'edit' | 'replace'>('create')
const saving = ref(false)
const editing = ref<KbDocument | null>(null)
const form = reactive({
  kbId: '',
  title: '',
  file: null as File | null,
})

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
}

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

function openCreate() {
  mode.value = 'create'
  editing.value = null
  form.kbId = filterKbId.value || (kbs.value[0] ? String(kbs.value[0].id) : '')
  form.title = ''
  form.file = null
  drawerOpen.value = true
}

function openEdit(row: KbDocument) {
  mode.value = 'edit'
  editing.value = row
  form.kbId = String(row.kbId)
  form.title = row.title
  form.file = null
  drawerOpen.value = true
}

function openReplace(row: KbDocument) {
  mode.value = 'replace'
  editing.value = row
  form.kbId = String(row.kbId)
  form.title = row.title
  form.file = null
  drawerOpen.value = true
}

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  form.file = input.files?.[0] || null
  if (form.file && (mode.value === 'create' || !form.title)) {
    form.title = form.file.name.replace(/\.[^.]+$/, '')
  }
}

async function save() {
  saving.value = true
  try {
    if (mode.value === 'create') {
      if (!form.kbId) throw new Error('请选择知识库')
      if (!form.file) throw new Error('请选择文件')
      await uploadDocument(Number(form.kbId), form.file, form.title.trim() || undefined)
      toast.success('上传成功')
    } else if (mode.value === 'replace') {
      if (!editing.value) throw new Error('文档不存在')
      if (!form.file) throw new Error('请选择要替换的文件')
      await replaceDocument(editing.value.id, form.file, form.title.trim() || undefined)
      toast.success('替换成功，已重新入队')
    } else if (editing.value) {
      if (!form.title.trim()) throw new Error('请填写标题')
      await updateDocument(editing.value.id, form.title.trim())
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
  if (!confirm(`确认删除文档「${row.title}」？将同时删除 MinIO 对象。`)) return
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
  if (!confirm(`确认将文档「${row.title}」重新投递入库队列？`)) return
  try {
    await requeueDocument(row.id)
    toast.success('已重新投递')
    await load()
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '投递失败')
  }
}

function canRequeue(status: DocumentStatus) {
  return status === 'PENDING' || status === 'FAILED' || status === 'PROCESSING'
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
        <p>上传到 MinIO 后进入 PENDING，并异步投递 RabbitMQ 入库队列</p>
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
        <option value="PENDING">PENDING</option>
        <option value="PROCESSING">PROCESSING</option>
        <option value="READY">READY</option>
        <option value="FAILED">FAILED</option>
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
                <span class="tag" :class="row.status.toLowerCase()">{{ row.status }}</span>
              </td>
              <td class="actions">
                <button type="button" @click="openEdit(row)">编辑</button>
                <button type="button" @click="openReplace(row)">替换上传</button>
                <button
                  v-if="canRequeue(row.status)"
                  type="button"
                  @click="requeue(row)"
                >
                  重新入库
                </button>
                <button type="button" class="danger" @click="remove(row)">删除</button>
              </td>
            </tr>
            <tr v-if="!rows.length">
              <td colspan="7" class="empty">暂无文档</td>
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

    <SideDrawer
      :open="drawerOpen"
      :title="drawerTitle"
      @close="drawerOpen = false"
    >
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
      <template #footer>
        <button type="button" @click="drawerOpen = false">取消</button>
        <button type="button" class="primary" :disabled="saving" @click="save">
          {{ saving ? '提交中…' : '确定' }}
        </button>
      </template>
    </SideDrawer>
  </div>
</template>
