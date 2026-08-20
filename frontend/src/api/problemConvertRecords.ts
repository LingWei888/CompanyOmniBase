import { http } from './http'
import { unwrap, type ApiResult } from './types'

export interface ProblemConvertRecordItem {
  id: number
  title: string
  referenceNickname: string
  createdAt: string
  updatedAt: string
}

export interface ProblemConvertRecordDetail extends ProblemConvertRecordItem {
  originalText: string
  resultMarkdown: string
  solutionCode?: string
}

export interface ProblemConvertRecordUpsertPayload {
  title?: string
  referenceNickname?: string
  originalText?: string
  resultMarkdown?: string
  solutionCode?: string
}

const BASE = '/api/auth/agents/problem-convert/records'

export async function listProblemConvertRecords() {
  const { data } = await http.get<ApiResult<ProblemConvertRecordItem[]>>(BASE)
  return unwrap(data)
}

export async function createProblemConvertRecord() {
  const { data } = await http.post<ApiResult<ProblemConvertRecordDetail>>(BASE, {})
  return unwrap(data)
}

export async function getProblemConvertRecord(id: number) {
  const { data } = await http.get<ApiResult<ProblemConvertRecordDetail>>(`${BASE}/${id}`)
  return unwrap(data)
}

export async function updateProblemConvertRecord(id: number, payload: ProblemConvertRecordUpsertPayload) {
  const { data } = await http.put<ApiResult<ProblemConvertRecordDetail>>(`${BASE}/${id}`, payload)
  return unwrap(data)
}

export async function deleteProblemConvertRecord(id: number) {
  const { data } = await http.delete<ApiResult<void>>(`${BASE}/${id}`)
  return unwrap(data)
}
