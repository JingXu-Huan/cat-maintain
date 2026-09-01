<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { confirmAppointment, listStoreAppointments, rejectAppointment } from '../api/appointments'
import { completeMaintenance, listStoreMaintenanceRecords, startMaintenance } from '../api/maintenance'
import type { Appointment } from '../types/appointment'
import type { MaintenanceRecord } from '../types/maintenance'

const appointments = ref<Appointment[]>([])
const records = ref<MaintenanceRecord[]>([])
const feedback = ref('')
const isLoading = ref(false)
const actingId = ref<number | null>(null)
const rejectReason = ref('当前时段无法接待')
const startContent = ref<Record<number, string>>({})
const completeContent = ref<Record<number, string>>({})

async function refresh() { isLoading.value = true; try { const [a, r] = await Promise.all([listStoreAppointments(), listStoreMaintenanceRecords()]); appointments.value = a.content; records.value = r.content } catch (error) { feedback.value = error instanceof Error ? error.message : '门店服务数据加载失败。' } finally { isLoading.value = false } }
async function act(id: number, action: () => Promise<unknown>, success: string) { actingId.value = id; feedback.value = ''; try { await action(); feedback.value = success; await refresh() } catch (error) { feedback.value = error instanceof Error ? error.message : '操作失败。' } finally { actingId.value = null } }
onMounted(refresh)
</script>

<template>
  <section class="service-panel">
    <div class="section-heading"><div><p class="eyebrow">STORE SERVICE</p><h2>预约与保养工作台</h2></div><button class="secondary-button" type="button" :disabled="isLoading" @click="refresh">刷新工作台</button></div>
    <p v-if="feedback" class="feedback" :class="feedback.includes('失败') ? 'error' : 'success'" role="status">{{ feedback }}</p>
    <label class="reject-reason"><span>拒绝预约原因</span><input v-model.trim="rejectReason" maxlength="500" /></label>
    <div class="order-list"><article v-for="appointment in appointments" :key="appointment.id" class="order-item"><div><strong>预约 #{{ appointment.id }} · {{ appointment.vehiclePlate }}</strong><p>{{ appointment.status }} · {{ appointment.appointmentTime }}<span v-if="appointment.orderId"> · 订单 #{{ appointment.orderId }}</span></p></div><div class="application-actions"><button v-if="appointment.status === 'PENDING'" class="approve-button" type="button" :disabled="actingId !== null" @click="act(appointment.id, () => confirmAppointment(appointment.id), '预约已确认。')">确认</button><button v-if="appointment.status === 'PENDING'" class="reject-button" type="button" :disabled="actingId !== null" @click="act(appointment.id, () => rejectAppointment(appointment.id, rejectReason), '预约已拒绝。')">拒绝</button><template v-if="appointment.status === 'CONFIRMED'"><input v-model.trim="startContent[appointment.id]" class="inline-input" placeholder="服务开始说明" /><button class="approve-button" type="button" :disabled="actingId !== null" @click="act(appointment.id, () => startMaintenance(appointment.id, startContent[appointment.id] || '已开始保养'), '保养已开始。')">开始保养</button></template></div></article><p v-if="!isLoading && appointments.length === 0" class="admin-empty">暂无预约。</p></div>
    <h3>保养记录</h3><div class="order-list"><article v-for="record in records" :key="record.id" class="order-item"><div><strong>记录 #{{ record.id }} · 预约 #{{ record.appointmentId }}</strong><p>{{ record.content }}<span v-if="record.serviceCompletedAt"> · 已完成于 {{ record.serviceCompletedAt }}</span></p></div><div v-if="!record.serviceCompletedAt" class="application-actions"><input v-model.trim="completeContent[record.id]" class="inline-input" placeholder="完成说明" /><button class="approve-button" type="button" :disabled="actingId !== null" @click="act(record.id, () => completeMaintenance(record.id, { content: completeContent[record.id] || '保养已完成' }), '保养已完成。')">完成保养</button></div></article><p v-if="!isLoading && records.length === 0" class="admin-empty">暂无保养记录。</p></div>
  </section>
</template>
