import { http } from './http'
import { unwrap, type ApiResult } from './types'

export interface TestdataGenRecordItem {
  id: number
  title: string
  createdAt: string
  updatedAt: string
}

export interface TestdataGenRecordDetail extends TestdataGenRecordItem {
  originalText: string
  resultPython: string
  solutionCode?: string
}

export interface TestdataGenRecordUpsertPayload {
  title?: string
  originalText?: string
  resultPython?: string
  solutionCode?: string
}

const BASE = '/api/auth/agents/testdata-gen/records'

export async function listTestdataGenRecords() {
  const { data } = await http.get<ApiResult<TestdataGenRecordItem[]>>(BASE)
  return unwrap(data)
}

export async function createTestdataGenRecord() {
  const { data } = await http.post<ApiResult<TestdataGenRecordDetail>>(BASE, {})
  return unwrap(data)
}

export async function getTestdataGenRecord(id: number) {
  const { data } = await http.get<ApiResult<TestdataGenRecordDetail>>(`${BASE}/${id}`)
  return unwrap(data)
}

export async function updateTestdataGenRecord(id: number, payload: TestdataGenRecordUpsertPayload) {
  const { data } = await http.put<ApiResult<TestdataGenRecordDetail>>(`${BASE}/${id}`, payload)
  return unwrap(data)
}

export async function deleteTestdataGenRecord(id: number) {
  const { data } = await http.delete<ApiResult<void>>(`${BASE}/${id}`)
  return unwrap(data)
}
