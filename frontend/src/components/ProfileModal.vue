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
  nickname: '',
  avatarUrl: '',
})

watch(
  () => props.open,
  (open) => {
    if (open) {
      form.nickname = auth.user?.nickname || ''
      form.avatarUrl = auth.user?.avatarUrl || ''
    }
  },
)

async function submit() {
  loading.value = true
  try {
    await auth.updateProfile({
      nickname: form.nickname.trim(),
      avatarUrl: form.avatarUrl.trim(),
    })
    toast.success('账户信息已更新')
    emit('close')
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '保存失败')
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
          <h3>修改账户信息</h3>
          <button type="button" class="x" @click="emit('close')">×</button>
        </header>
        <form @submit.prevent="submit">
          <label>
            昵称
            <input v-model="form.nickname" maxlength="64" placeholder="显示昵称" />
          </label>
          <label>
            头像 URL
            <input v-model="form.avatarUrl" maxlength="512" placeholder="可选，填写图片地址" />
          </label>
          <div class="actions">
            <button type="button" @click="emit('close')">取消</button>
            <button type="submit" class="primary" :disabled="loading">
              {{ loading ? '保存中…' : '保存' }}
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
