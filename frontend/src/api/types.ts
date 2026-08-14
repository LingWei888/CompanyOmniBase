export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export function unwrap<T>(result: ApiResult<T>): T {
  if (result.code !== 0) {
    throw new Error(result.message || '请求失败')
  }
  return result.data
}
