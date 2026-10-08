<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { cancelAppointment, createAppointment, listMyAppointments } from '../api/appointments'
import { listMyMaintenanceRecords } from '../api/maintenance'
import { listMyOrders } from '../api/orders'
import { createProductReview, createReview, listMyProductReviews, listMyReviews } from '../api/reviews'
import { listActiveStores } from '../api/stores'
import type { Appointment } from '../types/appointment'
import type { MaintenanceRecord } from '../types/maintenance'
import type { Order } from '../types/order'
import type { ProductReview, Review } from '../types/review'
import type { StoreSummary } from '../types/store'
import { formatTime, statusLabel } from '../utils/format'
import { loadAllPages } from '../utils/paging'
import PaginationControls from './PaginationControls.vue'
import ConfirmDialog from './ConfirmDialog.vue'
import DataState from './DataState.vue'

const props = defineProps<{ orderToBook: Order | null; revision: number }>()
const emit = defineEmits<{ changed: [] }>()
const stores = ref<StoreSummary[]>([])
const appointments = ref<Appointment[]>([])
const records = ref<MaintenanceRecord[]>([])
const orders = ref<Order[]>([])
const reviews = ref<Review[]>([])
const productReviews = ref<ProductReview[]>([])
const appointmentPage = ref(0)
const appointmentTotal = ref(0)
const recordPage = ref(0)
const recordTotal = ref(0)
const loading = ref(false)
const processing = ref(false)
const feedback = ref('')
const hasError = ref(false)
const cancellingAppointment = ref<Appointment | null>(null)
const confirmingCancel = ref(false)
const appointmentForm = ref({ storeId: null as number | null, orderId: null as number | null, appointmentTime: nextAppointmentTime(), vehiclePlate: '', vehicleModel: '', remark: '' })
const reviewKind = ref<'store' | 'product'>('store')
const reviewForm = ref({ orderId: null as number | null, productId: null as number | null, rating: 5, content: '' })
const bookableOrders = computed(() => orders.value.filter((order) => ['APPROVED', 'DELIVERED', 'VERIFIED'].includes(order.status)))
const reviewableOrders = computed(() => orders.value.filter((order) => order.status === 'COMPLETED' && (reviewKind.value === 'store'
  ? !reviews.value.some((review) => review.orderId === order.id)
  : order.items.some((item) => !productReviews.value.some((review) => review.orderId === order.id && review.productId === item.productId)))))
const reviewableProducts = computed(() => orders.value.find((order) => order.id === reviewForm.value.orderId)?.items.filter((item) =>
  !productReviews.value.some((review) => review.orderId === reviewForm.value.orderId && review.productId === item.productId)) ?? [])

