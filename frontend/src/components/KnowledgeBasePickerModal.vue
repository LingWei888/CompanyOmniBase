<script setup lang="ts">
import { ref, watch } from 'vue'
import type { PublicKnowledgeBaseOption } from '@/api/publicSite'

const props = defineProps<{
  open: boolean
  options: PublicKnowledgeBaseOption[]
  modelValue: number[]
}>()

const emit = defineEmits<{
  'update:modelValue': [ids: number[]]
  close: []
}>()

const draft = ref<number[]>([])

watch(
  () => props.open,
  (open) => {
    if (open) {
      draft.value = [...props.modelValue]
      document.body.style.overflow = 'hidden'
    } else {
      document.body.style.overflow = ''
    }
  },
)

function toggle(id: number) {
  if (draft.value.includes(id)) {
    draft.value = draft.value.filter((item) => item !== id)
  } else {
    draft.value = [...draft.value, id]
  }
}

function selectAll() {
  draft.value = props.options.map((item) => item.id)
}

function clearAll() {
  draft.value = []
}

function confirm() {
  emit('update:modelValue', [...draft.value])
  emit('close')
}
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="kb-root">
      <div class="mask" @click="emit('close')" />
      <div class="dialog" role="dialog" aria-modal="true">
        <header class="head">
          <h3>选择知识库</h3>
          <button type="button" class="x" aria-label="关闭" @click="emit('close')">×</button>
        </header>

        <p class="hint">可多选；默认全选。全不选表示关闭知识库检索（纯对话）。</p>

        <div class="actions">
          <button type="button" class="link" @click="selectAll">全选</button>
          <button type="button" class="link" @click="clearAll">全不选</button>
        </div>

        <div v-if="!options.length" class="empty">暂无可用知识库</div>
        <ul v-else class="list">
          <li v-for="kb in options" :key="kb.id">
            <label class="row">
              <input
                type="checkbox"
                :checked="draft.includes(kb.id)"
                @change="toggle(kb.id)"
              />
              <span class="name">{{ kb.name }}</span>
              <span v-if="kb.description" class="desc">{{ kb.description }}</span>
            </label>
          </li>
        </ul>

        <footer class="foot">
          <button type="button" class="ghost" @click="emit('close')">取消</button>
          <button type="button" class="primary" @click="confirm">确定</button>
        </footer>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.kb-root {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: grid;
  place-items: center;
}

.mask {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.35);
}

.dialog {
  position: relative;
  width: min(440px, calc(100vw - 32px));
  max-height: min(72vh, 560px);
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.18);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px 8px;
}

.head h3 {
  margin: 0;
  font-size: 17px;
}

.x {
  border: 0;
  background: transparent;
  font-size: 24px;
  line-height: 1;
  cursor: pointer;
  color: #666;
}

.hint {
  margin: 0;
  padding: 0 18px 8px;
  font-size: 13px;
  color: #777;
}

.actions {
  display: flex;
  gap: 12px;
  padding: 0 18px 10px;
}

.link {
  border: 0;
  background: transparent;
  color: #1a5fb4;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  padding: 0;
}

.empty {
  padding: 24px 18px;
  color: #888;
  font-size: 14px;
}

.list {
  list-style: none;
  margin: 0;
  padding: 0 10px 10px;
  overflow: auto;
  flex: 1;
}

.row {
  display: grid;
  grid-template-columns: auto 1fr;
  grid-template-rows: auto auto;
  column-gap: 10px;
  row-gap: 2px;
  align-items: start;
  padding: 10px 8px;
  border-radius: 10px;
  cursor: pointer;
}

.row:hover {
  background: #f6f6f6;
}

.row input {
  grid-row: 1 / span 2;
  margin-top: 3px;
}

.name {
  font-size: 14px;
  color: #222;
}

.desc {
  grid-column: 2;
  font-size: 12px;
  color: #888;
  line-height: 1.4;
}

.foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 18px 16px;
  border-top: 1px solid #eee;
}

.ghost,
.primary {
  border-radius: 10px;
  padding: 8px 16px;
  font: inherit;
  cursor: pointer;
}

.ghost {
  border: 1px solid #e5e5e5;
  background: #fff;
  color: #444;
}

.primary {
  border: 0;
  background: #111;
  color: #fff;
}
</style>
