<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useUserAuthStore } from '@/stores/userAuth'
import { useToast } from '@/composables/useToast'

const props = defineProps<{
  open: boolean
  initialMode?: 'login' | 'register'
}>()

const emit = defineEmits<{
  close: []
}>()

const auth = useUserAuthStore()
const toast = useToast()
const mode = ref<'login' | 'register'>('login')
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
  nickname: '',
})

watch(
  () => props.open,
  (open) => {
    if (open) {
      mode.value = props.initialMode || 'login'
      form.username = ''
      form.password = ''
      form.nickname = ''
      document.body.style.overflow = 'hidden'
    } else {
      document.body.style.overflow = ''
    }
  },
)

async function submit() {
  const username = form.username.trim()
  const password = form.password
  if (!username || !password) {
    toast.error('请填写用户名和密码')
    return
  }
  loading.value = true
  try {
    if (mode.value === 'login') {
      await auth.login(username, password)
      toast.success('登录成功')
    } else {
      await auth.register(username, password, form.nickname.trim() || undefined)
      toast.success('注册成功，已自动登录')
    }
    emit('close')
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '操作失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="auth-root">
      <div class="mask" @click="emit('close')" />
      <div class="dialog" role="dialog" aria-modal="true">
        <header class="head">
          <div class="tabs">
            <button type="button" :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</button>
            <button type="button" :class="{ active: mode === 'register' }" @click="mode = 'register'">注册</button>
          </div>
          <button type="button" class="x" aria-label="关闭" @click="emit('close')">×</button>
        </header>

        <form class="body" @submit.prevent="submit">
          <label>
            用户名
            <input v-model="form.username" autocomplete="username" maxlength="32" placeholder="3-32 位用户名" />
          </label>
          <label v-if="mode === 'register'">
            昵称（可选）
            <input v-model="form.nickname" maxlength="64" placeholder="默认使用用户名" />
          </label>
          <label>
            密码
            <input
              v-model="form.password"
              type="password"
              autocomplete="current-password"
              maxlength="64"
              :placeholder="mode === 'register' ? '至少 6 位' : '请输入密码'"
            />
          </label>

          <button type="submit" class="submit" :disabled="loading">
            {{ loading ? '提交中…' : mode === 'login' ? '登录' : '注册并登录' }}
          </button>
          <p class="tip">演示账号：user / user123（默认 Free 套餐）</p>
        </form>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.auth-root {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: grid;
  place-items: center;
  padding: 16px;
}

.mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.42);
}

.dialog {
  position: relative;
  width: min(420px, 100%);
  background: #fff;
  border-radius: 16px;
  border: 1px solid #e5e5e5;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.18);
  overflow: hidden;
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 0;
  gap: 12px;
}

.tabs {
  display: flex;
  gap: 6px;
  background: #f5f5f5;
  border-radius: 10px;
  padding: 4px;
}

.tabs button {
  border: 0;
  background: transparent;
  border-radius: 8px;
  padding: 8px 14px;
  cursor: pointer;
  color: #737373;
  font: inherit;
}

.tabs button.active {
  background: #fff;
  color: #171717;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.x {
  border: 0;
  background: transparent;
  font-size: 24px;
  line-height: 1;
  cursor: pointer;
  color: #737373;
}

.body {
  padding: 18px 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 14px;
  color: #404040;
}

input {
  border: 1px solid #d4d4d4;
  border-radius: 10px;
  padding: 10px 12px;
  font: inherit;
}

.submit {
  margin-top: 4px;
  border: 0;
  border-radius: 10px;
  background: #171717;
  color: #fff;
  padding: 11px 14px;
  font: inherit;
  cursor: pointer;
}

.submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.tip {
  margin: 0;
  text-align: center;
  color: #a3a3a3;
  font-size: 12px;
}
</style>