function nextAppointmentTime() {
  const date = new Date(Date.now() + 86400000)
  date.setMinutes(0, 0, 0)
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16)
}
async function refresh() {
  loading.value = true
  if (hasError.value) { feedback.value = ''; hasError.value = false }
  try {
    const [availableStores, a, r, myReviews, myProductReviews, myOrders] = await Promise.all([
      listActiveStores(), listMyAppointments(appointmentPage.value), listMyMaintenanceRecords(recordPage.value),
      listMyReviews(), listMyProductReviews(), loadAllPages(listMyOrders),
    ])
    stores.value = availableStores; appointments.value = a.content; appointmentTotal.value = a.total
    records.value = r.content; recordTotal.value = r.total; reviews.value = myReviews
    productReviews.value = myProductReviews; orders.value = myOrders
    appointmentForm.value.storeId ??= availableStores[0]?.id ?? null
  } catch (error) { hasError.value = true; feedback.value = error instanceof Error ? error.message : '服务数据加载失败。' }
  finally { loading.value = false }
}
async function run(action: () => Promise<unknown>, success: string) {
  processing.value = true; feedback.value = ''
  try { await action(); hasError.value = false; feedback.value = success; await refresh(); emit('changed') }
  catch (error) { hasError.value = true; feedback.value = error instanceof Error ? error.message : '操作失败。' }
  finally { processing.value = false }
}
async function submitAppointment() {
  const form = appointmentForm.value
  if (!form.storeId) { hasError.value = true; feedback.value = '请选择服务门店。'; return }
  await run(async () => {
    await createAppointment({ ...form, storeId: form.storeId!, orderId: form.orderId ?? undefined })
    appointmentForm.value.orderId = null
    appointmentForm.value.vehiclePlate = ''
    appointmentForm.value.remark = ''
    appointmentPage.value = 0
  }, '预约已提交，等待门店确认。')
}
async function submitReview() {
  const form = reviewForm.value
  if (!form.orderId || (reviewKind.value === 'product' && !form.productId)) return
  await run(async () => {
    if (reviewKind.value === 'product') await createProductReview(form.orderId!, form.productId!, form.rating, form.content)
    else await createReview(form.orderId!, form.rating, form.content)
    reviewForm.value.content = ''
  }, '评价已提交。')
}
function requestCancel(appointment: Appointment) { cancellingAppointment.value = appointment; confirmingCancel.value = true }
function confirmCancel() {
  if (cancellingAppointment.value) {
    const id = cancellingAppointment.value.id
    void run(() => cancelAppointment(id), '预约已取消。')
  }
}
watch(() => appointmentForm.value.orderId, (id) => {
  const order = orders.value.find((item) => item.id === id)
  if (order) appointmentForm.value.storeId = order.storeId
})
watch(() => props.orderToBook, (order) => {
  if (order) { appointmentForm.value.orderId = order.id; appointmentForm.value.storeId = order.storeId }
}, { immediate: true })
watch(reviewableOrders, (available) => {
  if (!available.some((order) => order.id === reviewForm.value.orderId)) reviewForm.value.orderId = available[0]?.id ?? null
})
watch(reviewableProducts, (available) => {
  if (!available.some((item) => item.productId === reviewForm.value.productId)) reviewForm.value.productId = available[0]?.productId ?? null
})
watch(() => props.revision, () => { void refresh() })
function changeAppointmentPage(page: number) { appointmentPage.value = page; void refresh() }
function changeRecordPage(page: number) { recordPage.value = page; void refresh() }
onMounted(refresh)
</script>

