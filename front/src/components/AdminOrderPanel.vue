<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { approveOrder, deliverOrder, listAdminOrders, rejectOrder } from '../api/orders'
import type { Order } from '../types/order'

const orders = ref<Order[]>([])
const feedback = ref('')
const isLoading = ref(false)
const actingId = ref<number | null>(null)
const rejectReason = ref('订单信息需要补充')

async function refresh() {
  isLoading.value = true
  try { orders.value = (await listAdminOrders()).content } catch (error) { feedback.value = error instanceof Error ? error.message : '订单加载失败。' } finally { isLoading.value = false }
}
async function act(id: number, action: () => Promise<Order>, success: string) {
  actingId.value = id
  feedback.value = ''
  try { await action(); feedback.value = success; await refresh() } catch (error) { feedback.value = error instanceof Error ? error.message : '订单操作失败。' } finally { actingId.value = null }
}
onMounted(refresh)
</script>

<template>
  <section class="admin-panel">
    <div class="admin-panel-heading"><div><p class="eyebrow">ADMIN ORDERS</p><h2>订单审批与配送</h2></div><button class="secondary-button" type="button" :disabled="isLoading" @click="refresh">刷新订单</button></div>
    <p v-if="feedback" class="feedback" :class="feedback.includes('失败') ? 'error' : 'success'" role="status">{{ feedback }}</p>
    <label class="reject-reason"><span>拒绝原因</span><input v-model.trim="rejectReason" maxlength="255" /></label>
    <div class="admin-product-list">
      <article v-for="order in orders" :key="order.id" class="admin-product-item">
        <div><strong>{{ order.orderNo }} · ¥{{ order.totalAmount }}</strong><p>{{ order.status }} · 门店 #{{ order.storeId }} · 商品 {{ order.items.map((item) => `${item.productName} × ${item.quantity}`).join('，') }}</p><small v-if="order.verificationCode">核销码：{{ order.verificationCode }}</small></div>
        <div class="application-actions">
          <button v-if="order.status === 'PENDING_APPROVAL'" class="approve-button" type="button" :disabled="actingId !== null" @click="act(order.id, () => approveOrder(order.id), '订单已通过。')">通过</button>
          <button v-if="order.status === 'PENDING_APPROVAL'" class="reject-button" type="button" :disabled="actingId !== null" @click="act(order.id, () => rejectOrder(order.id, rejectReason), '订单已拒绝。')">拒绝</button>
          <button v-if="order.status === 'APPROVED'" class="approve-button" type="button" :disabled="actingId !== null" @click="act(order.id, () => deliverOrder(order.id), '订单已配送并生成核销码。')">配送</button>
        </div>
      </article>
      <p v-if="!isLoading && orders.length === 0" class="admin-empty">暂无订单。</p>
    </div>
  </section>
</template>
