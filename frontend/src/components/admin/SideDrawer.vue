<script setup lang="ts">
import { computed, watch } from 'vue'

const props = defineProps<{
  open: boolean
  title: string
  width?: string
}>()

const emit = defineEmits<{
  close: []
}>()

const panelStyle = computed(() => ({
  '--drawer-width': props.width || '420px',
}))

watch(
  () => props.open,
  (value) => {
    document.body.style.overflow = value ? 'hidden' : ''
  },
)

function onMaskClick() {
  emit('close')
}
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="drawer-root">
      <div class="mask" @click="onMaskClick" />
      <aside class="panel" :style="panelStyle">
        <header class="head">
          <h3>{{ title }}</h3>
          <button type="button" class="x" @click="emit('close')">×</button>
        </header>
        <div class="body">
          <slot />
        </div>
        <footer v-if="$slots.footer" class="foot">
          <slot name="footer" />
        </footer>
      </aside>
    </div>
  </Teleport>
</template>

<style scoped>
.drawer-root {
  position: fixed;
  inset: 0;
  z-index: 1000;
}

.mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
}

.panel {
  position: absolute;
  top: 0;
  right: 0;
  height: 100%;
  width: min(var(--drawer-width, 420px), 100vw);
  max-width: 100%;
  background: #fff;
  box-shadow: -8px 0 24px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  animation: slide-in 0.2s ease;
}

@keyframes slide-in {
  from {
    transform: translateX(24px);
    opacity: 0.6;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  border-bottom: 1px solid #ececec;
  gap: 12px;
}

.head h3 {
  margin: 0;
  font-size: 16px;
}

.x {
  border: 0;
  background: transparent;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  color: #737373;
}

.body {
  flex: 1;
  overflow: auto;
  padding: 18px;
}

.foot {
  padding: 14px 18px;
  border-top: 1px solid #ececec;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

@media (max-width: 640px) {
  .panel {
    width: 100vw;
  }

  .foot {
    justify-content: stretch;
  }

  .foot :deep(button) {
    flex: 1;
  }
}
</style>
