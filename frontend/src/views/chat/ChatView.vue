<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import AuthModal from '@/components/AuthModal.vue'
import ProfileModal from '@/components/ProfileModal.vue'
import PasswordModal from '@/components/PasswordModal.vue'
import { useUserAuthStore } from '@/stores/userAuth'
import { useSiteStore } from '@/stores/site'
import { useToast } from '@/composables/useToast'

interface ChatSession {
  id: string
  title: string
  updatedAt: string
}

interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
}

const auth = useUserAuthStore()
const site = useSiteStore()
const toast = useToast()

const sessions = ref<ChatSession[]>([
  { id: '1', title: '公司制度问答示例', updatedAt: '今天' },
  { id: '2', title: '产品手册检索示例', updatedAt: '昨天' },
  { id: '3', title: '新会话（占位）', updatedAt: '更早' },
])

const activeSessionId = ref('1')
const draft = ref('')
const selectedModelId = ref('')
const sidebarExpanded = ref(true)
const searchOpen = ref(false)
const searchKeyword = ref('')
const authOpen = ref(false)
const authMode = ref<'login' | 'register'>('login')
const menuOpen = ref(false)
const profileOpen = ref(false)
const passwordOpen = ref(false)
const logoutConfirmOpen = ref(false)
const attachInput = ref<HTMLInputElement | null>(null)
const pendingFileName = ref('')
const messagesEl = ref<HTMLElement | null>(null)
const isMobile = ref(false)

const messages = ref<ChatMessage[]>([])

const hasMessages = computed(() => messages.value.length > 0)

const displayName = computed(() => auth.user?.nickname || auth.user?.username || '')

const avatarLetter = computed(() => {
  const name = displayName.value.trim()
  return name ? name.slice(0, 1).toUpperCase() : '?'
})

const roleLabel = computed(() => {
  if (!auth.user) return '游客'
  return auth.user.role === 'ADMIN' ? '管理员' : '注册用户'
})

const planLabel = computed(() => {
  const plan = auth.user?.plan || 'FREE'
  return plan === 'FREE' ? 'Free' : plan
})

const heroTitle = computed(() => {
  if (auth.isLoggedIn && avatarLetter.value && avatarLetter.value !== '?') {
    return `尽情的问我问题吧，${avatarLetter.value}！`
  }
  return '尽情的问我问题吧！'
})

const selectedModelName = computed(() => {
  const id = Number(selectedModelId.value)
  const found = site.models.find((item) => item.id === id)
  return found?.name || '选择模型'
})

const filteredSessions = computed(() => {
  const key = searchKeyword.value.trim().toLowerCase()
  if (!key) return sessions.value
  return sessions.value.filter((item) => item.title.toLowerCase().includes(key))
})

watch(
  () => site.models,
  (list) => {
    if (!list.length) {
      selectedModelId.value = ''
      return
    }
    const first = list[0]
    if (!first) return
    if (!list.some((item) => String(item.id) === selectedModelId.value)) {
      selectedModelId.value = String(first.id)
    }
  },
  { immediate: true },
)

watch(messages, async () => {
  await nextTick()
  if (messagesEl.value) {
    messagesEl.value.scrollTop = messagesEl.value.scrollHeight
  }
}, { deep: true })

function selectSession(id: string) {
  activeSessionId.value = id
  messages.value = []
  pendingFileName.value = ''
  if (isMobile.value) {
    sidebarExpanded.value = false
  }
}

function createSession() {
  const id = String(Date.now())
  sessions.value.unshift({
    id,
    title: '新对话',
    updatedAt: '刚刚',
  })
  activeSessionId.value = id
  messages.value = []
  pendingFileName.value = ''
  draft.value = ''
  if (isMobile.value) {
    sidebarExpanded.value = false
  }
}

