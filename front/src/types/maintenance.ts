export interface MaintenanceRecord {
  id: number
  appointmentId: number
  orderId: number | null
  accountId: number
  storeId: number
  serviceStartedAt: string
  serviceCompletedAt: string | null
  mileage: number | null
  content: string
  remark: string | null
  createdAt: string
}

export interface MaintenancePage {
  content: MaintenanceRecord[]
  total: number
  page: number
  size: number
}
