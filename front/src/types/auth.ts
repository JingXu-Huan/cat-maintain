export interface AccountResponse {
  id: number
  username: string
  phone: string
  role: 'USER' | 'STORE' | 'ADMIN'
  status: 'PENDING' | 'ACTIVE' | 'REJECTED'
}

export interface LoginResponse {
  account: AccountResponse
  message: string
}

export interface StoreRegisterResponse {
  accountId: number
  username: string
  status: 'PENDING'
  message: string
}