function sendMessage() {
  const content = draft.value.trim()
  if (!content && !pendingFileName.value) return
  if (!selectedModelId.value) {
    toast.error('请先选择模型')
    return
  }
  const text = pendingFileName.value
    ? `${content || '（已附加文件）'}\n[附件] ${pendingFileName.value}`
    : content
  messages.value.push({
    id: `u-${Date.now()}`,
    role: 'user',
    content: text,
  })
  draft.value = ''
  pendingFileName.value = ''
  messages.value.push({
    id: `a-${Date.now()}`,
    role: 'assistant',
    content: `（演示）已使用「${selectedModelName.value}」接收消息，后端问答尚未接入。`,
  })
  const active = sessions.value.find((item) => item.id === activeSessionId.value)
  if (active && active.title === '新对话') {
    active.title = text.slice(0, 24) || '新对话'
    active.updatedAt = '刚刚'
  }
}

function openAuth(mode: 'login' | 'register' = 'login') {
  authMode.value = mode
  authOpen.value = true
  menuOpen.value = false
}

function toggleUserMenu() {
  if (!auth.isLoggedIn) {
    openAuth('login')
    return
  }
  menuOpen.value = !menuOpen.value
}

function openProfile() {
  menuOpen.value = false
  profileOpen.value = true
}

function openPassword() {
  menuOpen.value = false
  passwordOpen.value = true
}

function askLogout() {
  menuOpen.value = false
  logoutConfirmOpen.value = true
}

async function confirmLogout() {
  logoutConfirmOpen.value = false
  await auth.logout()
  toast.success('已退出登录')
}

function onAttachClick() {
  attachInput.value?.click()
}

function onAttachChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  pendingFileName.value = file?.name || ''
  input.value = ''
}

function onDocClick(event: MouseEvent) {
  const target = event.target as HTMLElement | null
  if (!target?.closest('.user-panel') && !target?.closest('.user-menu')) {
    menuOpen.value = false
  }
}

function syncViewport() {
  const mobile = window.innerWidth <= 860
  const wasMobile = isMobile.value
  isMobile.value = mobile
  if (mobile && !wasMobile) {
    sidebarExpanded.value = false
  }
  if (!mobile && wasMobile) {
    sidebarExpanded.value = true
  }
}

