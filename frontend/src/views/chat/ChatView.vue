<script setup lang="ts">
import { computed, ref } from 'vue'

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

const sessions = ref<ChatSession[]>([
  { id: '1', title: '公司制度问答示例', updatedAt: '今天' },
  { id: '2', title: '产品手册检索示例', updatedAt: '昨天' },
  { id: '3', title: '新会话（占位）', updatedAt: '更早' },
])

const activeSessionId = ref('1')
const draft = ref('')
const userAvatarUrl = ref<string | null>(null)

const messages = ref<ChatMessage[]>([
  {
    id: 'm1',
    role: 'assistant',
    content: '你好，我是企业知识库助手。Day1-2 仅完成界面骨架，问答能力将在后续里程碑接入。',
  },
  {
    id: 'm2',
    role: 'user',
    content: '先看看聊天界面长什么样。',
  },
  {
    id: 'm3',
    role: 'assistant',
    content: '左侧是会话列表，右上角是用户头像（未登录为空），底部可输入消息。',
  },
])

const activeTitle = computed(
  () => sessions.value.find((item) => item.id === activeSessionId.value)?.title ?? '新对话',
)

function selectSession(id: string) {
  activeSessionId.value = id
}

function createSession() {
  const id = String(Date.now())
  sessions.value.unshift({
    id,
    title: '新对话',
    updatedAt: '刚刚',
  })
  activeSessionId.value = id
  messages.value = [
    {
      id: `m-${id}`,
      role: 'assistant',
      content: '已创建新会话（本地占位，尚未接入后端）。',
    },
  ]
}

function sendMessage() {
  const content = draft.value.trim()
  if (!content) return
  messages.value.push({
    id: `u-${Date.now()}`,
    role: 'user',
    content,
  })
  draft.value = ''
  messages.value.push({
    id: `a-${Date.now()}`,
    role: 'assistant',
    content: '功能尚未接入，当前仅为界面演示。',
  })
}
</script>

<template>
  <div class="chat-shell">
    <aside class="sidebar">
      <button class="new-chat" type="button" @click="createSession">＋ 新对话</button>
      <div class="session-list">
        <button
          v-for="session in sessions"
          :key="session.id"
          type="button"
          class="session-item"
          :class="{ active: session.id === activeSessionId }"
          @click="selectSession(session.id)"
        >
          <span class="session-title">{{ session.title }}</span>
          <span class="session-time">{{ session.updatedAt }}</span>
        </button>
      </div>
      <div class="sidebar-footer">知识库问答 · 用户端</div>
    </aside>

    <section class="main">
      <header class="topbar">
        <h1>{{ activeTitle }}</h1>
        <div class="avatar" :title="userAvatarUrl ? '已登录' : '未登录'">
          <img v-if="userAvatarUrl" :src="userAvatarUrl" alt="avatar" />
        </div>
      </header>

      <div class="messages">
        <div
          v-for="message in messages"
          :key="message.id"
          class="message"
          :class="message.role"
        >
          <div class="bubble">{{ message.content }}</div>
        </div>
      </div>

      <footer class="composer">
        <div class="composer-box">
          <textarea
            v-model="draft"
            rows="1"
            placeholder="输入消息，Enter 发送（功能占位）"
            @keydown.enter.exact.prevent="sendMessage"
          />
          <button type="button" class="send" @click="sendMessage">发送</button>
        </div>
        <p class="hint">界面骨架阶段，消息不会调用后端。</p>
      </footer>
    </section>
  </div>
</template>

<style scoped>
.chat-shell {
  display: grid;
  grid-template-columns: 280px 1fr;
  height: 100vh;
  background: #f7f7f8;
  color: #1f1f1f;
}

.sidebar {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px 12px;
  background: #171717;
  color: #ececec;
}

.new-chat {
  border: 1px solid #3f3f3f;
  background: transparent;
  color: inherit;
  border-radius: 10px;
  padding: 10px 12px;
  text-align: left;
  cursor: pointer;
}

.new-chat:hover {
  background: #2a2a2a;
}

.session-list {
  flex: 1;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.session-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: inherit;
  padding: 10px 12px;
  text-align: left;
  cursor: pointer;
}

.session-item:hover,
.session-item.active {
  background: #2f2f2f;
}

.session-title {
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-time {
  font-size: 12px;
  color: #9a9a9a;
}

.sidebar-footer {
  font-size: 12px;
  color: #8a8a8a;
  padding: 8px 4px 0;
}

.main {
  display: grid;
  grid-template-rows: 56px 1fr auto;
  min-width: 0;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border-bottom: 1px solid #e6e6e6;
  background: #fff;
}

.topbar h1 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px dashed #c8c8c8;
  background: #f0f0f0;
  overflow: hidden;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.messages {
  overflow: auto;
  padding: 24px 0 12px;
}

.message {
  width: min(760px, calc(100% - 48px));
  margin: 0 auto 16px;
}

.bubble {
  padding: 14px 16px;
  border-radius: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  font-size: 15px;
}

.message.user .bubble {
  background: #ececec;
}

.message.assistant .bubble {
  background: #fff;
  border: 1px solid #ececec;
}

.composer {
  padding: 8px 24px 20px;
}

.composer-box {
  width: min(760px, 100%);
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: end;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 16px;
  padding: 10px 12px;
}

.composer-box textarea {
  border: 0;
  outline: none;
  resize: none;
  font: inherit;
  min-height: 24px;
  max-height: 160px;
  background: transparent;
}

.send {
  border: 0;
  border-radius: 10px;
  background: #111;
  color: #fff;
  padding: 8px 14px;
  cursor: pointer;
}

.hint {
  width: min(760px, 100%);
  margin: 8px auto 0;
  text-align: center;
  color: #8a8a8a;
  font-size: 12px;
}

@media (max-width: 860px) {
  .chat-shell {
    grid-template-columns: 1fr;
  }

  .sidebar {
    display: none;
  }
}
</style>
