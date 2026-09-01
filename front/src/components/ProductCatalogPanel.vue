<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listProducts } from '../api/products'
import type { Product } from '../types/product'

const products = ref<Product[]>([])
const isLoading = ref(false)
const feedback = ref('')

async function refresh() {
  isLoading.value = true
  feedback.value = ''
  try {
    const page = await listProducts()
    products.value = page.content
  } catch (error) {
    feedback.value = error instanceof Error ? error.message : '商品加载失败。'
  } finally {
    isLoading.value = false
  }
}

onMounted(refresh)
</script>

<template>
  <section class="catalog-panel">
    <div class="section-heading">
      <div>
        <p class="eyebrow">PRODUCT CATALOG</p>
        <h2>可售配件</h2>
      </div>
      <button class="secondary-button" type="button" :disabled="isLoading" @click="refresh">
        {{ isLoading ? '加载中…' : '刷新商品' }}
      </button>
    </div>
    <p v-if="feedback" class="feedback error" role="alert">{{ feedback }}</p>
    <p v-else-if="!isLoading && products.length === 0" class="admin-empty">暂无可售商品。</p>
    <div v-else class="product-grid">
      <article v-for="product in products" :key="product.id" class="product-card">
        <div class="product-card-top">
          <span class="product-sku">{{ product.sku }}</span>
          <span class="stock-label">库存 {{ product.stock }}</span>
        </div>
        <h3>{{ product.productName }}</h3>
        <p>{{ product.brand || '通用配件' }}<span v-if="product.description"> · {{ product.description }}</span></p>
        <div class="product-price">¥{{ product.price }}<small> + 工时费 ¥{{ product.laborFee }}</small></div>
      </article>
    </div>
  </section>
</template>
