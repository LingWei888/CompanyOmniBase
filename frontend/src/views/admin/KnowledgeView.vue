<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import SideDrawer from '@/components/admin/SideDrawer.vue'
import PaginationBar from '@/components/admin/PaginationBar.vue'
import { useToast } from '@/composables/useToast'
import {
  createKnowledgeBase,
  deleteKnowledgeBase,
  listKnowledgeBases,
  updateKnowledgeBase,
  type KnowledgeBase,
} from '@/api/knowledge'

const toast = useToast()
const loading = ref(false)
const rows = ref<KnowledgeBase[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const totalPages = ref(0)

const drawerOpen = ref(false)
const saving = ref(false)
const editing = ref<KnowledgeBase | null>(null)
const form = reactive({
  name: '',
  description: '',
  enabled: true,
})

async function load() {
  loading.value = true
  try {
    const data = await listKnowledgeBases(page.value, size.value)
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
  form.description = ''
  form.enabled = true
  drawerOpen.value = true
}

function openEdit(row: KnowledgeBase) {
  editing.value = row
  form.name = row.name
  form.description = row.description || ''
  form.enabled = row.enabled
  drawerOpen.value = true
}

async function save() {
  if (!form.name.trim()) {
    toast.error('请填写知识库名称')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      description: form.description.trim(),
      enabled: form.enabled,
    }
    if (editing.value) {
      await updateKnowledgeBase(editing.value.id, payload)
      toast.success('保存成功')
    } else {
      await createKnowledgeBase(payload)
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

async function remove(row: KnowledgeBase) {
  if (!confirm(`确认删除知识库「${row.name}」？`)) return
  try {
    await deleteKnowledgeBase(row.id)
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

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>知识库管理</h2>
        <p>创建和管理知识库元数据</p>
      </div>
      <button type="button" class="primary" @click="openCreate">新增知识库</button>
    </div>

    <p v-if="loading">加载中…</p>

    <template v-else>
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>名称</th>
              <th>描述</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td>{{ row.id }}</td>
              <td>{{ row.name }}</td>
              <td>{{ row.description || '-' }}</td>
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
              <td colspan="5" class="empty">暂无知识库</td>
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
      :title="editing ? '编辑知识库' : '新增知识库'"
      @close="drawerOpen = false"
    >
      <label>
        名称
        <input v-model="form.name" maxlength="128" placeholder="例如：公司制度" />
      </label>
      <label>
        描述
        <textarea v-model="form.description" rows="4" maxlength="512" placeholder="可选" />
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
