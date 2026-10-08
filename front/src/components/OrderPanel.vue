<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { createOrder, listMyOrders } from '../api/orders'
import { listActiveStores } from '../api/stores'
import { useCart } from '../composables/useCart'
import type { Order } from '../types/order'
import type { StoreSummary } from '../types/store'
import { formatTime, money, statusLabel, toCents } from '../utils/format'
import PaginationControls from './PaginationControls.vue'
import ConfirmDialog from './ConfirmDialog.vue'
import DataState from './DataState.vue'

const emit = defineEmits<{ changed: []; book: [order: Order]; browse: [] }>()
const { entries, checkingOut, productAmount, laborAmount, itemCount, remove, setQuantity, clear, clearForAccount, getAccountId, refreshPrices } = useCart()
const stores = ref<StoreSummary[]>([])
const orders = ref<Order[]>([])
const selectedStoreId = ref<number | null>(null)
const page = ref(0)
const total = ref(0)
const isLoading = ref(false)
const feedback = ref('')
const hasError = ref(false)
const confirmingClear = ref(false)
async function refresh(nextPage = page.value) {
  isLoading.value = true
  if (hasError.value) { feedback.value = ''; hasError.value = false }
  try {
    const [availableStores, response] = await Promise.all([listActiveStores(), listMyOrders(nextPage)])
    stores.value = availableStores; orders.value = response.content; page.value = nextPage; total.value = response.total
    if (!availableStores.some((store) => store.id === selectedStoreId.value)) selectedStoreId.value = availableStores[0]?.id ?? null
  } catch (error) { hasError.value = true; feedback.value = error instanceof Error ? error.message : '订单数据加载失败。' }
  finally { isLoading.value = false }
}
function changeQuantity(productId: number, event: Event) {
  const input = event.target as HTMLInputElement
  const error = setQuantity(productId, Number(input.value))
  hasError.value = error !== null
  feedback.value = error ?? ''
  input.value = String(entries.value.find((item) => item.product.id === productId)?.quantity ?? 1)
}
async function submitOrder() {
  if (checkingOut.value) return
  const ownerId = getAccountId()
  if (ownerId === null) return
  if (!selectedStoreId.value || !entries.value.length) { hasError.value = true; feedback.value = '请选择门店并加入商品。'; return }
  isLoading.value = true
  checkingOut.value = true
  feedback.value = ''
  try {
    const previousAmount = productAmount.value + laborAmount.value
    await refreshPrices()
    if (getAccountId() !== ownerId) throw new Error('登录账号已变化，请重新查看购物车。')
    if (entries.value.some((item) => !Number.isInteger(item.quantity) || item.quantity < 1 || item.quantity > Math.min(item.product.stock, 99))) throw new Error('商品数量超过库存或不合法，请调整购物车。')
    if (previousAmount !== productAmount.value + laborAmount.value) throw new Error('商品价格或工时费已更新，请查看新金额后再次提交。')
    const order = await createOrder(selectedStoreId.value, entries.value.map((item) => ({ productId: item.product.id, quantity: item.quantity })))
    clearForAccount(ownerId); hasError.value = false; feedback.value = `订单 ${order.orderNo} 已提交，等待平台审核。`
    await refresh(0)
    emit('changed')
  } catch (error) { hasError.value = true; feedback.value = error instanceof Error ? error.message : '下单失败。' }
  finally { isLoading.value = false; checkingOut.value = false }
}
onMounted(() => refresh())
defineExpose({ refresh })
</script>

<template>
  <section id="orders" class="order-panel">
    <div class="section-heading"><div><h2>购物车</h2><p>确认配件数量后，选择接收配件和提供保养的门店。</p></div><button class="secondary-button" type="button" :disabled="isLoading" @click="refresh()">{{ isLoading ? '加载中…' : '刷新订单' }}</button></div>
    <p v-if="feedback" class="feedback" :class="hasError ? 'error' : 'success'" :role="hasError ? 'alert' : 'status'">{{ feedback }}</p>
    <DataState v-if="!entries.length" title="购物车还是空的" description="把需要的配件加入购物车，再一起提交订单。"><button class="primary-button" type="button" @click="emit('browse')">去选配件</button></DataState>
    <div v-else class="cart-list">
      <article v-for="item in entries" :key="item.product.id" class="cart-item">
        <span><strong>{{ item.product.productName }}</strong><small>¥{{ item.product.price }} + 工时费 ¥{{ item.product.laborFee }} · 库存 {{ item.product.stock }}</small><small v-if="item.unavailable">商品暂不可购买，请移除或稍后重试。</small></span>
        <div class="application-actions">
          <input :value="item.quantity" :aria-label="`${item.product.productName} 数量`" type="number" min="1" :max="Math.max(1, Math.min(item.product.stock, 99))" step="1" :disabled="isLoading || checkingOut" @change="changeQuantity(item.product.id, $event)" />
          <span>¥{{ money((toCents(item.product.price) + toCents(item.product.laborFee)) * item.quantity) }}</span>
          <button class="reject-button" type="button" :disabled="isLoading || checkingOut" @click="remove(item.product.id)">移除</button>
        </div>
      </article>
    </div>
    <div v-if="entries.length" class="cart-summary"><span>共 {{ itemCount }} 件 · 配件 ¥{{ money(productAmount) }} · 工时费 ¥{{ money(laborAmount) }}</span><strong>合计 ¥{{ money(productAmount + laborAmount) }}</strong><button class="secondary-button compact-button" type="button" :disabled="isLoading || checkingOut" @click="confirmingClear = true">清空购物车</button></div>
    <div v-if="entries.length" class="order-form-row">
      <label><span>配送和保养门店</span><select v-model="selectedStoreId" :disabled="isLoading || checkingOut"><option :value="null">选择门店</option><option v-for="store in stores" :key="store.id" :value="store.id">{{ store.storeName }} · {{ store.address }}</option></select></label>
      <button class="primary-button" type="button" :disabled="isLoading || checkingOut || !selectedStoreId || !entries.length" @click="submitOrder">{{ checkingOut ? '提交中…' : '提交订单' }}</button>
    </div>
    <h3>我的订单</h3>
    <DataState v-if="isLoading && !orders.length" loading compact title="正在加载订单" />
    <div class="order-list">
      <article v-for="order in orders" :key="order.id" class="order-item">
        <div><strong>{{ order.orderNo }}</strong><p><span class="status-badge">{{ statusLabel(order.status) }}</span> · ¥{{ order.totalAmount }} · {{ formatTime(order.createdAt) }}</p><p>{{ order.items.map((item) => `${item.productName} × ${item.quantity}`).join('，') }}</p><p v-if="order.rejectedReason">拒绝原因：{{ order.rejectedReason }}</p></div>
        <div class="application-actions"><span v-if="order.verificationCode" class="verification-badge">核销码 {{ order.verificationCode }}</span><button v-if="['APPROVED', 'DELIVERED', 'VERIFIED'].includes(order.status)" class="secondary-button compact-button" type="button" @click="emit('book', order)">预约保养</button></div>
      </article>
      <DataState v-if="!isLoading && !orders.length" compact :title="hasError ? '订单暂时未能加载' : '还没有订单'" :description="hasError ? '请点击刷新订单重试。' : '提交购物车后，你可以在这里跟进订单进度。'" />
    </div>
    <PaginationControls :page="page" :total="total" :disabled="isLoading" @change="refresh" />
    <ConfirmDialog v-model:open="confirmingClear" title="清空购物车？" description="购物车中的配件将全部移除。已经提交的订单会保留。" confirm-label="确认清空" @confirm="clear" />
  </section>
</template>
