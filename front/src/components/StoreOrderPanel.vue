<script setup lang="ts">
import { toastSuccess } from '../composables/useToast'
import { onMounted, ref } from 'vue'
import { lookupOrderAppointments } from '../api/checkIn'
import { listStoreOrders, lookupStoreOrder, verifyStoreOrder } from '../api/orders'
import type { Appointment } from '../types/appointment'
import type { Order } from '../types/order'
import { formatTime, statusLabel } from '../utils/format'
import PaginationControls from './PaginationControls.vue'
import DataState from './DataState.vue'

const emit = defineEmits<{ changed: [] }>()
const code = ref('')
const order = ref<Order | null>(null)
const appointments = ref<Appointment[]>([])
const orders = ref<Order[]>([])
const page = ref(0)
const total = ref(0)
const feedback = ref('')
const hasError = ref(false)
const isLoading = ref(false)
async function refresh(nextPage = page.value) {
  isLoading.value = true
  if (hasError.value) { feedback.value = ''; hasError.value = false }
  try { const response = await listStoreOrders(nextPage, 20); orders.value = response.content; page.value = nextPage; total.value = response.total }
  catch (error) { hasError.value = true; feedback.value = error instanceof Error ? error.message : '门店订单加载失败。' }
  finally { isLoading.value = false }
}
async function lookup() {
  isLoading.value = true; feedback.value = ''
  try {
    const [foundOrder, foundAppointments] = await Promise.all([lookupStoreOrder(code.value), lookupOrderAppointments(code.value)])
    order.value = foundOrder; appointments.value = foundAppointments; hasError.value = false; toastSuccess('已找到当前门店的订单和关联预约。')
  } catch (error) { order.value = null; appointments.value = []; hasError.value = true; feedback.value = error instanceof Error ? error.message : '订单查询失败。' }
  finally { isLoading.value = false }
}
async function verify() {
  if (!order.value) return
  isLoading.value = true; feedback.value = ''
  try { order.value = await verifyStoreOrder(order.value.id, code.value); hasError.value = false; toastSuccess('订单已核销。'); await refresh(); emit('changed') }
  catch (error) { hasError.value = true; feedback.value = error instanceof Error ? error.message : '核销失败。' }
  finally { isLoading.value = false }
}
onMounted(() => refresh())
</script>

<template>
  <section class="admin-panel">
    <div class="admin-panel-heading"><div><h2>查询与核销</h2></div><button class="secondary-button" type="button" :disabled="isLoading" @click="refresh()">{{ isLoading ? '加载中…' : '刷新订单' }}</button></div>
    <form class="verify-form" @submit.prevent="lookup"><label>订单核销码<input v-model.trim="code" inputmode="numeric" pattern="[0-9]{8}" maxlength="8" required placeholder="输入 8 位数字" :disabled="isLoading" /></label><button class="primary-button" type="submit" :disabled="isLoading || !/^[0-9]{8}$/.test(code)">查询订单与预约</button></form>
    <p v-if="feedback && hasError" class="feedback error" role="alert">{{ feedback }}</p>
    <div v-if="order" class="verify-result">
      <article class="order-item"><div><strong>{{ order.orderNo }}</strong><p>{{ statusLabel(order.status) }} · ¥{{ order.totalAmount }} · {{ order.items.map((item) => `${item.productName} × ${item.quantity}`).join('，') }}</p></div><button class="approve-button" type="button" :disabled="isLoading || order.status !== 'DELIVERED'" @click="verify">{{ order.status === 'VERIFIED' || order.status === 'COMPLETED' ? '已核销' : '确认核销' }}</button></article>
      <article v-for="appointment in appointments" :key="appointment.id" class="history-item"><strong>关联预约 · {{ appointment.vehiclePlate }} · {{ statusLabel(appointment.status) }}</strong><p>{{ formatTime(appointment.appointmentTime) }} · {{ appointment.vehicleModel || '未填写车型' }}</p><p>{{ appointment.checkedInAt ? '到店登记：' + formatTime(appointment.checkedInAt) : '尚未扫码登记' }}</p></article>
      <p v-if="!appointments.length" class="admin-empty">该订单尚无关联预约。</p>
    </div>
    <h3>本店订单</h3>
    <DataState v-if="isLoading && !orders.length" loading title="正在加载门店订单" />
    <div class="order-list"><article v-for="item in orders" :key="item.id" class="order-item"><div><strong>{{ item.orderNo }}</strong><p><span class="status-badge">{{ statusLabel(item.status) }}</span> · ¥{{ item.totalAmount }}</p><p>{{ item.items.map((product) => `${product.productName} × ${product.quantity}`).join('，') }}</p></div><span v-if="item.verificationCode" class="verification-badge">核销码 {{ item.verificationCode }}</span></article><DataState v-if="!isLoading && !orders.length" :title="hasError ? '门店订单暂时未能加载' : '暂无门店订单'" :description="hasError ? '请点击刷新订单重试。' : '用户选择本店下单后，订单会显示在这里。'" /></div>
    <PaginationControls :page="page" :total="total" :disabled="isLoading" @change="refresh" />
  </section>
</template>
