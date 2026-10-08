<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    page: number
    total: number
    size?: number
    disabled?: boolean
    alwaysVisible?: boolean
    showPageNumbers?: boolean
    sizeOptions?: number[]
  }>(),
  { size: 20, disabled: false, alwaysVisible: false, showPageNumbers: false, sizeOptions: () => [] },
)
const emit = defineEmits<{ change: [page: number]; sizeChange: [size: number] }>()
const pageCount = computed(() => Math.max(1, Math.ceil(props.total / props.size)))
const pageNumbers = computed<(number | 'start-gap' | 'end-gap')[]>(() => {
  const count = pageCount.value
  if (count <= 7) return Array.from({ length: count }, (_, index) => index)
  const start = Math.max(1, Math.min(props.page - 1, count - 4))
  const end = Math.min(count - 2, Math.max(props.page + 1, 3))
  const pages: (number | 'start-gap' | 'end-gap')[] = [0]
  if (start > 1) pages.push('start-gap')
  for (let page = start; page <= end; page++) pages.push(page)
  if (end < count - 2) pages.push('end-gap')
  pages.push(count - 1)
  return pages
})

function changeSize(event: Event) {
  const select = event.target as HTMLSelectElement
  const size = Number(select.value)
  select.value = String(props.size)
  if (props.sizeOptions.includes(size) && size !== props.size && !props.disabled) {
    emit('sizeChange', size)
  }
}
</script>

<template>
  <nav v-if="props.total > 0 && (props.alwaysVisible || props.total > props.size)" class="pagination" aria-label="分页">
    <label v-if="props.sizeOptions.length" class="pagination-size">
      每页
      <select :value="props.size" :disabled="props.disabled" aria-label="每页商品数量" @change="changeSize">
        <option v-for="size in props.sizeOptions" :key="size" :value="size">{{ size }} 款</option>
      </select>
    </label>
    <button
      class="secondary-button compact-button"
      type="button"
      :disabled="props.disabled || props.page === 0"
      @click="emit('change', props.page - 1)"
    >
      上一页
    </button>
    <div v-if="props.showPageNumbers" class="pagination-pages">
      <template v-for="page in pageNumbers" :key="page">
        <button
          v-if="typeof page === 'number'"
          class="secondary-button pagination-page"
          :class="{ active: props.page === page }"
          type="button"
          :aria-label="`第 ${page + 1} 页`"
          :aria-current="props.page === page ? 'page' : undefined"
          :disabled="props.disabled || props.page === page"
          @click="emit('change', page)"
        >
          {{ page + 1 }}
        </button>
        <span v-else aria-hidden="true">…</span>
      </template>
    </div>
    <span aria-live="polite">第 {{ props.page + 1 }} / {{ pageCount }} 页 · 共 {{ props.total }} 条</span>
    <button
      class="secondary-button compact-button"
      type="button"
      :disabled="props.disabled || (props.page + 1) * props.size >= props.total"
      @click="emit('change', props.page + 1)"
    >
      下一页
    </button>
  </nav>
</template>
