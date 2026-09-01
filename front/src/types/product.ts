export type ProductStatus = 'ACTIVE' | 'INACTIVE'

export interface Product {
  id: number
  sku: string
  productName: string
  brand: string | null
  description: string | null
  price: string
  laborFee: string
  stock: number
  status: ProductStatus
  createdAt: string
  updatedAt: string
}

export interface ProductPage {
  content: Product[]
  total: number
  page: number
  size: number
}