onMounted(async () => {
  syncViewport()
  window.addEventListener('resize', syncViewport)
  document.addEventListener('click', onDocClick)
  await site.load()
  if (auth.isLoggedIn) {
    try {
      await auth.refreshProfile()
    } catch {
      auth.clearLocal()
    }
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', syncViewport)
  document.removeEventListener('click', onDocClick)
})
</script>

<template>
  <div
    class="gpt-shell"
    :class="{
      collapsed: !sidebarExpanded,
      mobile: isMobile,
      'mobile-open': isMobile && sidebarExpanded,
    }"
  >
    <div v-if="isMobile && sidebarExpanded" class="side-mask" @click="sidebarExpanded = false" />

    <aside class="sidebar">
      <div class="side-head">
        <template v-if="sidebarExpanded">
          <div class="title-wrap" :title="site.siteName">
            <strong>{{ site.siteName }}</strong>
          </div>
          <div class="head-actions">
            <button
              type="button"
              class="icon-btn"
              :class="{ on: searchOpen }"
              title="搜索对话"
              @click="searchOpen = !searchOpen"
            >
              <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2">
                <circle cx="11" cy="11" r="7" />
                <path d="M20 20l-3.5-3.5" />
              </svg>
            </button>
            <button type="button" class="icon-btn" title="收起侧栏" @click="sidebarExpanded = false">
              <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2">
                <rect x="3" y="4" width="18" height="16" rx="2" />
                <path d="M9 4v16" />
              </svg>
            </button>
          </div>
        </template>

        <button
          v-else
          type="button"
          class="collapse-logo"
          title="展开侧栏"
          @click="sidebarExpanded = true"
        >
          <img v-if="site.siteLogo" :src="site.siteLogo" class="logo-img" alt="logo" />
          <span v-else class="logo-fallback">{{ site.siteName.slice(0, 1) }}</span>
          <span class="expand-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2">
              <rect x="3" y="4" width="18" height="16" rx="2" />
              <path d="M9 4v16" />
            </svg>
          </span>
        </button>
      </div>

      <button v-if="sidebarExpanded" type="button" class="new-chat" @click="createSession">
        <span class="plus">＋</span>
        创建新对话
      </button>
      <button v-else type="button" class="rail-new" title="创建新对话" @click="createSession">＋</button>

      <div v-if="sidebarExpanded && searchOpen" class="search-box">
        <input v-model="searchKeyword" placeholder="搜索历史对话" />
      </div>

      <div class="history">
        <div v-if="sidebarExpanded" class="history-label">最近</div>
        <button
          v-for="session in filteredSessions"
          :key="session.id"
          type="button"
          class="history-item"
          :class="{ active: session.id === activeSessionId, compact: !sidebarExpanded }"
          :title="session.title"
          @click="selectSession(session.id)"
        >
          <span class="dot" />
          <span v-if="sidebarExpanded" class="history-text">
            <span class="history-title">{{ session.title }}</span>
          </span>
        </button>
      </div>

      <div class="side-footer">
        <button
          v-if="auth.isLoggedIn"
          type="button"
          class="user-panel"
          :class="{ compact: !sidebarExpanded }"
          @click.stop="toggleUserMenu"
        >
          <span class="avatar">
            <img v-if="auth.user?.avatarUrl" :src="auth.user.avatarUrl" alt="" />
            <span v-else>{{ avatarLetter }}</span>
          </span>
          <span v-if="sidebarExpanded" class="user-info">
            <strong>{{ displayName }}</strong>
            <small>{{ roleLabel }}</small>
          </span>
        </button>
        <button
          v-else
          type="button"
          class="user-panel guest"
          :class="{ compact: !sidebarExpanded }"
          @click="openAuth('login')"
        >
          <span class="avatar guest-avatar">?</span>
          <span v-if="sidebarExpanded" class="user-info">
            <strong>未登录</strong>
            <small>点击登录 / 注册</small>
          </span>
        </button>

        <div v-if="menuOpen && auth.isLoggedIn" class="user-menu" :class="{ compact: !sidebarExpanded }">
          <button type="button" @click="openProfile">修改账户信息</button>
          <button type="button" @click="openPassword">修改密码</button>
          <button type="button" class="danger" @click="askLogout">退出登录</button>
        </div>
      </div>
    </aside>

    <section class="main" :class="{ empty: !hasMessages }">
      <input ref="attachInput" type="file" class="hidden-file" @change="onAttachChange" />

      <header class="topbar">
        <button
          v-if="isMobile && !sidebarExpanded"
          type="button"
          class="mobile-expand"
          title="展开侧栏"
          @click="sidebarExpanded = true"
        >
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2">
            <rect x="3" y="4" width="18" height="16" rx="2" />
            <path d="M9 4v16" />
          </svg>
        </button>

        <select
          v-if="isMobile"
          v-model="selectedModelId"
          class="model-select top-model"
          :title="selectedModelName"
        >
          <option v-for="model in site.models" :key="model.id" :value="String(model.id)">
            {{ model.name }}
          </option>
        </select>

        <div class="topbar-spacer" />
        <div v-if="auth.isLoggedIn" class="plan-badge" title="当前权益">{{ planLabel }} 套餐</div>
      </header>

      <div v-if="!hasMessages" class="empty-stage">
        <h1 class="hero-title">{{ heroTitle }}</h1>
        <div class="composer-wrap">
          <div class="composer" :class="{ 'no-model': isMobile }">
            <button type="button" class="attach" title="添加文件" @click="onAttachClick">＋</button>
            <textarea
              v-model="draft"
              rows="1"
              placeholder="有问题，尽管问"
              @keydown.enter.exact.prevent="sendMessage"
            />
            <select
              v-if="!isMobile"
              v-model="selectedModelId"
              class="model-select"
              :title="selectedModelName"
            >
              <option v-for="model in site.models" :key="model.id" :value="String(model.id)">
                {{ model.name }}
              </option>
            </select>
            <button type="button" class="send" :disabled="!draft.trim() && !pendingFileName" @click="sendMessage">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
                <path d="M3.4 20.6l17.5-8.1c.8-.4.8-1.5 0-1.9L3.4 2.5c-.8-.4-1.6.4-1.3 1.2l2.5 6.5c.1.4.5.6.9.6h7.2c.4 0 .7.3.7.7s-.3.7-.7.7H5.5c-.4 0-.8.3-.9.6L2.1 19.4c-.3.8.5 1.6 1.3 1.2z" />
              </svg>
            </button>
          </div>
          <p v-if="pendingFileName" class="file-tip">已选择：{{ pendingFileName }}</p>
        </div>
      </div>

      <template v-else>
        <div ref="messagesEl" class="messages">
          <div
            v-for="message in messages"
            :key="message.id"
            class="message"
            :class="message.role"
          >
            <div class="bubble">{{ message.content }}</div>
          </div>
        </div>

        <div class="composer-wrap docked">
          <div class="composer" :class="{ 'no-model': isMobile }">
            <button type="button" class="attach" title="添加文件" @click="onAttachClick">＋</button>
            <textarea
              v-model="draft"
              rows="1"
              placeholder="继续提问…"
              @keydown.enter.exact.prevent="sendMessage"
            />
            <select
              v-if="!isMobile"
              v-model="selectedModelId"
              class="model-select"
              :title="selectedModelName"
            >
              <option v-for="model in site.models" :key="model.id" :value="String(model.id)">
                {{ model.name }}
              </option>
            </select>
            <button type="button" class="send" :disabled="!draft.trim() && !pendingFileName" @click="sendMessage">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
                <path d="M3.4 20.6l17.5-8.1c.8-.4.8-1.5 0-1.9L3.4 2.5c-.8-.4-1.6.4-1.3 1.2l2.5 6.5c.1.4.5.6.9.6h7.2c.4 0 .7.3.7.7s-.3.7-.7.7H5.5c-.4 0-.8.3-.9.6L2.1 19.4c-.3.8.5 1.6 1.3 1.2z" />
              </svg>
            </button>
          </div>
          <p v-if="pendingFileName" class="file-tip">已选择：{{ pendingFileName }}</p>
        </div>
      </template>
    </section>

    <AuthModal :open="authOpen" :initial-mode="authMode" @close="authOpen = false" />
    <ProfileModal :open="profileOpen" @close="profileOpen = false" />
    <PasswordModal :open="passwordOpen" @close="passwordOpen = false" />

    <Teleport to="body">
      <div v-if="logoutConfirmOpen" class="confirm-root">
        <div class="mask" @click="logoutConfirmOpen = false" />
        <div class="confirm-dialog">
          <h3>退出登录</h3>
          <p>确定要退出当前账号吗？</p>
          <div class="confirm-actions">
            <button type="button" @click="logoutConfirmOpen = false">取消</button>
            <button type="button" class="danger" @click="confirmLogout">退出登录</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.gpt-shell {
  --side: 280px;
  --rail: 56px;
  --bg: #fff;
  --side-bg: #f9f9f9;
  display: grid;
  grid-template-columns: var(--side) 1fr;
  height: 100vh;
  height: 100dvh;
  max-height: 100dvh;
  overflow: hidden;
  background: var(--bg);
  color: #0d0d0d;
  transition: grid-template-columns 0.2s ease;
}

