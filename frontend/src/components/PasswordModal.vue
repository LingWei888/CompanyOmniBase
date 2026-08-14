<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useUserAuthStore } from '@/stores/userAuth'
import { useToast } from '@/composables/useToast'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{ close: [] }>()

const auth = useUserAuthStore()
const toast = useToast()
const loading = ref(false)
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

watch(
  () => props.open,
  (open) => {
    if (open) {
      form.oldPassword = ''
      form.newPassword = ''
      form.confirmPassword = ''
    }
  },
)

async function submit() {
  if (form.newPassword.length < 6) {
    toast.error('新密码至少 6 位')
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    toast.error('两次输入的新密码不一致')
    return
  }
  loading.value = true
  try {
    await auth.changePassword(form.oldPassword, form.newPassword)
    toast.success('密码已修改')
    emit('close')
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '修改失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="modal-root">
      <div class="mask" @click="emit('close')" />
      <div class="dialog">
        <header>
          <h3>修改密码</h3>
          <button type="button" class="x" @click="emit('close')">×</button>
        </header>
        <form @submit.prevent="submit">
          <label>
            原密码
            <input v-model="form.oldPassword" type="password" autocomplete="current-password" required />
          </label>
          <label>
            新密码
            <input v-model="form.newPassword" type="password" autocomplete="new-password" required />
          </label>
          <label>
            确认新密码
            <input v-model="form.confirmPassword" type="password" autocomplete="new-password" required />
          </label>
          <div class="actions">
            <button type="button" @click="emit('close')">取消</button>
            <button type="submit" class="primary" :disabled="loading">
              {{ loading ? '提交中…' : '确认修改' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-root {
  position: fixed;
  inset: 0;
  z-index: 3200;
  display: grid;
  place-items: center;
  padding: 16px;
}
.mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
}
.dialog {
  position: relative;
  width: min(420px, 100%);
  background: #fff;
  border-radius: 16px;
  border: 1px solid #e5e5e5;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.16);
  padding: 18px;
}
header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
h3 {
  margin: 0;
  font-size: 16px;
}
.x {
  border: 0;
  background: transparent;
  font-size: 22px;
  cursor: pointer;
  color: #737373;
}
label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
  font-size: 14px;
}
input {
  border: 1px solid #d4d4d4;
  border-radius: 10px;
  padding: 10px 12px;
  font: inherit;
}
.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}
button {
  border: 1px solid #d4d4d4;
  background: #fff;
  border-radius: 10px;
  padding: 8px 14px;
  cursor: pointer;
  font: inherit;
}
button.primary {
  background: #0d0d0d;
  border-color: #0d0d0d;
  color: #fff;
}
</style>
