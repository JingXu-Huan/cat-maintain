import { getJson } from './http'
import type { StoreSummary } from '../types/store'

export function listActiveStores() {
  return getJson<StoreSummary[]>('/api/stores')
}
