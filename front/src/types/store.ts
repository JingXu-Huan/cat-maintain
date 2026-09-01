export interface StoreApplication {
  id: number
  accountId: number
  username: string
  storeName: string
  contactName: string
  phone: string
  address: string
  status: 'PENDING' | 'ACTIVE' | 'REJECTED'
  reviewRemark: string | null
  reviewedAt: string | null
}
