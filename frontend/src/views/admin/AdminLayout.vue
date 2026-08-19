<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { useAdminAuthStore } from '@/stores/adminAuth'
import { useSiteStore } from '@/stores/site'

const auth = useAdminAuthStore()
const site = useSiteStore()
const router = useRouter()
const route = useRoute()
const menuOpen = ref(false)

async function logout() {
  await auth.logout()
  await router.replace({ name: 'admin-login' })
}

function closeMenu() {
  menuOpen.value = false
}

watch(
  () => route.fullPath,
  () => {
    menuOpen.value = false
  },
)

watch(menuOpen, (open) => {
  document.body.style.overflow = open ? 'hidden' : ''
})

function onResize() {
  if (window.innerWidth > 900) {
    menuOpen.value = false
  }
}

onMounted(async () => {
  window.addEventListener('resize', onResize)
  await site.load()
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  document.body.style.overflow = ''
})
</script>

<template>
  <div class="admin-shell" :class="{ 'menu-open': menuOpen }">
    <div v-if="menuOpen" class="nav-mask" @click="closeMenu" />

    <aside class="side">
      <div class="brand-row">
        <div class="brand">
          <img v-if="site.siteLogo" :src="site.siteLogo" class="brand-logo" alt="logo" />
          <div v-else class="brand-fallback">{{ site.siteName.slice(0, 1) }}</div>
          <div class="brand-text">
            <strong>{{ site.siteName }}</strong>
            <small>管理后台</small>
          </div>
        </div>
        <button type="button" class="icon-btn mobile-only" aria-label="关闭菜单" @click="closeMenu">×</button>
      </div>
      <nav>
        <RouterLink to="/admin/dashboard" @click="closeMenu">概览</RouterLink>
        <RouterLink to="/admin/users" @click="closeMenu">用户</RouterLink>
        <RouterLink to="/admin/knowledge" @click="closeMenu">知识库</RouterLink>
        <RouterLink to="/admin/documents" @click="closeMenu">文档</RouterLink>
        <RouterLink to="/admin/ingest-ops" @click="closeMenu">入库运维</RouterLink>
        <RouterLink to="/admin/models" @click="closeMenu">模型</RouterLink>
        <RouterLink to="/admin/settings" @click="closeMenu">系统设置</RouterLink>
      </nav>
      <button class="logout" type="button" @click="logout">退出登录</button>
    </aside>

    <section class="content">
      <header class="bar">
        <div class="bar-left">
          <button type="button" class="icon-btn mobile-only" aria-label="打开菜单" @click="menuOpen = true">☰</button>
          <div class="bar-desc">{{ site.siteDescription }}</div>
        </div>
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
  grid-template-columns: 240px 1fr;
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
  min-width: 0;
}

.brand-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.brand {
  display: flex;
  gap: 10px;
  align-items: center;
  min-width: 0;
  padding: 0 4px;
}

.brand-logo {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  object-fit: contain;
  background: #fff;
  flex-shrink: 0;
}

.brand-fallback {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: #fff;
  color: #111;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  flex-shrink: 0;
}

.brand-text {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-text strong {
  font-size: 14px;
  line-height: 1.3;
  word-break: break-word;
}

.brand-text small {
  color: #a3a3a3;
  font-size: 12px;
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
  min-width: 0;
}

.bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #e5e5e5;
  gap: 12px;
}

.bar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.bar-desc {
  color: #737373;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user {
  color: #525252;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 40vw;
}

main {
  padding: 20px;
  min-width: 0;
}

.icon-btn {
  border: 1px solid #d4d4d4;
  background: #fff;
  border-radius: 8px;
  width: 36px;
  height: 36px;
  cursor: pointer;
  font-size: 18px;
  line-height: 1;
  padding: 0;
}

.side .icon-btn {
  border-color: #3f3f3f;
  background: transparent;
  color: #f5f5f5;
}

.mobile-only {
  display: none;
}

.nav-mask {
  display: none;
}

@media (max-width: 900px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .mobile-only {
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }

  .nav-mask {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.4);
    z-index: 30;
  }

  .side {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    width: min(280px, 86vw);
    z-index: 40;
    transform: translateX(-105%);
    transition: transform 0.2s ease;
  }

  .admin-shell.menu-open .side {
    transform: translateX(0);
  }

  main {
    padding: 12px;
  }

  .bar {
    padding: 0 12px;
  }
}
</style>
