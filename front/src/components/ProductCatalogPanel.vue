<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Package, ShoppingCart } from 'lucide'
import { listProducts } from '../api/products'
import { useCart } from '../composables/useCart'
import type { Product } from '../types/product'
import PaginationControls from './PaginationControls.vue'
import ReviewList from './ReviewList.vue'
import DataState from './DataState.vue'
import UiIcon from './UiIcon.vue'
import { money, toCents } from '../utils/format'

const props = defineProps<{ canShop: boolean }>()
const emit = defineEmits<{ signIn: [] }>()
const { add, checkingOut } = useCart()
const products = ref<Product[]>([])
const page = ref(0)
const total = ref(0)
const isLoading = ref(false)
const feedback = ref('')
const hasError = ref(false)
const reviewProductId = ref<number | null>(null)
async function refresh(nextPage = page.value) {
  isLoading.value = true
  feedback.value = ''
  hasError.value = false
  try {
    const response = await listProducts(nextPage)
    products.value = response.content
    page.value = nextPage
    total.value = response.total
    reviewProductId.value = null
  } catch (error) { hasError.value = true; feedback.value = error instanceof Error ? error.message : '商品加载失败。' }
  finally { isLoading.value = false }
}
function addToCart(product: Product) {
  const error = add(product)
  hasError.value = error !== null
  feedback.value = error ?? `${product.productName} 已加入购物车。`
}
onMounted(() => refresh())
</script>

<template>
  <section id="catalog" class="catalog-panel">
    <div class="section-heading"><div><h2>可售配件</h2><p v-if="!isLoading && !hasError">共 {{ total }} 款可售配件</p></div><button class="secondary-button" type="button" :disabled="isLoading" @click="refresh()">{{ isLoading ? '加载中…' : '刷新商品' }}</button></div>
    <p v-if="feedback" class="feedback" :class="hasError ? 'error' : 'success'" :role="hasError ? 'alert' : 'status'">{{ feedback }}</p>
    <DataState v-if="isLoading && !products.length" loading title="正在加载配件" />
    <DataState v-else-if="!products.length" :title="hasError ? '商品暂时未能加载' : '暂无可售配件'" :description="hasError ? '请点击刷新商品重试。' : '配件上架后会显示在这里，可以先查看服务门店。'" />
    <div class="product-grid">
      <article v-for="product in products" :key="product.id" class="product-card">
        <div class="product-card-top"><span class="product-sku">{{ product.sku }}</span><span class="stock-label" :class="{ unavailable: product.stock === 0 }">{{ product.stock === 0 ? '暂时缺货' : `库存 ${product.stock}` }}</span></div>
        <div class="product-symbol"><UiIcon :icon="Package" :size="24" /></div>
        <h3>{{ product.productName }}</h3>
        <p>{{ product.brand || '通用配件' }}<span v-if="product.description"> · {{ product.description }}</span></p>
        <div class="product-price">¥{{ money(toCents(product.price)) }}<small>配件价</small></div>
        <div class="product-labor">工时费 ¥{{ money(toCents(product.laborFee)) }} / 件</div>
        <div class="product-actions">
          <button v-if="props.canShop" class="primary-button compact-button" type="button" :disabled="checkingOut || product.stock === 0" @click="addToCart(product)"><UiIcon :icon="ShoppingCart" :size="16" />{{ product.stock === 0 ? '暂时缺货' : '加入购物车' }}</button>
          <button v-else class="primary-button compact-button" type="button" @click="emit('signIn')">登录后购买</button>
          <button class="secondary-button compact-button" type="button" :aria-expanded="reviewProductId === product.id" :aria-controls="reviewProductId === product.id ? `product-reviews-${product.id}` : undefined" @click="reviewProductId = reviewProductId === product.id ? null : product.id">{{ reviewProductId === product.id ? '收起评价' : '查看评价' }}</button>
        </div>
        <ReviewList v-if="reviewProductId === product.id" :id="`product-reviews-${product.id}`" kind="product" :target-id="product.id" />
      </article>
    </div>
    <PaginationControls :page="page" :total="total" :disabled="isLoading" @change="refresh" />
  </section>
</template>