<template>
  <section id="services" class="service-panel">
    <div class="section-heading"><div><h2>预约保养</h2></div><button class="secondary-button" type="button" :disabled="loading || processing" @click="refresh">{{ loading ? '加载中…' : '刷新服务' }}</button></div>
    <p v-if="feedback" class="feedback" :class="hasError ? 'error' : 'success'" :role="hasError ? 'alert' : 'status'">{{ feedback }}</p>
    <p class="panel-description">关联购买订单后，服务门店会与订单门店保持一致；也可以直接预约到店服务。</p>
    <form class="service-form" :aria-busy="processing" @submit.prevent="submitAppointment">
      <label>购买订单<select v-model="appointmentForm.orderId" :disabled="loading || processing"><option :value="null">不关联订单（到店服务）</option><option v-for="order in bookableOrders" :key="order.id" :value="order.id">{{ order.orderNo }} · {{ statusLabel(order.status) }}</option></select></label>
      <label>保养门店<select v-model="appointmentForm.storeId" required :disabled="loading || appointmentForm.orderId !== null || processing"><option :value="null">选择服务门店</option><option v-for="store in stores" :key="store.id" :value="store.id">{{ store.storeName }}</option></select></label>
      <label>预约时间<input v-model="appointmentForm.appointmentTime" type="datetime-local" required :disabled="processing" /></label>
      <label>车牌号<input v-model.trim="appointmentForm.vehiclePlate" required maxlength="20" placeholder="例如：苏A12345" :disabled="processing" /></label>
      <label>车型（可选）<input v-model.trim="appointmentForm.vehicleModel" maxlength="100" placeholder="品牌及车型" :disabled="processing" /></label>
      <label>备注（可选）<input v-model.trim="appointmentForm.remark" maxlength="500" placeholder="需要门店提前了解的事项" :disabled="processing" /></label>
      <div class="form-actions"><button class="primary-button" type="submit" :disabled="loading || processing || !appointmentForm.storeId">{{ processing ? '处理中…' : '提交预约' }}</button></div>
    </form>
    <h3>我的预约</h3>
    <DataState v-if="loading && !appointments.length" loading compact title="正在加载预约" />
    <div class="order-list">
      <article v-for="appointment in appointments" :key="appointment.id" class="order-item">
        <div><strong>{{ appointment.vehiclePlate }} · {{ statusLabel(appointment.status) }}</strong><p>{{ stores.find((store) => store.id === appointment.storeId)?.storeName }} · {{ formatTime(appointment.appointmentTime) }}</p><p v-if="appointment.checkedInAt">已到店登记：{{ formatTime(appointment.checkedInAt) }}</p><p v-if="appointment.remark">{{ appointment.remark }}</p></div>
        <button v-if="!appointment.checkedInAt && ['PENDING', 'CONFIRMED'].includes(appointment.status)" class="reject-button" type="button" :disabled="processing || loading" @click="requestCancel(appointment)">取消预约</button>
      </article>
      <DataState v-if="!loading && !appointments.length" compact :title="hasError ? '预约暂时未能加载' : '还没有预约'" :description="hasError ? '请点击刷新服务重试。' : '填写上方表单，安排下一次到店保养。'" />
    </div>
    <PaginationControls :page="appointmentPage" :total="appointmentTotal" :disabled="loading || processing" @change="changeAppointmentPage" />
    <div class="history-grid">
      <div><h3>保养记录</h3>
        <article v-for="record in records" :key="record.id" class="history-item"><strong>{{ formatTime(record.serviceStartedAt) }} · {{ record.serviceCompletedAt ? '已完成' : '保养中' }}</strong><p>{{ record.content }}<span v-if="record.mileage !== null"> · {{ record.mileage }} km</span></p><p v-if="record.remark">{{ record.remark }}</p><small v-if="record.serviceCompletedAt">完成时间：{{ formatTime(record.serviceCompletedAt) }}</small></article>
        <DataState v-if="loading && !records.length" loading compact title="正在加载保养记录" />
        <DataState v-else-if="!records.length" compact :title="hasError ? '记录暂时未能加载' : '暂无保养记录'" description="门店开始服务后，保养进度与完成信息会显示在这里。" />
        <PaginationControls :page="recordPage" :total="recordTotal" :disabled="loading || processing" @change="changeRecordPage" />
      </div>
      <div><h3>提交评价</h3>
        <form class="review-form" @submit.prevent="submitReview">
          <label>评价对象<select v-model="reviewKind" :disabled="processing"><option value="store">门店服务</option><option value="product">购买商品</option></select></label>
          <label>已完成订单<select v-model="reviewForm.orderId" required :disabled="loading || processing"><option :value="null">选择尚未评价的订单</option><option v-for="order in reviewableOrders" :key="order.id" :value="order.id">{{ order.orderNo }}</option></select></label>
          <label v-if="reviewKind === 'product'">商品<select v-model="reviewForm.productId" required :disabled="processing"><option :value="null">选择商品</option><option v-for="item in reviewableProducts" :key="item.productId" :value="item.productId">{{ item.productName }}</option></select></label>
          <label>评分<select v-model.number="reviewForm.rating" :disabled="processing"><option v-for="rating in [5, 4, 3, 2, 1]" :key="rating" :value="rating">{{ rating }} 分</option></select></label>
          <label>评价内容（可选）<textarea v-model.trim="reviewForm.content" placeholder="说说这次使用配件或到店服务的体验" maxlength="500" :disabled="processing"></textarea></label>
          <button class="primary-button" type="submit" :disabled="loading || processing || !reviewForm.orderId || (reviewKind === 'product' && !reviewForm.productId)">提交评价</button>
        </form>
        <p v-if="!loading && !hasError && !reviewableOrders.length" class="review-summary">订单完成保养后可以评价，每个订单的门店和商品分别评价一次。</p>
        <h3>我的评价</h3>
        <article v-for="review in reviews" :key="'store-' + review.id" class="history-item"><strong>门店服务 · {{ review.rating }} 分</strong><p>{{ review.content || '未填写文字评价' }}</p></article>
        <article v-for="review in productReviews" :key="'product-' + review.id" class="history-item"><strong>{{ review.productName }} · {{ review.rating }} 分</strong><p>{{ review.content || '未填写文字评价' }}</p></article>
      </div>
    </div>
    <ConfirmDialog v-model:open="confirmingCancel" title="取消这次预约？" :description="`${cancellingAppointment?.vehiclePlate ?? ''} · ${cancellingAppointment ? formatTime(cancellingAppointment.appointmentTime) : ''}。取消后如需到店，请重新提交预约。`" confirm-label="确认取消预约" @confirm="confirmCancel" />
  </section>
</template>
