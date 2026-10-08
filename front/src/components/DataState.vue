<script setup lang="ts">
import { PackageOpen } from 'lucide'
import UiIcon from './UiIcon.vue'
withDefaults(defineProps<{ loading?: boolean; title?: string; description?: string; compact?: boolean }>(), { loading: false, title: '暂无数据', description: '', compact: false })
</script>

<template>
  <div class="data-state" :class="{ 'is-loading': loading, compact }" role="status" :aria-busy="loading">
    <template v-if="loading"><div class="skeleton-row" aria-hidden="true"></div><div class="skeleton-row short" aria-hidden="true"></div></template>
    <UiIcon v-else :icon="PackageOpen" :size="28" />
    <strong>{{ title }}</strong>
    <p v-if="description">{{ description }}</p>
    <slot />
  </div>
</template>
