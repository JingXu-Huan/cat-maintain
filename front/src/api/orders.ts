import { getJson, postJson, putJson } from './http'
import type { CartItemInput, Order, OrderPage } from '../types/order'

export function createOrder(storeId: number, items: CartItemInput[]) {
  return postJson<Order>('/api/orders', { storeId, items })
}

export function listMyOrders(page = 0, size = 20) {
  return getJson<OrderPage>(`/api/orders?page=${page}&size=${size}`)
}

export function listAdminOrders(page = 0, size = 50) {
  return getJson<OrderPage>(`/api/admin/orders?page=${page}&size=${size}`)
}

export function approveOrder(id: number) {
  return putJson<Order>(`/api/admin/orders/${id}/approve`)
}

export function rejectOrder(id: number, reason: string) {
  return putJson<Order>(`/api/admin/orders/${id}/reject`, { reason })
}

export function deliverOrder(id: number) {
  return putJson<Order>(`/api/admin/orders/${id}/deliver`)
}

export function listStoreOrders(page = 0, size = 50) {
  return getJson<OrderPage>(`/api/store/orders?page=${page}&size=${size}`)
}

export function lookupStoreOrder(code: string) {
  return getJson<Order>(`/api/store/orders/lookup?code=${encodeURIComponent(code)}`)
}

export function verifyStoreOrder(id: number, code: string) {
  return putJson<Order>(`/api/store/orders/${id}/verify`, { verificationCode: code })
}
