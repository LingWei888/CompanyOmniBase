<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useToast } from '@/composables/useToast'
import { useSiteStore } from '@/stores/site'
import { listSysConfigs, saveSysConfigs, uploadSiteLogo, type SysConfigItem } from '@/api/systemConfig'

const toast = useToast()
const site = useSiteStore()
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const form = reactive({
  site_name: '',
  site_description: '',
  site_logo: '',
  contact_email: '',
  ingest_chunk_size: '800',
  ingest_chunk_overlap: '100',
})

async function load() {
  loading.value = true
  try {
    const list = await listSysConfigs()
    const map = new Map(list.map((item) => [item.configKey, item.configValue || '']))
    form.site_name = map.get('site_name') || ''
    form.site_description = map.get('site_description') || ''
    form.site_logo = map.get('site_logo') || ''
    form.contact_email = map.get('contact_email') || ''
    form.ingest_chunk_size = map.get('ingest_chunk_size') || '800'
    form.ingest_chunk_overlap = map.get('ingest_chunk_overlap') || '100'
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function onLogoChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  uploading.value = true
  try {
    const result = await uploadSiteLogo(file)
    form.site_logo = result.url
    await site.refresh()
    toast.success('Logo 已上传并保存')
  } catch (e) {
    toast.error(e instanceof Error ? e.message : 'Logo 上传失败')
  } finally {
    uploading.value = false
    input.value = ''
  }
}

async function save() {
  const chunkSize = Number(form.ingest_chunk_size)
  const overlap = Number(form.ingest_chunk_overlap)
  if (!Number.isFinite(chunkSize) || chunkSize < 100 || chunkSize > 8000) {
    toast.error('默认切分长度需在 100-8000 之间')
    return
  }
  if (!Number.isFinite(overlap) || overlap < 0 || overlap > 4000) {
    toast.error('默认重叠长度需在 0-4000 之间')
    return
  }
  if (overlap >= chunkSize) {
    toast.error('默认重叠长度必须小于切分长度')
    return
  }

  saving.value = true
  try {
    const items: SysConfigItem[] = [
      { configKey: 'site_name', configValue: form.site_name, remark: '站点名称' },
      { configKey: 'site_description', configValue: form.site_description, remark: '站点描述' },
      { configKey: 'site_logo', configValue: form.site_logo, remark: '站点 Logo URL' },
      { configKey: 'contact_email', configValue: form.contact_email, remark: '联系邮箱' },
      {
        configKey: 'ingest_chunk_size',
        configValue: String(chunkSize),
        remark: '默认切分长度（字符）',
      },
      {
        configKey: 'ingest_chunk_overlap',
        configValue: String(overlap),
        remark: '默认切分重叠长度（字符）',
      },
    ]
    await saveSysConfigs(items)
    await site.refresh()
    toast.success('保存成功')
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>系统设置</h2>
        <p>站点信息与文档入库默认切分参数</p>
      </div>
      <button type="button" class="primary" :disabled="saving || loading" @click="save">
        {{ saving ? '保存中…' : '保存设置' }}
      </button>
    </div>

    <p v-if="loading">加载中…</p>

    <div v-else class="form">
      <h3 class="section-title">站点信息</h3>
      <label>
        站点名称
        <input v-model="form.site_name" />
      </label>
      <label>
        站点描述
        <textarea v-model="form.site_description" rows="4" />
      </label>
      <label>
        站点 Logo
        <input
          type="file"
          accept="image/png,image/jpeg,image/gif,image/webp,image/svg+xml"
          :disabled="uploading"
          @change="onLogoChange"
        />
      </label>
      <div v-if="form.site_logo" class="logo-preview">
        <img :src="form.site_logo" alt="site logo" />
        <p class="logo-url">{{ form.site_logo }}</p>
      </div>
      <p v-if="uploading" class="hint">Logo 上传中…</p>
      <label>
        联系邮箱
        <input v-model="form.contact_email" />
      </label>

      <h3 class="section-title">文档入库默认切分</h3>
      <p class="hint">上传文档时高级设置会预填这些值，也可按文档单独修改。</p>
      <label>
        默认切分长度（字符）
        <input v-model="form.ingest_chunk_size" type="number" min="100" max="8000" />
      </label>
      <label>
        默认重叠长度（字符）
        <input v-model="form.ingest_chunk_overlap" type="number" min="0" max="4000" />
      </label>
    </div>
  </div>
</template>

<style scoped>
.form {
  max-width: 560px;
}

.section-title {
  margin: 8px 0 12px;
  font-size: 15px;
  color: #262626;
}

.logo-preview {
  margin: 0 0 14px;
}

.logo-preview img {
  max-width: 160px;
  max-height: 80px;
  object-fit: contain;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  background: #fff;
  padding: 8px;
}

.logo-url {
  margin: 8px 0 0;
  font-size: 12px;
  color: #737373;
  word-break: break-all;
}

.hint {
  margin: 0 0 12px;
  color: #737373;
  font-size: 13px;
}
</style>
