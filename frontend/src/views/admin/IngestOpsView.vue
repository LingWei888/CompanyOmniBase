<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useToast } from '@/composables/useToast'
import {
  fetchIngestOpsOverview,
  fetchRunningEmbeddingQueue,
  fetchWaitingEmbeddingQueue,
  startEmbedding,
  startEmbeddingBatch,
  type IngestQueueItem,
} from '@/api/ingestOps'

const toast = useToast()
const loading = ref(true)
const refreshing = ref(false)
const waitingRows = ref<IngestQueueItem[]>([])
const runningRows = ref<IngestQueueItem[]>([])
const selectedIds = ref<number[]>([])
const stats = ref({
  waitingEmbeddingCount: 0,
  embeddingCount: 0,
  failedCount: 0,
  readyCount: 0,
  documentStatusCounts: {} as Record<string, number>,
})

const allSelected = computed(() => {
  return waitingRows.value.length > 0 && selectedIds.value.length === waitingRows.value.length
})

async function loadData() {
  refreshing.value = true
  try {
    const [overview, waiting, running] = await Promise.all([
      fetchIngestOpsOverview(),
      fetchWaitingEmbeddingQueue(1, 50),
      fetchRunningEmbeddingQueue(1, 50),
    ])
    stats.value = {
      waitingEmbeddingCount: overview.waitingEmbeddingCount,
      embeddingCount: overview.embeddingCount,
      failedCount: overview.failedCount,
      readyCount: overview.readyCount,
      documentStatusCounts: overview.documentStatusCounts,
    }
    waitingRows.value = waiting.records
    runningRows.value = running.records
    selectedIds.value = selectedIds.value.filter((id) => waitingRows.value.some((row) => row.id === id))
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '加载运维数据失败')
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

function toggleSelect(id: number) {
  if (selectedIds.value.includes(id)) {
    selectedIds.value = selectedIds.value.filter((item) => item !== id)
  } else {
    selectedIds.value = [...selectedIds.value, id]
  }
}

function toggleSelectAll() {
  if (allSelected.value) {
    selectedIds.value = []
  } else {
    selectedIds.value = waitingRows.value.map((row) => row.id)
  }
}

async function handleStartOne(id: number) {
  try {
    await startEmbedding(id)
    toast.success('已提交向量化任务')
    await loadData()
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '提交失败')
  }
}

async function handleStartBatch() {
  if (!selectedIds.value.length) {
    toast.error('请先选择等待向量化的文档')
    return
  }
  try {
    await startEmbeddingBatch(selectedIds.value)
    toast.success(`已提交 ${selectedIds.value.length} 个向量化任务`)
    selectedIds.value = []
    await loadData()
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '批量提交失败')
  }
}

onMounted(() => {
  void loadData()
})
</script>

<template>
  <div class="panel">
    <div class="head">
      <div>
        <h2>入库运维</h2>
        <p class="desc">切分完成后进入「等待向量化」队列；Embedding 需手动触发以节省算力。</p>
      </div>
      <button type="button" class="btn" :disabled="refreshing" @click="loadData">
        {{ refreshing ? '刷新中…' : '刷新' }}
      </button>
    </div>

    <div v-if="loading" class="state">加载中…</div>
    <template v-else>
      <div class="stats">
        <div class="stat">
          <span>等待向量化</span>
          <strong>{{ stats.waitingEmbeddingCount }}</strong>
        </div>
        <div class="stat">
          <span>向量化中</span>
          <strong>{{ stats.embeddingCount }}</strong>
        </div>
        <div class="stat">
          <span>已完成 READY</span>
          <strong>{{ stats.readyCount }}</strong>
        </div>
        <div class="stat warn">
          <span>失败 FAILED</span>
          <strong>{{ stats.failedCount }}</strong>
        </div>
      </div>

      <section class="queue">
        <div class="queue-head">
          <h3>等待向量化队列</h3>
          <button type="button" class="btn primary" :disabled="!selectedIds.length" @click="handleStartBatch">
            批量开始向量化（{{ selectedIds.length }}）
          </button>
        </div>
        <div v-if="!waitingRows.length" class="empty">暂无等待向量化的文档</div>
        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th><input type="checkbox" :checked="allSelected" @change="toggleSelectAll" /></th>
                <th>ID</th>
                <th>标题</th>
                <th>知识库</th>
                <th>片段数</th>
                <th>更新时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in waitingRows" :key="row.id">
                <td>
                  <input
                    type="checkbox"
                    :checked="selectedIds.includes(row.id)"
                    @change="toggleSelect(row.id)"
                  />
                </td>
                <td>{{ row.id }}</td>
                <td>{{ row.title }}</td>
                <td>{{ row.kbId }}</td>
                <td>{{ row.chunkCount ?? 0 }}</td>
                <td>{{ row.updatedAt?.replace('T', ' ').slice(0, 19) }}</td>
                <td>
                  <button type="button" class="link" @click="handleStartOne(row.id)">开始向量化</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="queue">
        <div class="queue-head">
          <h3>向量化中队列</h3>
        </div>
        <div v-if="!runningRows.length" class="empty">当前没有正在向量化的文档</div>
        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>标题</th>
                <th>知识库</th>
                <th>片段数</th>
                <th>更新时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in runningRows" :key="row.id">
                <td>{{ row.id }}</td>
                <td>{{ row.title }}</td>
                <td>{{ row.kbId }}</td>
                <td>{{ row.chunkCount ?? 0 }}</td>
                <td>{{ row.updatedAt?.replace('T', ' ').slice(0, 19) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>
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

.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

h2 {
  margin: 0 0 8px;
}

.desc {
  margin: 0;
  color: #737373;
}

.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}

.stat {
  background: #fafafa;
  border-radius: 10px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat.warn strong {
  color: #b91c1c;
}

.stat span {
  color: #737373;
  font-size: 13px;
}

.queue {
  margin-top: 20px;
}

.queue-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.queue h3 {
  margin: 0;
  font-size: 16px;
}

.table-wrap {
  overflow: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

th,
td {
  border-bottom: 1px solid #eee;
  padding: 10px 8px;
  text-align: left;
  white-space: nowrap;
}

th:first-child,
td:first-child {
  width: 36px;
}

.empty,
.state {
  color: #737373;
  padding: 16px 0;
}

.btn {
  border: 1px solid #d4d4d4;
  background: #fff;
  border-radius: 8px;
  padding: 8px 14px;
  cursor: pointer;
}

.btn.primary {
  background: #111;
  color: #fff;
  border-color: #111;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.link {
  border: 0;
  background: transparent;
  color: #2563eb;
  cursor: pointer;
  padding: 0;
}
</style>
