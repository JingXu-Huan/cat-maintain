export interface Review {
  id: number
  accountId: number
  storeId: number
  orderId: number
  rating: number
  content: string | null
  createdAt: string
}

export interface ReviewPage {
  content: Review[]
  total: number
  page: number
  size: number
}
