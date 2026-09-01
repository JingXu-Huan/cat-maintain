import { getJson, postJson, putJson } from './http'
import type { MaintenancePage, MaintenanceRecord } from '../types/maintenance'

export function listMyMaintenanceRecords() {
  return getJson<MaintenancePage>('/api/maintenance-records?page=0&size=50')
}

export function listStoreMaintenanceRecords() {
  return getJson<MaintenancePage>('/api/store/maintenance-records?page=0&size=50')
}

export function startMaintenance(appointmentId: number, content: string) {
  return postJson<MaintenanceRecord>(`/api/store/maintenance-records/appointments/${appointmentId}/start`, { content })
}

export function completeMaintenance(id: number, input: { mileage?: number; content: string; remark?: string }) {
  return putJson<MaintenanceRecord>(`/api/store/maintenance-records/${id}/complete`, input)
}
