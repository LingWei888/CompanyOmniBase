<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import SideDrawer from '@/components/admin/SideDrawer.vue'
import PaginationBar from '@/components/admin/PaginationBar.vue'
import { useToast } from '@/composables/useToast'
import {
  createModel,
  deleteModel,
  listModels,
  updateModel,
  type LlmModel,
} from '@/api/model'

const toast = useToast()
const loading = ref(false)
const rows = ref<LlmModel[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const totalPages = ref(0)

const drawerOpen = ref(false)
const saving = ref(false)
const editing = ref<LlmModel | null>(null)
const form = reactive({
  name: '',
  protocol: 'OPENAI' as const,
  baseUrl: '',
  apiKey: '',
  modelName: '',
  enabled: true,
  remark: '',
})

async function load() {
  loading.value = true
  try {
    const data = await listModels(page.value, size.value)
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

function openCreate() {
  editing.value = null
  form.name = ''
  form.protocol = 'OPENAI'
  form.baseUrl = ''
  form.apiKey = ''
  form.modelName = ''
  form.enabled = true
  form.remark = ''
  drawerOpen.value = true
}

function openEdit(row: LlmModel) {
  editing.value = row
  form.name = row.name
  form.protocol = 'OPENAI'
  form.baseUrl = row.baseUrl
  form.apiKey = row.apiKey
  form.modelName = row.modelName || ''
  form.enabled = row.enabled
  form.remark = row.remark || ''
  drawerOpen.value = true
}

async function save() {
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      protocol: form.protocol,
      baseUrl: form.baseUrl.trim(),
      apiKey: form.apiKey.trim(),
      modelName: form.modelName.trim() || undefined,
      enabled: form.enabled,
      remark: form.remark.trim() || undefined,
    }
    if (!payload.name || !payload.baseUrl || !payload.apiKey) {
      throw new Error('名称、URL、Key 为必填项')
    }
    if (editing.value) {
      await updateModel(editing.value.id, payload)
      toast.success('保存成功')
    } else {
      await createModel(payload)
      toast.success('创建成功')
    }
    drawerOpen.value = false
    await load()
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function remove(row: LlmModel) {
  if (!confirm(`确认删除模型「${row.name}」？`)) return
  try {
    await deleteModel(row.id)
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

function maskKey(key: string) {
  if (!key) return '-'
  if (key.length <= 8) return '****'
  return `${key.slice(0, 4)}****${key.slice(-4)}`
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>模型管理</h2>
        <p>当前仅支持 OpenAI 兼容接口格式</p>
      </div>
      <button type="button" class="primary" @click="openCreate">新增模型</button>
    </div>

    <p v-if="loading">加载中…</p>

    <template v-else>
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>名称</th>
              <th>对接方式</th>
              <th>Base URL</th>
              <th>模型标识</th>
              <th>API Key</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td>{{ row.name }}</td>
              <td>{{ row.protocol }}</td>
              <td>{{ row.baseUrl }}</td>
              <td>{{ row.modelName || '-' }}</td>
              <td>{{ maskKey(row.apiKey) }}</td>
              <td>
                <span class="tag" :class="row.enabled ? 'on' : 'off'">
                  {{ row.enabled ? '启用' : '禁用' }}
                </span>
              </td>
              <td class="actions">
                <button type="button" @click="openEdit(row)">编辑</button>
                <button type="button" class="danger" @click="remove(row)">删除</button>
              </td>
            </tr>
            <tr v-if="!rows.length">
              <td colspan="7" class="empty">暂无模型配置</td>
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
      :title="editing ? '编辑模型' : '新增模型'"
      width="480px"
      @close="drawerOpen = false"
    >
      <label>
        名称
        <input v-model="form.name" maxlength="128" placeholder="例如：默认问答模型" />
      </label>
      <label>
        对接方式
        <select v-model="form.protocol">
          <option value="OPENAI">OpenAI 兼容</option>
        </select>
      </label>
      <label>
        Base URL
        <input v-model="form.baseUrl" maxlength="512" placeholder="https://api.openai.com/v1" />
      </label>
      <label>
        API Key
        <input v-model="form.apiKey" maxlength="512" placeholder="sk-..." />
      </label>
      <label>
        模型标识（可选）
        <input v-model="form.modelName" maxlength="128" placeholder="gpt-4o-mini" />
      </label>
      <label>
        备注
        <textarea v-model="form.remark" rows="3" maxlength="256" />
      </label>
      <label class="switch">
        <input v-model="form.enabled" type="checkbox" />
        启用
      </label>
      <template #footer>
        <button type="button" @click="drawerOpen = false">取消</button>
        <button type="button" class="primary" :disabled="saving" @click="save">
          {{ saving ? '保存中…' : '保存' }}
        </button>
      </template>
    </SideDrawer>
  </div>
</template>
