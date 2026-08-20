import { computed, ref, watch } from 'vue'
import {
  createTestdataGenRecord,
  deleteTestdataGenRecord,
  getTestdataGenRecord,
  listTestdataGenRecords,
  updateTestdataGenRecord,
  type TestdataGenRecordDetail,
  type TestdataGenRecordItem,
} from '@/api/testdataGenRecords'
import { useUserAuthStore } from '@/stores/userAuth'

const records = ref<TestdataGenRecordItem[]>([])
const activeRecordId = ref<number | null>(null)
const activeDetail = ref<TestdataGenRecordDetail | null>(null)
const loading = ref(false)
const ready = ref(false)

export type TestdataGenFlushPayload = {
  originalText: string
  resultPython: string
  solutionCode?: string
}

function sortRecords(list: TestdataGenRecordItem[]) {
  return [...list].sort((a, b) => String(b.updatedAt).localeCompare(String(a.updatedAt)))
}

function clearState() {
  records.value = []
  activeRecordId.value = null
  activeDetail.value = null
  ready.value = false
}

function isBlankDetail(detail: TestdataGenRecordDetail | null | undefined) {
  if (!detail) return true
  return !detail.originalText?.trim()
    && !detail.resultPython?.trim()
    && !detail.solutionCode?.trim()
}

function isBlankFlush(payload?: TestdataGenFlushPayload | null) {
  if (!payload) return true
  return !payload.originalText?.trim()
    && !payload.resultPython?.trim()
    && !payload.solutionCode?.trim()
}

export function useTestdataGenRecords() {
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
    const list = await listTestdataGenRecords()
    records.value = sortRecords(list)
  }

  async function loadDetail(id: number) {
    if (!canUseRecords.value) return null
    const detail = await getTestdataGenRecord(id)
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

  /**
   * 创建新记录：
   * - 若当前本地有内容，先保存到当前记录，再新建空白
   * - 若已有空白草稿（含当前），复用，不重复创建
   */
  async function createDraft(flush?: TestdataGenFlushPayload | null) {
    if (!canUseRecords.value) return { detail: null, reused: false }

    loading.value = true
    try {
      if (!isBlankFlush(flush)) {
        await upsertCurrent(flush!)
      } else if (isBlankDetail(activeDetail.value) && activeRecordId.value != null) {
        const detail = await loadDetail(activeRecordId.value)
        ready.value = true
        return { detail, reused: true }
      }

      const beforeId = activeRecordId.value
      const detail = await createTestdataGenRecord()
      await refreshList()
      activeRecordId.value = detail.id
      activeDetail.value = detail
      ready.value = true
      const reused = beforeId != null && detail.id === beforeId
      return { detail, reused }
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

  async function upsertCurrent(payload: TestdataGenFlushPayload) {
    if (!canUseRecords.value) return null
    let id = activeRecordId.value
    if (id == null) {
      const created = await createTestdataGenRecord()
      id = created.id
      activeRecordId.value = id
      activeDetail.value = created
    }
    const detail = await updateTestdataGenRecord(id, {
      originalText: payload.originalText,
      resultPython: payload.resultPython,
      solutionCode: payload.solutionCode ?? '',
    })
    activeRecordId.value = detail.id
    activeDetail.value = detail
    await refreshList()
    return detail
  }

  async function deleteRecord(id: number) {
    if (!canUseRecords.value) return
    await deleteTestdataGenRecord(id)
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
  }
}
