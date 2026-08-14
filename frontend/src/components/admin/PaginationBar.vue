<script setup lang="ts">
const props = defineProps<{
  page: number
  size: number
  total: number
  totalPages: number
}>()

const emit = defineEmits<{
  'update:page': [page: number]
  'update:size': [size: number]
}>()

function onSizeChange(event: Event) {
  const value = Number((event.target as HTMLSelectElement).value)
  emit('update:size', value)
  emit('update:page', 1)
}
</script>

<template>
  <div class="pager">
    <span class="meta">共 {{ total }} 条</span>
    <div class="btns">
      <button type="button" :disabled="page <= 1" @click="emit('update:page', page - 1)">上一页</button>
      <span class="cur">{{ page }} / {{ Math.max(totalPages, 1) }}</span>
      <button type="button" :disabled="page >= totalPages || totalPages === 0" @click="emit('update:page', page + 1)">
        下一页
      </button>
    </div>
    <select :value="size" @change="onSizeChange">
      <option :value="10">10条/页</option>
      <option :value="20">20条/页</option>
      <option :value="50">50条/页</option>
    </select>
  </div>
</template>

<style scoped>
.pager {
  margin-top: 14px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 13px;
  color: #525252;
}

.btns {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cur {
  min-width: 64px;
  text-align: center;
}

@media (max-width: 640px) {
  .pager {
    justify-content: space-between;
  }

  .pager select,
  .pager .btns {
    width: 100%;
  }

  .btns {
    justify-content: space-between;
  }
}
</style>
