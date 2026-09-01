import { getJson, postJson } from './http'
import type { Review, ReviewPage } from '../types/review'

export function createReview(orderId: number, rating: number, content: string) {
  return postJson<Review>('/api/reviews', { orderId, rating, content })
}

export function listMyReviews() {
  return getJson<Review[]>('/api/reviews')
}

export function listStoreReviews(storeId: number) {
  return getJson<ReviewPage>(`/api/stores/${storeId}/reviews?page=0&size=20`)
}
