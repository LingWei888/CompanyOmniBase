import { http } from './http'
import { unwrap, type ApiResult } from './types'

export interface SiteInfo {
  siteName: string
  siteDescription: string
  siteLogo: string
}

export interface PublicModelOption {
  id: number
  name: string
  modelName?: string | null
  remark?: string | null
}

export async function fetchSiteInfo() {
  const { data } = await http.get<ApiResult<SiteInfo>>('/api/public/site')
  return unwrap(data)
}

export async function fetchPublicModels() {
  const { data } = await http.get<ApiResult<PublicModelOption[]>>('/api/public/models')
  return unwrap(data)
}
