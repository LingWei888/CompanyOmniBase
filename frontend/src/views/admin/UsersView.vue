<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import SideDrawer from '@/components/admin/SideDrawer.vue'
import PaginationBar from '@/components/admin/PaginationBar.vue'
import { useToast } from '@/composables/useToast'
import {
  createAppUser,
  deleteAppUser,
  listAppUsers,
  resetAppUserPassword,
  updateAppUser,
  type ManagedAppUser,
} from '@/api/users'

const toast = useToast()
const loading = ref(false)
const rows = ref<ManagedAppUser[]>([])
const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const totalPages = ref(0)

const drawerOpen = ref(false)
const passwordDrawerOpen = ref(false)
const saving = ref(false)
const editing = ref<ManagedAppUser | null>(null)
const passwordTarget = ref<ManagedAppUser | null>(null)

const form = reactive({
  username: '',
  password: '',
  nickname: '',
  enabled: true,
})

const passwordForm = reactive({
  password: '',
})

async function load() {
  loading.value = true
  try {
    const data = await listAppUsers({
      keyword: keyword.value.trim() || undefined,
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
watch(keyword, () => {
  if (page.value !== 1) {
    page.value = 1
  } else {
    load()
  }
})

function openCreate() {
  editing.value = null
  form.username = ''
  form.password = ''
  form.nickname = ''
  form.enabled = true
  drawerOpen.value = true
}

function openEdit(row: ManagedAppUser) {
  editing.value = row
  form.username = row.username
  form.password = ''
  form.nickname = row.nickname
  form.enabled = row.enabled
  drawerOpen.value = true
}

function openResetPassword(row: ManagedAppUser) {
  passwordTarget.value = row
  passwordForm.password = ''
  passwordDrawerOpen.value = true
}

async function save() {
  saving.value = true
  try {
    if (editing.value) {
      await updateAppUser(editing.value.id, {
        nickname: form.nickname.trim() || undefined,
        enabled: form.enabled,
      })
      toast.success('保存成功')
    } else {
      if (!form.username.trim()) throw new Error('请填写用户名')
      if (!form.password) throw new Error('请填写密码')
      await createAppUser({
        username: form.username.trim(),
        password: form.password,
        nickname: form.nickname.trim() || undefined,
        enabled: form.enabled,
      })
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

async function savePassword() {
  if (!passwordTarget.value) return
  if (!passwordForm.password || passwordForm.password.length < 6) {
    toast.error('新密码至少 6 位')
    return
  }
  saving.value = true
  try {
    await resetAppUserPassword(passwordTarget.value.id, passwordForm.password)
    toast.success('密码已重置')
    passwordDrawerOpen.value = false
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '重置失败')
  } finally {
    saving.value = false
  }
}

async function remove(row: ManagedAppUser) {
  if (!confirm(`确认删除用户「${row.username}」？`)) return
  try {
    await deleteAppUser(row.id)
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
        <h2>用户管理</h2>
        <p>管理前台普通用户（与站长账号分表，站长不能登录聊天）</p>
      </div>
      <button type="button" class="primary" @click="openCreate">新增用户</button>
    </div>

    <div class="filters">
      <input v-model="keyword" type="search" placeholder="按用户名搜索" />
    </div>

    <p v-if="loading">加载中…</p>

    <template v-else>
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>用户名</th>
              <th>昵称</th>
              <th>套餐</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td>{{ row.id }}</td>
              <td>{{ row.username }}</td>
              <td>{{ row.nickname }}</td>
              <td>{{ row.plan }}</td>
              <td>
                <span class="tag" :class="row.enabled ? 'on' : 'off'">
                  {{ row.enabled ? '启用' : '禁用' }}
                </span>
              </td>
              <td class="actions">
                <button type="button" @click="openEdit(row)">编辑</button>
                <button type="button" @click="openResetPassword(row)">重置密码</button>
                <button type="button" class="danger" @click="remove(row)">删除</button>
              </td>
            </tr>
            <tr v-if="!rows.length">
              <td colspan="6" class="empty">暂无用户</td>
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
      :title="editing ? '编辑用户' : '新增用户'"
      @close="drawerOpen = false"
    >
      <label>
        用户名
        <input
          v-model="form.username"
          maxlength="32"
          :disabled="!!editing"
          placeholder="3-32 字符"
        />
      </label>
      <label v-if="!editing">
        密码
        <input v-model="form.password" type="password" maxlength="64" placeholder="至少 6 位" />
      </label>
      <label>
        昵称
        <input v-model="form.nickname" maxlength="64" placeholder="可选，默认用户名" />
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

    <SideDrawer
      :open="passwordDrawerOpen"
      title="重置密码"
      @close="passwordDrawerOpen = false"
    >
      <p v-if="passwordTarget" class="hint">用户：{{ passwordTarget.username }}</p>
      <label>
        新密码
        <input v-model="passwordForm.password" type="password" maxlength="64" placeholder="至少 6 位" />
      </label>
      <template #footer>
        <button type="button" @click="passwordDrawerOpen = false">取消</button>
        <button type="button" class="primary" :disabled="saving" @click="savePassword">
          {{ saving ? '提交中…' : '确定' }}
        </button>
      </template>
    </SideDrawer>
  </div>
</template>
