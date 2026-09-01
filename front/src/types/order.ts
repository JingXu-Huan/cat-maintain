export interface CartItemInput {
  productId: number
  quantity: number
}

export interface OrderItem {
  productId: number
  productName: string
  quantity: number
  unitPrice: string
  unitLaborFee: string
  lineTotal: string
}

export type OrderStatus = 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'DELIVERED' | 'VERIFIED' | 'COMPLETED' | 'CANCELLED'

export interface Order {
  id: number
  orderNo: string
  accountId: number
  storeId: number
  status: OrderStatus
  productAmount: string
  laborFeeAmount: string
  totalAmount: string
  verificationCode: string | null
  rejectedReason: string | null
  approvedAt: string | null
  deliveredAt: string | null
  verifiedAt: string | null
  verifiedByStoreId: number | null
  completedAt: string | null
  createdAt: string
  items: OrderItem[]
}

export interface OrderPage {
  content: Order[]
  total: number
  page: number
  size: number
}
