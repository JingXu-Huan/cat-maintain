<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { approveOrder, deliverOrder, listAdminOrders, rejectOrder } from '../api/orders'
import type { Order } from '../types/order'
import { statusLabel } from '../utils/format'
import PaginationControls from './PaginationControls.vue'
import DataState from './DataState.vue'

const orders = ref<Order[]>([])
const feedback = ref('')
const isLoading = ref(false)
const page = ref(0)
const total = ref(0)
const hasError = ref(false)
const actingId = ref<number | null>(null)
const rejectReason = ref('订单信息需要补充')

async function refresh(nextPage = page.value) {
  isLoading.value = true
  if (hasError.value) { feedback.value = ''; hasError.value = false }
  try { const response = await listAdminOrders(nextPage, 20); orders.value = response.content; page.value = nextPage; total.value = response.total } catch (error) { hasError.value = true; feedback.value = error instanceof Error ? error.message : '订单加载失败。' } finally { isLoading.value = false }
}
async function act(id: number, action: () => Promise<Order>, success: string) {
  actingId.value = id
  feedback.value = ''
  try { await action(); hasError.value = false; feedback.value = success; await refresh() } catch (error) { hasError.value = true; feedback.value = error instanceof Error ? error.message : '订单操作失败。' } finally { actingId.value = null }
}
onMounted(() => refresh())
</script>

<template>
  <section class="admin-panel">
    <div class="admin-panel-heading"><div><h2>订单审批与配送</h2></div><button class="secondary-button" type="button" :disabled="isLoading || actingId !== null" @click="refresh()">{{ isLoading ? '加载中…' : '刷新订单' }}</button></div>
    <p v-if="feedback" class="feedback" :class="hasError ? 'error' : 'success'" :role="hasError ? 'alert' : 'status'">{{ feedback }}</p>
    <label class="reject-reason"><span>拒绝订单时的说明</span><input v-model.trim="rejectReason" maxlength="255" :disabled="isLoading || actingId !== null" /></label>
    <DataState v-if="isLoading && !orders.length" loading title="正在加载订单" />
    <div class="admin-product-list">
      <article v-for="order in orders" :key="order.id" class="admin-product-item">
        <div><strong>{{ order.orderNo }} · ¥{{ order.totalAmount }}</strong><p>{{ statusLabel(order.status) }} · 门店 #{{ order.storeId }} · 商品 {{ order.items.map((item) => `${item.productName} × ${item.quantity}`).join('，') }}</p><small v-if="order.verificationCode">核销码：{{ order.verificationCode }}</small></div>
        <div class="application-actions">
          <button v-if="order.status === 'PENDING_APPROVAL'" class="approve-button" type="button" :disabled="isLoading || actingId !== null" @click="act(order.id, () => approveOrder(order.id), '订单已通过。')">通过订单</button>
          <button v-if="order.status === 'PENDING_APPROVAL'" class="reject-button" type="button" :disabled="isLoading || actingId !== null || !rejectReason" @click="act(order.id, () => rejectOrder(order.id, rejectReason), '订单已拒绝。')">拒绝订单</button>
          <button v-if="order.status === 'APPROVED'" class="approve-button" type="button" :disabled="isLoading || actingId !== null" @click="act(order.id, () => deliverOrder(order.id), '订单已配送并生成核销码。')">确认配送</button>
        </div>
      </article>
      <DataState v-if="!isLoading && !orders.length" :title="hasError ? '订单暂时未能加载' : '暂无用户订单'" :description="hasError ? '请点击刷新订单重试。' : '用户提交订单后，你可以在这里审核并安排配送。'" />
    </div>
    <PaginationControls :page="page" :total="total" :disabled="isLoading || actingId !== null" @change="refresh" />
  </section>
</template>
