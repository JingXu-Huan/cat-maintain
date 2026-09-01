<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { cancelAppointment, createAppointment, listMyAppointments } from '../api/appointments'
import { listMyMaintenanceRecords } from '../api/maintenance'
import { createReview, listMyReviews } from '../api/reviews'
import { listActiveStores } from '../api/stores'
import type { Appointment } from '../types/appointment'
import type { MaintenanceRecord } from '../types/maintenance'
import type { Review } from '../types/review'
import type { StoreSummary } from '../types/store'

const stores = ref<StoreSummary[]>([])
const appointments = ref<Appointment[]>([])
const records = ref<MaintenanceRecord[]>([])
const reviews = ref<Review[]>([])
const isLoading = ref(false)
const feedback = ref('')
const actingId = ref<number | null>(null)
const appointmentForm = ref({ storeId: null as number | null, orderId: '', appointmentTime: nextAppointmentTime(), vehiclePlate: '', vehicleModel: '', remark: '' })
const reviewForm = ref({ orderId: '', rating: 5, content: '' })

function nextAppointmentTime() {
  const date = new Date(Date.now() + 24 * 60 * 60 * 1000)
  date.setMinutes(0, 0, 0)
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16)
}

async function refresh() {
  isLoading.value = true
  feedback.value = ''
  try {
    const [availableStores, appointmentPage, maintenancePage, myReviews] = await Promise.all([listActiveStores(), listMyAppointments(), listMyMaintenanceRecords(), listMyReviews()])
    stores.value = availableStores
    appointments.value = appointmentPage.content
    records.value = maintenancePage.content
    reviews.value = myReviews
    appointmentForm.value.storeId ??= availableStores[0]?.id ?? null
  } catch (error) { feedback.value = error instanceof Error ? error.message : '服务数据加载失败。' } finally { isLoading.value = false }
}

async function submitAppointment() {
  if (!appointmentForm.value.storeId || !appointmentForm.value.vehiclePlate) { feedback.value = '请选择门店并填写车牌号。'; return }
  await run(async () => {
    await createAppointment({ ...appointmentForm.value, storeId: appointmentForm.value.storeId!, orderId: appointmentForm.value.orderId ? Number(appointmentForm.value.orderId) : undefined })
    feedback.value = '预约已提交，等待门店确认。'
    appointmentForm.value = { storeId: stores.value[0]?.id ?? null, orderId: '', appointmentTime: nextAppointmentTime(), vehiclePlate: '', vehicleModel: '', remark: '' }
    await refresh()
  })
}

async function cancel(id: number) { actingId.value = id; await run(async () => { await cancelAppointment(id); feedback.value = '预约已取消。'; await refresh() }); actingId.value = null }
async function submitReview() {
  if (!reviewForm.value.orderId) { feedback.value = '请填写已完成订单号。'; return }
  await run(async () => { await createReview(Number(reviewForm.value.orderId), reviewForm.value.rating, reviewForm.value.content); feedback.value = '评价已提交。'; reviewForm.value = { orderId: '', rating: 5, content: '' }; await refresh() })
}
async function run(action: () => Promise<void>) { try { await action() } catch (error) { feedback.value = error instanceof Error ? error.message : '操作失败。' } }
onMounted(refresh)
</script>

<template>
  <section class="service-panel">
    <div class="section-heading"><div><p class="eyebrow">USER SERVICE</p><h2>预约与保养历史</h2></div><button class="secondary-button" type="button" :disabled="isLoading" @click="refresh">刷新服务</button></div>
    <p v-if="feedback" class="feedback" :class="feedback.includes('失败') || feedback.includes('请') ? 'error' : 'success'" role="status">{{ feedback }}</p>
    <form class="service-form" @submit.prevent="submitAppointment">
      <select v-model="appointmentForm.storeId" required><option :value="null">选择服务门店</option><option v-for="store in stores" :key="store.id" :value="store.id">{{ store.storeName }}</option></select>
      <input v-model.trim="appointmentForm.orderId" placeholder="关联订单 ID（可选）" inputmode="numeric" />
      <input v-model="appointmentForm.appointmentTime" type="datetime-local" required />
      <input v-model.trim="appointmentForm.vehiclePlate" placeholder="车牌号" required maxlength="20" />
      <input v-model.trim="appointmentForm.vehicleModel" placeholder="车型（可选）" maxlength="100" />
      <input v-model.trim="appointmentForm.remark" class="service-form-wide" placeholder="预约备注（可选）" maxlength="500" />
      <button class="primary-button" type="submit" :disabled="isLoading">提交预约</button>
    </form>
    <div class="order-list"><article v-for="appointment in appointments" :key="appointment.id" class="order-item"><div><strong>预约 #{{ appointment.id }} · {{ appointment.vehiclePlate }}</strong><p>{{ appointment.status }} · 门店 #{{ appointment.storeId }} · {{ appointment.appointmentTime }}<span v-if="appointment.orderId"> · 订单 #{{ appointment.orderId }}</span></p></div><button v-if="appointment.status === 'PENDING' || appointment.status === 'CONFIRMED'" class="reject-button" type="button" :disabled="actingId !== null" @click="cancel(appointment.id)">取消</button></article><p v-if="!isLoading && appointments.length === 0" class="admin-empty">暂无预约。</p></div>
    <div class="history-grid"><div><h3>保养记录</h3><article v-for="record in records" :key="record.id" class="history-item"><strong>{{ record.serviceStartedAt }}</strong><p>{{ record.content }}<span v-if="record.mileage"> · {{ record.mileage }} km</span></p></article><p v-if="records.length === 0" class="admin-empty">暂无保养记录。</p></div><div><h3>提交评价</h3><form class="review-form" @submit.prevent="submitReview"><input v-model.trim="reviewForm.orderId" placeholder="已完成订单 ID" inputmode="numeric" required /><select v-model.number="reviewForm.rating"><option v-for="rating in [5, 4, 3, 2, 1]" :key="rating" :value="rating">{{ rating }} 分</option></select><textarea v-model.trim="reviewForm.content" maxlength="500" placeholder="服务体验（可选）"></textarea><button class="primary-button" type="submit" :disabled="isLoading">提交评价</button></form><p v-if="reviews.length > 0" class="review-summary">已评价 {{ reviews.length }} 个订单</p></div></div>
  </section>
</template>