.gpt-shell.collapsed {
  grid-template-columns: var(--rail) 1fr;
}

.side-mask {
  display: none;
}

.sidebar {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  height: 100%;
  max-height: 100%;
  overflow: hidden;
  background: var(--side-bg);
  border-right: 1px solid #ececec;
  padding: 10px 8px;
  position: relative;
  z-index: 20;
}

.side-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 44px;
  padding: 0 4px;
  flex-shrink: 0;
}

.title-wrap {
  display: flex;
  align-items: center;
  min-width: 0;
  flex: 1;
}

.title-wrap strong {
  font-size: 16px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.head-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.icon-btn,
.mobile-expand {
  width: 42px;
  height: 42px;
  border: 0;
  border-radius: 12px;
  background: transparent;
  color: #333;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
}

.icon-btn:hover,
.icon-btn.on,
.mobile-expand:hover {
  background: #ececec;
}

.collapse-logo {
  width: 40px;
  height: 40px;
  margin: 0 auto;
  border: 0;
  border-radius: 12px;
  background: transparent;
  cursor: pointer;
  position: relative;
  display: grid;
  place-items: center;
  padding: 0;
}

.collapse-logo .logo-img,
.collapse-logo .logo-fallback {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  object-fit: contain;
  display: grid;
  place-items: center;
  background: #0d0d0d;
  color: #fff;
  font-weight: 700;
  font-size: 13px;
}

.collapse-logo .logo-img {
  background: #fff;
  border: 1px solid #e8e8e8;
}

.collapse-logo .expand-icon {
  position: absolute;
  inset: 0;
  display: none;
  place-items: center;
  background: #ececec;
  border-radius: 12px;
  color: #333;
}

.collapse-logo:hover .logo-img,
.collapse-logo:hover .logo-fallback {
  opacity: 0;
}

.collapse-logo:hover .expand-icon {
  display: grid;
}

.new-chat {
  margin-top: 8px;
  width: 100%;
  border: 0;
  border-radius: 12px;
  background: transparent;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  font: inherit;
  color: inherit;
  text-align: left;
  flex-shrink: 0;
}

.new-chat:hover {
  background: #ececec;
}

.new-chat .plus {
  width: 20px;
  text-align: center;
  font-size: 18px;
}

.rail-new {
  width: 40px;
  height: 40px;
  margin: 8px auto 0;
  border: 0;
  border-radius: 12px;
  background: transparent;
  cursor: pointer;
  font-size: 20px;
  flex-shrink: 0;
}

.rail-new:hover {
  background: #ececec;
}

.search-box {
  margin-top: 8px;
  padding: 0 4px;
  flex-shrink: 0;
}

.search-box input {
  width: 100%;
  border: 1px solid #e5e5e5;
  background: #fff;
  border-radius: 10px;
  padding: 8px 10px;
  font: inherit;
}

.history {
  flex: 1 1 auto;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;
}

.history-label {
  position: sticky;
  top: 0;
  z-index: 1;
  padding: 6px 12px;
  font-size: 12px;
  color: #8a8a8a;
  background: var(--side-bg);
}

.history-item {
  border: 0;
  background: transparent;
  border-radius: 10px;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  text-align: left;
  font: inherit;
  color: inherit;
  flex-shrink: 0;
}

.history-item.compact {
  width: 40px;
  height: 40px;
  margin: 0 auto;
  justify-content: center;
  padding: 0;
}

.history-item:hover,
.history-item.active {
  background: #ececec;
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #b0b0b0;
  flex-shrink: 0;
}

.history-item.active .dot {
  background: #0d0d0d;
}

.history-text {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.history-title {
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.side-footer {
  position: relative;
  padding-top: 8px;
  border-top: 1px solid #ececec;
  margin-top: 8px;
  flex-shrink: 0;
}

.user-panel {
  width: 100%;
  border: 0;
  background: transparent;
  border-radius: 12px;
  padding: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  font: inherit;
  text-align: left;
}

.user-panel:hover {
  background: #ececec;
}

.user-panel.compact {
  width: 40px;
  height: 40px;
  margin: 0 auto;
  justify-content: center;
  padding: 0;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  background: #0d0d0d;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.guest-avatar {
  background: #e8e8e8;
  color: #666;
  border: 1px dashed #cfcfcf;
}

.user-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-info strong {
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-info small {
  font-size: 12px;
  color: #8a8a8a;
}

.user-menu {
  position: absolute;
  left: 8px;
  right: 8px;
  bottom: calc(100% + 8px);
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 14px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
  padding: 6px;
  display: flex;
  flex-direction: column;
  z-index: 20;
}

.user-menu.compact {
  left: 4px;
  right: auto;
  width: 180px;
}

.user-menu button {
  border: 0;
  background: transparent;
  text-align: left;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  font: inherit;
}

.user-menu button:hover {
  background: #f3f3f3;
}

.user-menu .danger {
  color: #b91c1c;
}

.main {
  min-width: 0;
  min-height: 0;
  height: 100%;
  overflow: hidden;
  display: grid;
  grid-template-rows: 56px 1fr auto;
  background: #fff;
  position: relative;
}

.main.empty {
  grid-template-rows: 56px 1fr;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 16px;
  gap: 10px;
  min-height: 56px;
}

.topbar-spacer {
  flex: 1;
}

.top-model {
  max-width: min(160px, 42vw);
}

.plan-badge {
  border: 1px solid #e8e8e8;
  background: #f7f7f7;
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 12px;
  color: #444;
  font-weight: 600;
}

.empty-stage {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 48px;
  min-height: 0;
  overflow: auto;
  padding: 24px 16px 48px;
}

.hero-title {
  margin: 0;
  text-align: center;
  font-size: clamp(28px, 5vw, 40px);
  font-weight: 600;
  letter-spacing: 0.02em;
  line-height: 1.35;
  max-width: 780px;
}

.messages {
  overflow: auto;
  min-height: 0;
  padding: 8px 0 24px;
}

.message {
  width: min(720px, calc(100% - 32px));
  margin: 0 auto 14px;
}

.bubble {
  padding: 12px 16px;
  border-radius: 18px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 15px;
}

.message.user .bubble {
  background: #f4f4f4;
  margin-left: auto;
  width: fit-content;
  max-width: 100%;
}

.message.assistant .bubble {
  background: transparent;
  padding-left: 0;
  padding-right: 0;
}

.composer-wrap {
  width: min(760px, calc(100% - 24px));
  margin: 0 auto;
}

.composer-wrap.docked {
  padding-bottom: calc(14px + env(safe-area-inset-bottom));
}

.composer {
  display: grid;
  grid-template-columns: auto 1fr auto auto;
  gap: 8px;
  align-items: end;
  border: 1px solid #e5e5e5;
  background: #fff;
  border-radius: 28px;
  padding: 10px 10px 10px 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.04);
}

.composer.no-model {
  grid-template-columns: auto 1fr auto;
}

.attach,
.send {
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: 50%;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.attach {
  background: transparent;
  color: #444;
  font-size: 20px;
}

.attach:hover {
  background: #f2f2f2;
}

.model-select {
  border: 1px solid #e8e8e8;
  background: #f7f7f7;
  border-radius: 999px;
  padding: 0 12px;
  height: 36px;
  font: inherit;
  font-size: 13px;
  color: #262626;
  max-width: 150px;
  cursor: pointer;
}

.send {
  background: #0d0d0d;
  color: #fff;
}

.send:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.composer textarea {
  border: 0;
  outline: none;
  resize: none;
  font: inherit;
  min-height: 36px;
  max-height: 160px;
  padding: 8px 4px;
  background: transparent;
  line-height: 1.45;
}

.hidden-file {
  display: none;
}

.file-tip {
  text-align: center;
  font-size: 12px;
  color: #8a8a8a;
  margin: 8px 0 0;
}

.confirm-root {
  position: fixed;
  inset: 0;
  z-index: 3300;
  display: grid;
  place-items: center;
  padding: 16px;
}

.confirm-root .mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
}

.confirm-dialog {
  position: relative;
  width: min(380px, 100%);
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 20px 48px rgba(0, 0, 0, 0.16);
}

.confirm-dialog h3 {
  margin: 0 0 8px;
}

.confirm-dialog p {
  margin: 0 0 16px;
  color: #525252;
}

.confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.confirm-actions button {
  border: 1px solid #d4d4d4;
  background: #fff;
  border-radius: 10px;
  padding: 8px 14px;
  cursor: pointer;
  font: inherit;
}

.confirm-actions .danger {
  background: #b91c1c;
  border-color: #b91c1c;
  color: #fff;
}

@media (max-width: 860px) {
  .gpt-shell.mobile,
  .gpt-shell.mobile.collapsed {
    grid-template-columns: 1fr;
  }

  .gpt-shell.mobile.collapsed .sidebar {
    display: none;
  }

  .side-mask {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.28);
    z-index: 30;
  }

  .gpt-shell.mobile-open .sidebar {
    display: flex;
    position: fixed;
    inset: 0 auto 0 0;
    width: min(300px, 88vw);
    height: 100%;
    max-height: 100dvh;
    overflow: hidden;
    z-index: 40;
    box-shadow: 8px 0 28px rgba(0, 0, 0, 0.12);
  }

  .empty-stage {
    gap: 36px;
    padding: 16px 12px 32px;
  }

  .hero-title {
    font-size: 26px;
  }

  .message {
    width: calc(100% - 20px);
  }

  .composer-wrap {
    width: calc(100% - 20px);
  }
}
</style>
