<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchDashboardOverview } from '@/api/adminAuth'

const loading = ref(true)
const error = ref('')
const overview = ref<Record<string, string> | null>(null)

onMounted(async () => {
  try {
    overview.value = await fetchDashboardOverview()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="panel">
    <h2>概览</h2>
    <p class="desc">Day2 鉴权验收：此页需携带 ADMIN Token 才能访问。</p>

    <div v-if="loading" class="state">加载中…</div>
    <div v-else-if="error" class="state error">{{ error }}</div>
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
        <span>服务器时间</span>
        <strong>{{ overview?.serverTime }}</strong>
      </div>
    </div>
  </div>
</template>

<style scoped>
.panel {
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 12px;
  padding: 20px;
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

.state.error {
  color: #b91c1c;
}

.grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.item {
  background: #fafafa;
  border-radius: 10px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.item span {
  color: #737373;
  font-size: 13px;
}
</style>
