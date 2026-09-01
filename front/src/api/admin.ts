import { getJson, putJson } from './http'
import type { StoreApplication } from '../types/store'

export function listStoreApplications(status = 'PENDING') {
  return getJson<StoreApplication[]>(`/api/admin/stores?status=${status}`)
}

export function approveStore(storeId: number) {
  return putJson<StoreApplication>(`/api/admin/stores/${storeId}/approve`)
}

export function rejectStore(storeId: number, reason: string) {
  return putJson<StoreApplication>(`/api/admin/stores/${storeId}/reject`, { reason })
}
