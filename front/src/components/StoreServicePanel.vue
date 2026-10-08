<script setup lang="ts">
import { toastSuccess } from '../composables/useToast'
import { onMounted, ref, watch } from 'vue'
import { confirmAppointment, listStoreAppointments, rejectAppointment } from '../api/appointments'
import { completeMaintenance, listStoreMaintenanceRecords, startMaintenance } from '../api/maintenance'
import type { Appointment } from '../types/appointment'
import type { MaintenanceRecord } from '../types/maintenance'
import { formatTime, statusLabel } from '../utils/format'
import PaginationControls from './PaginationControls.vue'
import DataState from './DataState.vue'

const props = defineProps<{ revision: number }>()
const appointments = ref<Appointment[]>([])
const records = ref<MaintenanceRecord[]>([])
const appointmentPage = ref(0)
const appointmentTotal = ref(0)
const recordPage = ref(0)
const recordTotal = ref(0)
const feedback = ref('')
const hasError = ref(false)
const isLoading = ref(false)
const actingId = ref<number | null>(null)
const rejectReason = ref('当前时段无法接待')
const startContent = ref<Record<number, string>>({})
const completeContent = ref<Record<number, string>>({})
const mileage = ref<Record<number, number | string | undefined>>({})
const remark = ref<Record<number, string>>({})
async function refresh() {
  isLoading.value = true
  if (hasError.value) { feedback.value = ''; hasError.value = false }
  try {
    const [a, r] = await Promise.all([listStoreAppointments(appointmentPage.value), listStoreMaintenanceRecords(recordPage.value)])
    appointments.value = a.content; appointmentTotal.value = a.total
    records.value = r.content; recordTotal.value = r.total
  } catch (error) { hasError.value = true; feedback.value = error instanceof Error ? error.message : '门店服务数据加载失败。' }
  finally { isLoading.value = false }
}
async function act(id: number, action: () => Promise<unknown>, success: string) {
  actingId.value = id; feedback.value = ''
  try { await action(); hasError.value = false; toastSuccess(success); await refresh() }
  catch (error) { hasError.value = true; feedback.value = error instanceof Error ? error.message : '操作失败。' }
  finally { actingId.value = null }
}
function finish(record: MaintenanceRecord) {
  const raw = mileage.value[record.id]
  const distance = raw === '' || raw === undefined ? undefined : Number(raw)
  if (distance !== undefined && (!Number.isInteger(distance) || distance < 0)) { hasError.value = true; feedback.value = '里程必须为非负整数。'; return }
  void act(record.id, () => completeMaintenance(record.id, { content: completeContent.value[record.id]?.trim() || '保养已完成', mileage: distance, remark: remark.value[record.id] }), '保养已完成，订单和预约已同步更新。')
}
function changeAppointmentPage(page: number) { appointmentPage.value = page; void refresh() }
function changeRecordPage(page: number) { recordPage.value = page; void refresh() }
watch(() => props.revision, () => { void refresh() })
onMounted(refresh)
</script>

<template>
  <section class="service-panel">
    <div class="section-heading"><div><h2>门店预约</h2></div><button class="secondary-button" type="button" :disabled="isLoading || actingId !== null" @click="refresh">{{ isLoading ? '加载中…' : '刷新工作台' }}</button></div>
    <p v-if="feedback && hasError" class="feedback error" role="alert">{{ feedback }}</p>
    <label class="reject-reason"><span>拒绝预约时的说明</span><input v-model.trim="rejectReason" maxlength="500" :disabled="isLoading || actingId !== null" /></label>
    <DataState v-if="isLoading && !appointments.length" loading title="正在加载预约" />
    <div class="order-list">
      <article v-for="appointment in appointments" :key="appointment.id" class="order-item">
        <div><strong>{{ appointment.vehiclePlate }} · {{ statusLabel(appointment.status) }}</strong><p>{{ formatTime(appointment.appointmentTime) }} · {{ appointment.vehicleModel || '未填写车型' }}<span v-if="appointment.orderId"> · 订单 #{{ appointment.orderId }}</span></p><p v-if="appointment.checkedInAt">已到店登记：{{ formatTime(appointment.checkedInAt) }}</p></div>
        <div class="application-actions">
          <button v-if="appointment.status === 'PENDING'" class="approve-button" type="button" :disabled="actingId !== null || isLoading" @click="act(appointment.id, () => confirmAppointment(appointment.id), '预约已确认。')">确认</button>
          <button v-if="appointment.status === 'PENDING'" class="reject-button" type="button" :disabled="actingId !== null || isLoading || !rejectReason" @click="act(appointment.id, () => rejectAppointment(appointment.id, rejectReason), '预约已拒绝。')">拒绝预约</button>
          <template v-if="appointment.status === 'CONFIRMED'"><label class="inline-field">服务开始说明（可选）<input v-model.trim="startContent[appointment.id]" placeholder="例如：更换机油与滤芯" maxlength="2000" :disabled="actingId !== null || isLoading" /></label><button class="approve-button" type="button" :disabled="actingId !== null || isLoading" @click="act(appointment.id, () => startMaintenance(appointment.id, startContent[appointment.id] || '已开始保养'), '保养已开始。')">开始保养</button></template>
        </div>
      </article><DataState v-if="!isLoading && !appointments.length" :title="hasError ? '预约暂时未能加载' : '暂无门店预约'" :description="hasError ? '请点击刷新工作台重试。' : '用户提交预约后，你可以确认服务时间并安排接待。'" />
    </div>
    <PaginationControls :page="appointmentPage" :total="appointmentTotal" :disabled="isLoading || actingId !== null" @change="changeAppointmentPage" />
    <h3>保养记录</h3>
    <DataState v-if="isLoading && !records.length" loading title="正在加载保养记录" />
    <div class="order-list">
      <article v-for="record in records" :key="record.id" class="order-item">
        <div><strong>记录 #{{ record.id }} · 预约 #{{ record.appointmentId }}</strong><p>{{ record.content }}</p><p v-if="record.mileage !== null">里程：{{ record.mileage }} km</p><p v-if="record.remark">{{ record.remark }}</p><p v-if="record.serviceCompletedAt">已完成于 {{ formatTime(record.serviceCompletedAt) }}</p></div>
        <form v-if="!record.serviceCompletedAt" class="maintenance-complete-form" @submit.prevent="finish(record)">
          <label>完成说明<input v-model.trim="completeContent[record.id]" placeholder="填写实际完成的保养项目" maxlength="2000" required :disabled="isLoading || actingId !== null" /></label>
          <label>里程（km，可选）<input v-model.number="mileage[record.id]" type="number" min="0" step="1" :disabled="isLoading || actingId !== null" /></label>
          <label>保养备注（可选）<input v-model.trim="remark[record.id]" placeholder="后续保养建议等" maxlength="500" :disabled="isLoading || actingId !== null" /></label>
          <button class="approve-button" type="submit" :disabled="actingId !== null || isLoading">完成保养</button>
        </form>
      </article><DataState v-if="!isLoading && !records.length" :title="hasError ? '记录暂时未能加载' : '暂无保养记录'" :description="hasError ? '请点击刷新工作台重试。' : '开始保养后，在这里补充完成说明、车辆里程和备注。'" />
    </div>
    <PaginationControls :page="recordPage" :total="recordTotal" :disabled="isLoading || actingId !== null" @change="changeRecordPage" />
  </section>
</template>
