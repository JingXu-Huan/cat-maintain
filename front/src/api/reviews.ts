import { getJson, postJson } from './http'
import type { ProductReview, ProductReviewPage, Review, ReviewPage } from '../types/review'

export function createReview(orderId: number, rating: number, content: string) {
  return postJson<Review>('/api/reviews', { orderId, rating, content })
}

export function listMyReviews() {
  return getJson<Review[]>('/api/reviews')
}

export function listStoreReviews(storeId: number, page = 0, size = 20) {
  return getJson<ReviewPage>(`/api/stores/${storeId}/reviews?page=${page}&size=${size}`)
}

export function createProductReview(orderId: number, productId: number, rating: number, content: string) {
  return postJson<ProductReview>('/api/product-reviews', { orderId, productId, rating, content })
}

export function listMyProductReviews() {
  return getJson<ProductReview[]>('/api/product-reviews')
}

export function listProductReviews(productId: number, page = 0, size = 20) {
  return getJson<ProductReviewPage>(`/api/products/${productId}/reviews?page=${page}&size=${size}`)
}
