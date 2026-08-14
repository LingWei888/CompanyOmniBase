<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdminAuthStore } from '@/stores/adminAuth'

const auth = useAdminAuthStore()
const router = useRouter()
const route = useRoute()

const username = ref('admin')
const password = ref('admin123')
const loading = ref(false)
const error = ref('')

async function onSubmit() {
  error.value = ''
  loading.value = true
  try {
    await auth.login(username.value.trim(), password.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/admin/dashboard'
    await router.replace(redirect)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <form class="card" @submit.prevent="onSubmit">
      <h1>站长登录</h1>
      <p class="sub">进入知识库管理后台</p>

      <label>
        用户名
        <input v-model="username" autocomplete="username" required />
      </label>
      <label>
        密码
        <input v-model="password" type="password" autocomplete="current-password" required />
      </label>

      <p v-if="error" class="error">{{ error }}</p>

      <button type="submit" :disabled="loading">
        {{ loading ? '登录中…' : '登录' }}
      </button>
      <p class="tip">默认账号 admin / admin123</p>
    </form>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background:
    radial-gradient(circle at top left, rgba(20, 20, 20, 0.08), transparent 40%),
    linear-gradient(160deg, #f4f4f5, #e9e9eb 55%, #f7f7f8);
}

.card {
  width: min(400px, calc(100% - 32px));
  background: #fff;
  border: 1px solid #e4e4e7;
  border-radius: 16px;
  padding: 28px 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

h1 {
  margin: 0;
  font-size: 24px;
}

.sub {
  margin: -6px 0 4px;
  color: #71717a;
}

label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 14px;
  color: #3f3f46;
}

input {
  border: 1px solid #d4d4d8;
  border-radius: 10px;
  padding: 10px 12px;
  font: inherit;
}

button {
  margin-top: 4px;
  border: 0;
  border-radius: 10px;
  background: #18181b;
  color: #fff;
  padding: 11px 14px;
  font: inherit;
  cursor: pointer;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error {
  margin: 0;
  color: #b91c1c;
  font-size: 13px;
}

.tip {
  margin: 0;
  text-align: center;
  color: #a1a1aa;
  font-size: 12px;
}
</style>
