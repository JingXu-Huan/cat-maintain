export type AppointmentStatus = 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'CANCELLED' | 'IN_PROGRESS' | 'COMPLETED'

export interface Appointment {
  id: number
  accountId: number
  storeId: number
  orderId: number | null
  appointmentTime: string
  status: AppointmentStatus
  vehiclePlate: string
  vehicleModel: string | null
  remark: string | null
  checkedInAt: string | null
  createdAt: string
}

export interface AppointmentPage {
  content: Appointment[]
  total: number
  page: number
  size: number
}
