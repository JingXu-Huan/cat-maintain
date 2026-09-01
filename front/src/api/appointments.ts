import { getJson, postJson, putJson } from './http'
import type { Appointment, AppointmentPage } from '../types/appointment'

export function createAppointment(input: { storeId: number; orderId?: number; appointmentTime: string; vehiclePlate: string; vehicleModel?: string; remark?: string }) {
  return postJson<Appointment>('/api/appointments', input)
}

export function listMyAppointments() {
  return getJson<AppointmentPage>('/api/appointments?page=0&size=50')
}

export function cancelAppointment(id: number) {
  return putJson<Appointment>(`/api/appointments/${id}/cancel`)
}

export function listStoreAppointments() {
  return getJson<AppointmentPage>('/api/store/appointments?page=0&size=50')
}

export function confirmAppointment(id: number) {
  return putJson<Appointment>(`/api/store/appointments/${id}/confirm`)
}

export function rejectAppointment(id: number, reason: string) {
  return putJson<Appointment>(`/api/store/appointments/${id}/reject`, { reason })
}
