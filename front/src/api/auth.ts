import { postJson } from './http'
import type { AccountResponse, LoginResponse, StoreRegisterResponse } from '../types/auth'

export function registerUser(payload: { username: string; password: string; phone: string }) {
  return postJson<AccountResponse>('/api/auth/register/user', payload)
}

export function registerStore(payload: {
  username: string
  password: string
  storeName: string
  contactName: string
  phone: string
  address: string
}) {
  return postJson<StoreRegisterResponse>('/api/auth/register/store', payload)
}

export function login(payload: { username: string; password: string }) {
  return postJson<LoginResponse>('/api/auth/login', payload)
}
