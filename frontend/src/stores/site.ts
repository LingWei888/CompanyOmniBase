import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { fetchPublicModels, fetchSiteInfo, type PublicModelOption, type SiteInfo } from '@/api/publicSite'

const DEFAULT_SITE: SiteInfo = {
  siteName: '企业知识库智能问答',
  siteDescription: '基于企业知识库的智能问答助手',
  siteLogo: '',
}

function applyDocumentMeta(info: SiteInfo) {
  document.title = info.siteName || DEFAULT_SITE.siteName
  const description = info.siteDescription || DEFAULT_SITE.siteDescription
  let meta = document.querySelector<HTMLMetaElement>("meta[name='description']")
  if (!meta) {
    meta = document.createElement('meta')
    meta.name = 'description'
    document.head.appendChild(meta)
  }
  meta.content = description

  const href = info.siteLogo || '/favicon.ico'
  let link = document.querySelector<HTMLLinkElement>("link[rel='icon']")
  if (!link) {
    link = document.createElement('link')
    link.rel = 'icon'
    document.head.appendChild(link)
  }
  link.href = href
}

export const useSiteStore = defineStore('site', () => {
  const site = ref<SiteInfo>({ ...DEFAULT_SITE })
  const models = ref<PublicModelOption[]>([])
  const loaded = ref(false)
  const loading = ref(false)

  const siteName = computed(() => site.value.siteName || DEFAULT_SITE.siteName)
  const siteDescription = computed(() => site.value.siteDescription || DEFAULT_SITE.siteDescription)
  const siteLogo = computed(() => site.value.siteLogo || '')

  async function load(force = false) {
    if (loading.value) return
    if (loaded.value && !force) return
    loading.value = true
    try {
      const [siteInfo, modelList] = await Promise.all([fetchSiteInfo(), fetchPublicModels()])
      site.value = {
        siteName: siteInfo.siteName || DEFAULT_SITE.siteName,
        siteDescription: siteInfo.siteDescription || DEFAULT_SITE.siteDescription,
        siteLogo: siteInfo.siteLogo || '',
      }
      models.value = modelList
      applyDocumentMeta(site.value)
      loaded.value = true
    } catch {
      if (!loaded.value) {
        site.value = { ...DEFAULT_SITE }
        models.value = []
        applyDocumentMeta(site.value)
      }
    } finally {
      loading.value = false
    }
  }

  async function refresh() {
    await load(true)
  }

  return {
    site,
    models,
    loaded,
    loading,
    siteName,
    siteDescription,
    siteLogo,
    load,
    refresh,
  }
})
