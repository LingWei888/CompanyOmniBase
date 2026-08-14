<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdminAuthStore } from '@/stores/adminAuth'
import { useSiteStore } from '@/stores/site'
import { useToast } from '@/composables/useToast'

const auth = useAdminAuthStore()
const site = useSiteStore()
const router = useRouter()
const route = useRoute()
const toast = useToast()

const username = ref('admin')
const password = ref('admin123')
const loading = ref(false)

async function onSubmit() {
  loading.value = true
  try {
    await auth.login(username.value.trim(), password.value)
    toast.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/admin/dashboard'
    await router.replace(redirect)
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '登录失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  site.load()
})
</script>

<template>
  <div class="login-page">
    <form class="card" @submit.prevent="onSubmit">
      <div class="brand">
        <img v-if="site.siteLogo" :src="site.siteLogo" alt="logo" />
        <div>
          <h1>{{ site.siteName }}</h1>
          <p class="sub">{{ site.siteDescription || '进入知识库管理后台' }}</p>
        </div>
      </div>

      <label>
        用户名
        <input v-model="username" autocomplete="username" required />
      </label>
      <label>
        密码
        <input v-model="password" type="password" autocomplete="current-password" required />
      </label>

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
  min-height: 100dvh;
  display: grid;
  place-items: center;
  padding: 16px;
  background:
    radial-gradient(circle at top left, rgba(20, 20, 20, 0.08), transparent 40%),
    linear-gradient(160deg, #f4f4f5, #e9e9eb 55%, #f7f7f8);
}

.card {
  width: min(420px, 100%);
  background: #fff;
  border: 1px solid #e4e4e7;
  border-radius: 16px;
  padding: 28px 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.brand {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 4px;
}

.brand img {
  width: 48px;
  height: 48px;
  object-fit: contain;
  border-radius: 12px;
  border: 1px solid #eee;
  background: #fafafa;
  flex-shrink: 0;
}

h1 {
  margin: 0;
  font-size: clamp(18px, 4.5vw, 22px);
  line-height: 1.3;
}

.sub {
  margin: 4px 0 0;
  color: #71717a;
  font-size: 13px;
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
  width: 100%;
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

.tip {
  margin: 0;
  text-align: center;
  color: #a1a1aa;
  font-size: 12px;
}
</style>
