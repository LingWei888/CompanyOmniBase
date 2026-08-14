<script setup lang="ts">
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useAdminAuthStore } from '@/stores/adminAuth'

const auth = useAdminAuthStore()
const router = useRouter()

async function logout() {
  await auth.logout()
  await router.replace({ name: 'admin-login' })
}
</script>

<template>
  <div class="admin-shell">
    <aside class="side">
      <div class="brand">KB Admin</div>
      <nav>
        <RouterLink to="/admin/dashboard">概览</RouterLink>
        <RouterLink to="/admin/knowledge">知识库</RouterLink>
        <RouterLink to="/admin/documents">文档</RouterLink>
      </nav>
      <button class="logout" type="button" @click="logout">退出登录</button>
    </aside>
    <section class="content">
      <header class="bar">
        <div>管理后台</div>
        <div class="user">{{ auth.user?.nickname || auth.user?.username || 'Admin' }}</div>
      </header>
      <main>
        <RouterView />
      </main>
    </section>
  </div>
</template>

<style scoped>
.admin-shell {
  display: grid;
  grid-template-columns: 220px 1fr;
  min-height: 100vh;
  background: #f5f5f5;
}

.side {
  background: #111;
  color: #f5f5f5;
  padding: 20px 14px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.brand {
  font-weight: 700;
  letter-spacing: 0.04em;
  padding: 0 8px;
}

nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}

nav a {
  color: #d4d4d4;
  text-decoration: none;
  padding: 10px 12px;
  border-radius: 8px;
}

nav a.router-link-active,
nav a:hover {
  background: #262626;
  color: #fff;
}

.logout {
  border: 1px solid #3f3f3f;
  background: transparent;
  color: inherit;
  border-radius: 8px;
  padding: 10px 12px;
  cursor: pointer;
}

.content {
  display: grid;
  grid-template-rows: 56px 1fr;
}

.bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #e5e5e5;
}

.user {
  color: #525252;
  font-size: 14px;
}

main {
  padding: 20px;
}

@media (max-width: 860px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .side {
    flex-direction: row;
    align-items: center;
    gap: 12px;
  }

  nav {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .logout {
    margin-left: auto;
  }
}
</style>
