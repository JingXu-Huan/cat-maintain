<script setup lang="ts">
import { ref } from 'vue'
import { lookupStoreOrder, verifyStoreOrder } from '../api/orders'
import type { Order } from '../types/order'

const code = ref('')
const order = ref<Order | null>(null)
const feedback = ref('')
const isLoading = ref(false)

async function lookup() {
  isLoading.value = true
  feedback.value = ''
  try { order.value = await lookupStoreOrder(code.value); feedback.value = '已找到当前门店订单。' } catch (error) { order.value = null; feedback.value = error instanceof Error ? error.message : '订单查询失败。' } finally { isLoading.value = false }
}
async function verify() {
  if (!order.value) return
  isLoading.value = true
  feedback.value = ''
  try { order.value = await verifyStoreOrder(order.value.id, code.value); feedback.value = '核销成功；重复点击不会重复处理。' } catch (error) { feedback.value = error instanceof Error ? error.message : '核销失败。' } finally { isLoading.value = false }
}
</script>

<template>
  <section class="admin-panel">
    <div class="admin-panel-heading"><div><p class="eyebrow">STORE CHECK-IN</p><h2>数字凭据核销</h2></div></div>
    <div class="verify-form"><input v-model.trim="code" inputmode="numeric" maxlength="8" placeholder="输入 8 位核销码" /><button class="primary-button" type="button" :disabled="isLoading || code.length !== 8" @click="lookup">查询</button></div>
    <p v-if="feedback" class="feedback" :class="feedback.includes('失败') || feedback.includes('没有') ? 'error' : 'success'" role="status">{{ feedback }}</p>
    <article v-if="order" class="order-item verify-result"><div><strong>{{ order.orderNo }}</strong><p>{{ order.status }} · ¥{{ order.totalAmount }} · 商品 {{ order.items.map((item) => `${item.productName} × ${item.quantity}`).join('，') }}</p></div><button class="approve-button" type="button" :disabled="isLoading || order.status === 'COMPLETED'" @click="verify">{{ order.status === 'VERIFIED' ? '再次确认' : '确认核销' }}</button></article>
  </section>
</template>
