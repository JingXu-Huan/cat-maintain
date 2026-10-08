<script setup lang="ts">
const props = withDefaults(defineProps<{ page: number; total: number; size?: number; disabled?: boolean }>(), { size: 20, disabled: false })
const emit = defineEmits<{ change: [page: number] }>()
</script>

<template>
  <nav v-if="props.total > props.size" class="pagination" aria-label="分页">
    <button class="secondary-button compact-button" type="button" :disabled="props.disabled || props.page === 0" @click="emit('change', props.page - 1)">上一页</button>
    <span>第 {{ props.page + 1 }} / {{ Math.ceil(props.total / props.size) }} 页 · 共 {{ props.total }} 条</span>
    <button class="secondary-button compact-button" type="button" :disabled="props.disabled || (props.page + 1) * props.size >= props.total" @click="emit('change', props.page + 1)">下一页</button>
  </nav>
</template>
