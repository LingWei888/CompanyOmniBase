import { http } from './http'
import { unwrap, type ApiResult } from './types'

export interface SysConfigItem {
  id?: number
  configKey: string
  configValue?: string
  remark?: string
}

export async function listSysConfigs() {
  const { data } = await http.get<ApiResult<SysConfigItem[]>>('/api/admin/system/config')
  return unwrap(data)
}

export async function saveSysConfigs(items: SysConfigItem[]) {
  const { data } = await http.put<ApiResult<SysConfigItem[]>>('/api/admin/system/config', items)
  return unwrap(data)
}

export async function uploadSiteLogo(file: File) {
  const form = new FormData()
  form.append('file', file)
  const { data } = await http.post<ApiResult<{ url: string }>>('/api/admin/system/config/logo', form, {
    timeout: 60000,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return unwrap(data)
}
