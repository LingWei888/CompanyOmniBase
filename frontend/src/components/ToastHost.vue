<script setup lang="ts">
import { useToast } from '@/composables/useToast'

const { toasts, dismiss } = useToast()
</script>

<template>
  <Teleport to="body">
    <div class="toast-host" aria-live="polite">
      <TransitionGroup name="toast">
        <div
          v-for="item in toasts"
          :key="item.id"
          class="toast"
          :class="item.type"
          role="status"
          @click="dismiss(item.id)"
        >
          <span class="dot" />
          <p>{{ item.message }}</p>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-host {
  position: fixed;
  top: 16px;
  right: 16px;
  z-index: 4000;
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: min(360px, calc(100vw - 24px));
  pointer-events: none;
}

.toast {
  pointer-events: auto;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #e5e5e5;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.12);
  cursor: pointer;
}

.toast p {
  margin: 0;
  flex: 1;
  font-size: 14px;
  line-height: 1.45;
  color: #262626;
  word-break: break-word;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 5px;
  flex-shrink: 0;
}

.toast.success {
  border-color: #a7f3d0;
  background: #ecfdf5;
}

.toast.success .dot {
  background: #059669;
}

.toast.error {
  border-color: #fecaca;
  background: #fef2f2;
}

.toast.error .dot {
  background: #dc2626;
}

.toast.info {
  border-color: #bfdbfe;
  background: #eff6ff;
}

.toast.info .dot {
  background: #2563eb;
}

.toast-enter-active,
.toast-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.toast-enter-from {
  opacity: 0;
  transform: translateX(16px);
}

.toast-leave-to {
  opacity: 0;
  transform: translateX(16px);
}

@media (max-width: 640px) {
  .toast-host {
    top: 12px;
    right: 12px;
    left: 12px;
    width: auto;
  }
}
</style>
