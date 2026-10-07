import { deleteJson, getJson, postJson, putJson } from './http'
import type { Product, ProductPage, ProductStatus } from '../types/product'

export function listProducts(page = 0, size = 20) {
  return getJson<ProductPage>(`/api/products?page=${page}&size=${size}`)
}

export function listAdminProducts(status?: ProductStatus, page = 0, size = 50) {
  const query = new URLSearchParams({ page: String(page), size: String(size) })
  if (status) query.set('status', status)
  return getJson<ProductPage>(`/api/admin/products?${query.toString()}`)
}

export function createProduct(input: {
  sku: string
  productName: string
  brand?: string
  description?: string
  price: string
  laborFee: string
  stock: number
}) {
  return postJson<Product>('/api/admin/products', input)
}

export function updateProductStatus(id: number, status: ProductStatus) {
  return putJson<Product>(`/api/admin/products/${id}/status`, { status })
}

export function adjustProductStock(id: number, delta: number) {
  return postJson<Product>(`/api/admin/products/${id}/stock`, { delta })
}

export function deleteProduct(id: number) {
  return deleteJson<void>(`/api/admin/products/${id}`)
}
