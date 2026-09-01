<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listProducts } from '../api/products'
import { createOrder, listMyOrders } from '../api/orders'
import { listActiveStores } from '../api/stores'
import type { Order } from '../types/order'
import type { Product } from '../types/product'
import type { StoreSummary } from '../types/store'

const products = ref<Product[]>([])
const stores = ref<StoreSummary[]>([])
const orders = ref<Order[]>([])
const selectedStoreId = ref<number | null>(null)
const quantities = ref<Record<number, number>>({})
const isLoading = ref(false)
const feedback = ref('')

async function refresh() {
  isLoading.value = true
  feedback.value = ''
  try {
    const [productPage, availableStores, orderPage] = await Promise.all([listProducts(), listActiveStores(), listMyOrders()])
    products.value = productPage.content
    stores.value = availableStores
    orders.value = orderPage.content
    selectedStoreId.value ??= availableStores[0]?.id ?? null
  } catch (error) {
    feedback.value = error instanceof Error ? error.message : '订单数据加载失败。'
  } finally {
    isLoading.value = false
  }
}

async function submitOrder() {
  const items = products.value
    .map((product) => ({ productId: product.id, quantity: quantities.value[product.id] || 0 }))
    .filter((item) => item.quantity > 0)
  if (!selectedStoreId.value || items.length === 0) {
    feedback.value = '请选择门店并至少填写一个商品数量。'
    return
  }
  isLoading.value = true
  feedback.value = ''
  try {
    const order = await createOrder(selectedStoreId.value, items)
    feedback.value = `订单 ${order.orderNo} 已提交，等待平台审核。`
    quantities.value = {}
    await refresh()
  } catch (error) {
    feedback.value = error instanceof Error ? error.message : '下单失败。'
  } finally {
    isLoading.value = false
  }
}

onMounted(refresh)
</script>

<template>
  <section class="order-panel">
    <div class="section-heading">
      <div><p class="eyebrow">USER ORDERS</p><h2>选配件并下单</h2></div>
      <button class="secondary-button" type="button" :disabled="isLoading" @click="refresh">刷新订单</button>
    </div>
    <p v-if="feedback" class="feedback" :class="feedback.includes('失败') || feedback.includes('请选择') ? 'error' : 'success'" role="status">{{ feedback }}</p>
    <div class="order-form-row">
      <label><span>服务门店</span><select v-model="selectedStoreId"><option v-for="store in stores" :key="store.id" :value="store.id">{{ store.storeName }} · {{ store.address }}</option></select></label>
      <button class="primary-button" type="button" :disabled="isLoading || stores.length === 0" @click="submitOrder">提交订单</button>
    </div>
    <div class="cart-list">
      <label v-for="product in products" :key="product.id" class="cart-item">
        <span><strong>{{ product.productName }}</strong><small>¥{{ product.price }} + 工时费 ¥{{ product.laborFee }} · 库存 {{ product.stock }}</small></span>
        <input v-model.number="quantities[product.id]" type="number" min="0" :max="product.stock" />
      </label>
    </div>
    <div class="order-list">
      <article v-for="order in orders" :key="order.id" class="order-item">
        <div><strong>{{ order.orderNo }}</strong><p>{{ order.status }} · ¥{{ order.totalAmount }} · {{ order.items.map((item) => `${item.productName} × ${item.quantity}`).join('，') }}</p></div>
        <span v-if="order.verificationCode" class="verification-badge">核销码 {{ order.verificationCode }}</span>
      </article>
      <p v-if="!isLoading && orders.length === 0" class="admin-empty">还没有订单。</p>
    </div>
  </section>
</template>
