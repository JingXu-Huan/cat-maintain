<script setup lang="ts">
import type { IconNode } from 'lucide'
withDefaults(defineProps<{ icon: IconNode; size?: number }>(), { size: 20 })
</script>

<template>
  <svg :width="size" :height="size" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false" class="ui-icon">
    <component :is="node[0]" v-for="(node, index) in icon" :key="index" v-bind="node[1]" />
  </svg>
</template>
