import { getJson, postJson } from './http'
import type { Appointment } from '../types/appointment'
import type { Order } from '../types/order'
import type { StoreSummary } from '../types/store'

export function getStoreProfile() {
  return getJson<StoreSummary>('/api/store/profile')
}

export function lookupOrderAppointments(code: string) {
  return getJson<Appointment[]>(`/api/store/orders/lookup-appointments?code=${encodeURIComponent(code)}`)
}

export function checkIn(storeId: number, appointmentId: number, verificationCode: string) {
  return postJson<{ appointment: Appointment; order: Order }>(`/api/stores/${storeId}/check-ins`, { appointmentId, verificationCode })
}
