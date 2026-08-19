<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchDashboardOverview } from '@/api/adminAuth'
import { useToast } from '@/composables/useToast'

const toast = useToast()
const loading = ref(true)
const overview = ref<Record<string, unknown> | null>(null)

onMounted(async () => {
  try {
    overview.value = await fetchDashboardOverview()
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="panel">
    <h2>概览</h2>
    <p class="desc">站长账号登录后台；普通用户在「用户」菜单管理，不能登录前台聊天。</p>

    <div v-if="loading" class="state">加载中…</div>
    <div v-else-if="!overview" class="state">暂无数据</div>
    <div v-else class="grid">
      <div class="item">
        <span>标题</span>
        <strong>{{ overview?.title }}</strong>
      </div>
      <div class="item">
        <span>状态</span>
        <strong>{{ overview?.status }}</strong>
      </div>
      <div class="item">
        <span>等待向量化</span>
        <strong>{{ overview?.waitingEmbeddingCount ?? 0 }}</strong>
      </div>
      <div class="item">
        <span>向量化中</span>
        <strong>{{ overview?.embeddingCount ?? 0 }}</strong>
      </div>
      <div class="item">
        <span>READY</span>
        <strong>{{ overview?.readyCount ?? 0 }}</strong>
      </div>
      <div class="item">
        <span>FAILED</span>
        <strong>{{ overview?.failedCount ?? 0 }}</strong>
      </div>
      <div class="item">
        <span>服务器时间</span>
        <strong>{{ overview?.serverTime }}</strong>
      </div>
    </div>
    <p v-if="overview" class="hint">
      切分完成后文档会进入「等待向量化」；请到
      <RouterLink to="/admin/ingest-ops">入库运维</RouterLink>
      手动触发 Embedding。
    </p>
  </div>
</template>

<style scoped>
.panel {
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 12px;
  padding: 20px;
  min-width: 0;
}

h2 {
  margin: 0 0 8px;
}

.desc {
  margin: 0 0 18px;
  color: #737373;
}

.state {
  color: #525252;
}

.grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(min(180px, 100%), 1fr));
}

.item {
  background: #fafafa;
  border-radius: 10px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.item span {
  color: #737373;
  font-size: 13px;
}

.item strong {
  word-break: break-word;
}

.hint {
  margin: 16px 0 0;
  color: #525252;
  font-size: 14px;
}

.hint a {
  color: #2563eb;
}

@media (max-width: 640px) {
  .panel {
    padding: 14px;
  }
}
</style>
