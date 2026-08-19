<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useUserAuthStore } from '@/stores/userAuth'
import { useToast } from '@/composables/useToast'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{ close: [] }>()

const auth = useUserAuthStore()
const toast = useToast()
const loading = ref(false)
const uploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
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

async function onPickFile(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    toast.error('请选择图片文件')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    toast.error('头像不能超过 5MB')
    return
  }
  uploading.value = true
  try {
    const profile = await auth.uploadAvatar(file)
    form.avatarUrl = profile.avatarUrl || ''
    toast.success('头像已更新')
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '头像上传失败')
  } finally {
    uploading.value = false
  }
}

async function submit() {
  loading.value = true
  try {
    await auth.updateProfile({
      nickname: form.nickname.trim(),
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

          <div class="avatar-block">
            <span class="label">头像</span>
            <div class="avatar-row">
              <div class="preview">
                <img v-if="form.avatarUrl" :src="form.avatarUrl" alt="avatar" />
                <span v-else>{{ (form.nickname || auth.user?.username || '?').slice(0, 1).toUpperCase() }}</span>
              </div>
              <div class="avatar-actions">
                <input
                  ref="fileInput"
                  type="file"
                  accept="image/png,image/jpeg,image/jpg,image/gif,image/webp,image/svg+xml"
                  class="hidden"
                  @change="onPickFile"
                />
                <button type="button" :disabled="uploading" @click="fileInput?.click()">
                  {{ uploading ? '上传中…' : '上传头像' }}
                </button>
                <p class="hint">支持 png/jpg/gif/webp，最大 5MB</p>
              </div>
            </div>
          </div>

          <div class="actions">
            <button type="button" @click="emit('close')">取消</button>
            <button type="submit" class="primary" :disabled="loading || uploading">
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
.avatar-block {
  margin-bottom: 12px;
}
.avatar-block .label {
  display: block;
  font-size: 14px;
  margin-bottom: 8px;
}
.avatar-row {
  display: flex;
  align-items: center;
  gap: 14px;
}
.preview {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  overflow: hidden;
  background: #0d0d0d;
  color: #fff;
  display: grid;
  place-items: center;
  font-weight: 700;
  flex-shrink: 0;
}
.preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-start;
}
.hint {
  margin: 0;
  font-size: 12px;
  color: #8a8a8a;
}
.hidden {
  display: none;
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
button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
button.primary {
  background: #0d0d0d;
  border-color: #0d0d0d;
  color: #fff;
}
</style>
