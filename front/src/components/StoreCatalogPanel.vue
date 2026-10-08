<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { MapPin, Phone, Store } from 'lucide'
import { listActiveStores } from '../api/stores'
import type { StoreSummary } from '../types/store'
import ReviewList from './ReviewList.vue'
import DataState from './DataState.vue'
import UiIcon from './UiIcon.vue'
const stores = ref<StoreSummary[]>([])
const error = ref('')
const selectedId = ref<number | null>(null)
const busy = ref(false)
async function load() {
  busy.value = true; error.value = ''
  try { stores.value = await listActiveStores() }
  catch (cause) { error.value = cause instanceof Error ? cause.message : '门店加载失败。' }
  finally { busy.value = false }
}
onMounted(load)
</script>

<template>
  <section class="catalog-panel">
    <div class="section-heading"><div><h2>加盟店</h2><p v-if="!busy && !error">{{ stores.length }} 家门店提供到店服务</p></div><button type="button" class="secondary-button" :disabled="busy" @click="load">{{ busy ? '加载中…' : '刷新门店' }}</button></div>
    <p v-if="error" class="feedback error" role="alert">{{ error }}</p>
    <DataState v-if="busy && !stores.length" loading title="正在加载门店" />
    <DataState v-else-if="!stores.length" :title="error ? '门店暂时未能加载' : '暂无可用门店'" :description="error ? '请点击刷新门店重试。' : '审核通过的门店会显示在这里，请稍后再来查看。'" />
    <div class="product-grid"><article v-for="store in stores" :key="store.id" class="product-card store-card">
      <div class="product-symbol"><UiIcon :icon="Store" :size="24" /></div>
      <h3>{{ store.storeName }}</h3><p class="store-contact"><UiIcon :icon="MapPin" :size="16" /><span>{{ store.address }}</span></p>
      <a :href="`tel:${store.phone}`" :aria-label="`联系 ${store.storeName}：${store.phone}`"><UiIcon :icon="Phone" :size="16" />{{ store.phone }}</a>
      <button class="secondary-button compact-button" type="button" :aria-expanded="selectedId === store.id" :aria-controls="selectedId === store.id ? `store-reviews-${store.id}` : undefined" @click="selectedId = selectedId === store.id ? null : store.id">{{ selectedId === store.id ? '收起评价' : '查看门店评价' }}</button>
      <ReviewList v-if="selectedId === store.id" :id="`store-reviews-${store.id}`" kind="store" :target-id="store.id" />
    </article></div>
  </section>
</template>
