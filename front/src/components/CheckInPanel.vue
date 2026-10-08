<script setup lang="ts">
import { toastSuccess } from '../composables/useToast'
import { computed, ref, watch } from 'vue'
import { listMyAppointments } from '../api/appointments'
import { checkIn } from '../api/checkIn'
import { listActiveStores } from '../api/stores'
import type { AccountResponse } from '../types/auth'
import type { Appointment } from '../types/appointment'
import type { StoreSummary } from '../types/store'
import { formatTime } from '../utils/format'
import { loadAllPages } from '../utils/paging'

const props = defineProps<{ storeId: number; account: AccountResponse | null }>()
const emit = defineEmits<{ changed: []; signIn: [] }>()
const store = ref<StoreSummary | null>(null)
const appointments = ref<Appointment[]>([])
const appointmentId = ref<number | null>(null)
const code = ref('')
const busy = ref(false)
const feedback = ref('')
const hasError = ref(false)
const available = computed(() => appointments.value.filter((appointment) => appointment.storeId === props.storeId
  && appointment.orderId !== null && (appointment.status === 'CONFIRMED' || (appointment.checkedInAt !== null && ['IN_PROGRESS', 'COMPLETED'].includes(appointment.status)))))
async function load() {
  busy.value = true; feedback.value = ''
  hasError.value = false
  try {
    store.value = (await listActiveStores()).find((item) => item.id === props.storeId) ?? null
    if (!store.value) throw new Error('门店不存在或尚未启用，请确认二维码。')
    appointments.value = props.account?.role === 'USER' ? await loadAllPages(listMyAppointments) : []
    appointmentId.value = available.value[0]?.id ?? null
  } catch (error) { hasError.value = true; feedback.value = error instanceof Error ? error.message : '登记数据加载失败。' }
  finally { busy.value = false }
}
async function submit() {
  if (!appointmentId.value || !/^[0-9]{8}$/.test(code.value)) return
  busy.value = true; feedback.value = ''
  try {
    const response = await checkIn(props.storeId, appointmentId.value, code.value)
    const index = appointments.value.findIndex((appointment) => appointment.id === response.appointment.id)
    if (index >= 0) appointments.value[index] = response.appointment
    hasError.value = false; toastSuccess('到店登记成功，门店可以开始保养。'); emit('changed')
  } catch (error) { hasError.value = true; feedback.value = error instanceof Error ? error.message : '到店登记失败。' }
  finally { busy.value = false }
}
watch(() => [props.storeId, props.account?.id], () => { code.value = ''; appointments.value = []; void load() }, { immediate: true })
</script>

<template>
  <section id="check-in" class="service-panel">
    <div class="section-heading"><div><h2>到店登记{{ store ? ' · ' + store.storeName : '' }}</h2></div><button class="secondary-button" type="button" :disabled="busy" @click="load">{{ busy ? '加载中…' : '刷新预约' }}</button></div>
    <p v-if="store">{{ store.address }}</p>
    <p v-if="feedback && hasError" class="feedback error" role="alert">{{ feedback }}</p>
    <div v-if="!props.account" class="sign-in-notice"><p>请使用预约所属账号登录，再选择预约并输入订单中的 8 位核销码。登录后会返回本店登记页面。</p><button type="button" class="primary-button" @click="emit('signIn')">登录并继续登记</button></div>
    <p v-else-if="props.account.role !== 'USER'">请使用购买订单的用户账号登记。</p>
    <form v-else-if="store" class="service-form" @submit.prevent="submit">
      <label>已确认预约<select v-model="appointmentId" required :disabled="busy"><option :value="null">选择预约</option><option v-for="appointment in available" :key="appointment.id" :value="appointment.id">{{ appointment.vehiclePlate }} · {{ formatTime(appointment.appointmentTime) }}{{ appointment.checkedInAt ? ' · 已登记' : '' }}</option></select></label>
      <label>订单核销码<input v-model.trim="code" inputmode="numeric" pattern="[0-9]{8}" maxlength="8" required :disabled="busy" /></label>
      <button class="primary-button" type="submit" :disabled="busy || !appointmentId || !/^[0-9]{8}$/.test(code)">确认到店登记</button>
      <p v-if="!busy && !available.length" class="review-summary">该门店暂无已确认且关联订单的预约，请先预约并等待门店确认。</p>
    </form>
  </section>
</template>
