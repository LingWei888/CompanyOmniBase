import { computed, ref, watch } from 'vue'
import {
  createProblemConvertRecord,
  deleteProblemConvertRecord,
  getProblemConvertRecord,
  listProblemConvertRecords,
  updateProblemConvertRecord,
  type ProblemConvertRecordDetail,
  type ProblemConvertRecordItem,
} from '@/api/problemConvertRecords'
import { useUserAuthStore } from '@/stores/userAuth'

const records = ref<ProblemConvertRecordItem[]>([])
const activeRecordId = ref<number | null>(null)
const activeDetail = ref<ProblemConvertRecordDetail | null>(null)
const loading = ref(false)
const ready = ref(false)

function sortRecords(list: ProblemConvertRecordItem[]) {
  return [...list].sort((a, b) => String(b.updatedAt).localeCompare(String(a.updatedAt)))
}

function clearState() {
  records.value = []
  activeRecordId.value = null
  activeDetail.value = null
  ready.value = false
}

export function useProblemConvertRecords() {
  const auth = useUserAuthStore()
  const canUseRecords = computed(() => auth.isLoggedIn && auth.user?.id != null)

  watch(
    () => [auth.isLoggedIn, auth.user?.id] as const,
    ([loggedIn]) => {
      if (!loggedIn) clearState()
    },
  )

  async function refreshList() {
    if (!canUseRecords.value) {
      clearState()
      return
    }
    const list = await listProblemConvertRecords()
    records.value = sortRecords(list)
  }

  async function loadDetail(id: number) {
    if (!canUseRecords.value) return null
    const detail = await getProblemConvertRecord(id)
    activeRecordId.value = detail.id
    activeDetail.value = detail
    return detail
  }

  async function ensureLoaded() {
    if (!canUseRecords.value) {
      clearState()
      return
    }
    loading.value = true
    try {
      await refreshList()
      if (records.value.length === 0) {
        await createDraft()
      } else if (
        activeRecordId.value == null
        || !records.value.some((r) => r.id === activeRecordId.value)
      ) {
        await loadDetail(records.value[0].id)
      } else {
        await loadDetail(activeRecordId.value)
      }
      ready.value = true
    } finally {
      loading.value = false
    }
  }

  async function createDraft() {
    if (!canUseRecords.value) return null
    loading.value = true
    try {
      const detail = await createProblemConvertRecord()
      await refreshList()
      activeRecordId.value = detail.id
      activeDetail.value = detail
      ready.value = true
      return detail
    } finally {
      loading.value = false
    }
  }

  async function selectRecord(id: number) {
    if (!canUseRecords.value) return null
    if (activeRecordId.value === id && activeDetail.value?.id === id) {
      return activeDetail.value
    }
    loading.value = true
    try {
      return await loadDetail(id)
    } finally {
      loading.value = false
    }
  }

  async function upsertCurrent(payload: {
    referenceNickname: string
    originalText: string
    resultMarkdown: string
    solutionCode?: string
  }) {
    if (!canUseRecords.value) return null
    let id = activeRecordId.value
    if (id == null) {
      const created = await createDraft()
      id = created?.id ?? null
    }
    if (id == null) return null
    const detail = await updateProblemConvertRecord(id, {
      referenceNickname: payload.referenceNickname,
      originalText: payload.originalText,
      resultMarkdown: payload.resultMarkdown,
      solutionCode: payload.solutionCode ?? '',
    })
    activeRecordId.value = detail.id
    activeDetail.value = detail
    await refreshList()
    return detail
  }

  async function deleteRecord(id: number) {
    if (!canUseRecords.value) return
    await deleteProblemConvertRecord(id)
    if (activeRecordId.value === id) {
      activeRecordId.value = null
      activeDetail.value = null
    }
    await refreshList()
    if (records.value.length === 0) {
      await createDraft()
    } else if (activeRecordId.value == null) {
      await loadDetail(records.value[0].id)
    }
  }

  function formatRecordTime(iso: string) {
    const d = new Date(iso)
    if (Number.isNaN(d.getTime())) return ''
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${d.getMonth() + 1}/${d.getDate()} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  }

  return {
    records,
    activeRecordId,
    activeDetail,
    loading,
    ready,
    canUseRecords,
    ensureLoaded,
    refreshList,
    createDraft,
    selectRecord,
    upsertCurrent,
    deleteRecord,
    formatRecordTime,
  }
}
