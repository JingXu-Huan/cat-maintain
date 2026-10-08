<script setup lang="ts">
import { ref, watch } from 'vue'
import { Star } from 'lucide'
import { listProductReviews, listStoreReviews } from '../api/reviews'
import { formatTime } from '../utils/format'
import PaginationControls from './PaginationControls.vue'
import UiIcon from './UiIcon.vue'

const props = defineProps<{ kind: 'product' | 'store'; targetId: number }>()
const reviews = ref<{ id: number; rating: number; content: string | null; createdAt: string }[]>([])
const page = ref(0)
const total = ref(0)
const busy = ref(false)
const error = ref('')
let requestId = 0

async function load(nextPage = 0) {
  const id = ++requestId
  busy.value = true
  error.value = ''
  try {
    const response = props.kind === 'product'
      ? await listProductReviews(props.targetId, nextPage)
      : await listStoreReviews(props.targetId, nextPage)
    if (id !== requestId) return
    reviews.value = response.content
    total.value = response.total
    page.value = nextPage
  } catch (cause) {
    if (id === requestId) error.value = cause instanceof Error ? cause.message : '评价加载失败。'
  } finally { if (id === requestId) busy.value = false }
}
watch(() => [props.kind, props.targetId], () => { reviews.value = []; total.value = 0; void load() }, { immediate: true })
</script>

<template>
  <div class="review-list" aria-live="polite">
    <p v-if="error" class="feedback error" role="alert">{{ error }} <button type="button" class="secondary-button compact-button" :disabled="busy" @click="load(page)">重试</button></p>
    <p v-else-if="busy">正在加载评价…</p>
    <p v-else-if="reviews.length === 0" class="admin-empty">暂无评价。</p>
    <article v-for="review in reviews" :key="review.id" class="history-item">
      <strong class="review-rating"><span class="rating-stars" aria-hidden="true"><UiIcon v-for="rating in 5" :key="rating" :icon="Star" :size="14" :class="{ filled: rating <= review.rating }" /></span>{{ review.rating }} / 5 分</strong>
      <p>{{ review.content || '用户未填写文字评价' }}</p><small>{{ formatTime(review.createdAt) }}</small>
    </article>
    <PaginationControls :page="page" :total="total" :disabled="busy" @change="load" />
  </div>
</template>
