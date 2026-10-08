import { getJson, postJson, putJson } from './http'
import type { Appointment, AppointmentPage } from '../types/appointment'

export function createAppointment(input: { storeId: number; orderId?: number; appointmentTime: string; vehiclePlate: string; vehicleModel?: string; remark?: string }) {
  return postJson<Appointment>('/api/appointments', input)
}

export function listMyAppointments(page = 0, size = 20) {
  return getJson<AppointmentPage>(`/api/appointments?page=${page}&size=${size}`)
}

export function cancelAppointment(id: number) {
  return putJson<Appointment>(`/api/appointments/${id}/cancel`)
}

export function listStoreAppointments(page = 0, size = 20) {
  return getJson<AppointmentPage>(`/api/store/appointments?page=${page}&size=${size}`)
}

export function confirmAppointment(id: number) {
  return putJson<Appointment>(`/api/store/appointments/${id}/confirm`)
}

export function rejectAppointment(id: number, reason: string) {
  return putJson<Appointment>(`/api/store/appointments/${id}/reject`, { reason })
}
