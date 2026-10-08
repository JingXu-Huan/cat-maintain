<script setup lang="ts">
import { computed } from 'vue'
import { BadgeCheck, Ban, ChevronDown, CircleCheck, CircleX, Clock3, MapPin, PackageCheck, Truck } from 'lucide'
import type { Order, OrderStatus } from '../types/order'
import type { StoreSummary } from '../types/store'
import { formatTime, money, statusLabel, toCents } from '../utils/format'
import UiIcon from './UiIcon.vue'

const props = withDefaults(
  defineProps<{
    order: Order
    store?: StoreSummary | null
    audience?: 'USER' | 'STORE' | 'ADMIN'
    detailsOpen?: boolean
    headingLevel?: 3 | 4
  }>(),
  { audience: 'USER', detailsOpen: false, headingLevel: 3 },
)

const states = {
  PENDING_APPROVAL: { tone: 'pending', icon: Clock3, hint: '订单已提交，等待平台审核。' },
  APPROVED: { tone: 'approved', icon: Truck, hint: '审核已通过，等待配件配送至服务门店。' },
  DELIVERED: { tone: 'delivered', icon: PackageCheck, hint: '配件已到店，可安排到店保养并核销订单。' },
  VERIFIED: { tone: 'verified', icon: BadgeCheck, hint: '门店已核销订单，保养进度可在预约与保养中查看。' },
  COMPLETED: { tone: 'completed', icon: CircleCheck, hint: '本次保养已完成，感谢你的使用。' },
  REJECTED: { tone: 'rejected', icon: CircleX, hint: '订单未通过审核，请查看下方拒绝原因。' },
  CANCELLED: { tone: 'cancelled', icon: Ban, hint: '订单已取消，无需继续配送或核销。' },
} satisfies Record<OrderStatus, { tone: string; icon: typeof Clock3; hint: string }>

const headingTag = computed(() => `h${props.headingLevel}`)
const detailHeadingTag = computed(() => `h${props.headingLevel + 1}`)
const state = computed(() => states[props.order.status])
const hint = computed(() => {
  if (props.audience === 'ADMIN') {
    if (props.order.status === 'PENDING_APPROVAL') return '核对商品和服务门店后，审核这笔订单。'
    if (props.order.status === 'APPROVED') return '配件送达门店后，确认配送并生成核销码。'
  }
  if (props.audience === 'STORE') {
    if (props.order.status === 'DELIVERED') return '用户到店后，核对预约和核销码，再确认核销。'
    if (props.order.status === 'VERIFIED') return '订单已核销，可在预约与保养工作台继续处理服务。'
    if (props.order.status === 'COMPLETED') return '本次保养已完成，订单与服务记录已留存。'
  }
  return state.value.hint
})
const quantity = computed(() => props.order.items.reduce((sum, item) => sum + item.quantity, 0))
const productSummary = computed(() => {
  const items = props.order.items
    .slice(0, 2)
    .map((item) => `${item.productName} × ${item.quantity}`)
    .join('，')
  return props.order.items.length > 2 ? `${items} 等 ${props.order.items.length} 款配件` : items
})
const codeState = computed(() => {
  if (['VERIFIED', 'COMPLETED'].includes(props.order.status)) return '已使用'
  if (['CANCELLED', 'REJECTED'].includes(props.order.status)) return '已失效'
  return '待核销'
})
const events = computed(() =>
  [
    { label: '提交订单', time: props.order.createdAt },
    { label: '平台审核通过', time: props.order.approvedAt },
    { label: '配件配送到店', time: props.order.deliveredAt },
    { label: '门店核销', time: props.order.verifiedAt },
    { label: '保养完成', time: props.order.completedAt },
  ].filter((event) => event.time),
)
function amount(value: string) {
  return money(toCents(value))
}
</script>

<template>
  <article
    class="order-card"
    :class="`order-state-${state.tone}`"
    :aria-label="`订单 ${order.orderNo}，${statusLabel(order.status)}`"
  >
    <header class="order-card-header">
      <span class="order-status"><UiIcon :icon="state.icon" :size="16" />{{ statusLabel(order.status) }}</span>
      <span class="order-created"
        >下单时间 <time :datetime="order.createdAt">{{ formatTime(order.createdAt) }}</time></span
      >
    </header>
    <div class="order-card-overview">
      <div class="order-card-identity">
        <component :is="headingTag" class="order-card-number">
          订单 <span>{{ order.orderNo }}</span>
        </component>
        <p class="order-product-summary">
          {{ productSummary }}<span>共 {{ quantity }} 件</span>
        </p>
        <p class="order-store">
          <UiIcon :icon="MapPin" :size="16" /><span>{{ store?.storeName || `服务门店 #${order.storeId}` }}</span>
        </p>
      </div>
      <div class="order-card-amount">
        <span>订单金额</span><strong><span>¥</span>{{ amount(order.totalAmount) }}</strong
        ><small>含配件与工时费</small>
      </div>
    </div>
    <p class="order-status-hint">{{ hint }}</p>
    <p v-if="order.rejectedReason" class="order-rejection"><strong>拒绝原因</strong>{{ order.rejectedReason }}</p>
    <div v-if="order.verificationCode || $slots.actions" class="order-card-footer">
      <div v-if="order.verificationCode" class="order-code" :class="{ 'is-used': codeState !== '待核销' }">
        <span>核销码 · {{ codeState }}</span
        ><strong>{{ order.verificationCode }}</strong>
      </div>
      <div v-if="$slots.actions" class="order-card-actions"><slot name="actions" /></div>
    </div>
    <details class="order-details" :open="detailsOpen">
      <summary>
        <span>订单详情</span><span class="order-detail-caption">费用明细 · 门店信息 · 处理记录</span
        ><UiIcon :icon="ChevronDown" :size="18" />
      </summary>
      <div class="order-details-content">
        <section class="order-line-items" aria-label="商品及费用明细">
          <component :is="detailHeadingTag" class="order-detail-heading">商品及费用明细</component>
          <ul>
            <li v-for="item in order.items" :key="item.productId">
              <div>
                <strong>{{ item.productName }}</strong>
                <p>配件 ¥{{ amount(item.unitPrice) }} / 件 · 工时费 ¥{{ amount(item.unitLaborFee) }} / 件</p>
              </div>
              <span class="order-item-quantity">× {{ item.quantity }}</span>
              <strong class="order-item-total">¥{{ amount(item.lineTotal) }}</strong>
            </li>
          </ul>
          <dl class="order-costs">
            <div>
              <dt>配件金额</dt>
              <dd>¥{{ amount(order.productAmount) }}</dd>
            </div>
            <div>
              <dt>工时费</dt>
              <dd>¥{{ amount(order.laborFeeAmount) }}</dd>
            </div>
            <div class="order-cost-total">
              <dt>订单合计</dt>
              <dd>¥{{ amount(order.totalAmount) }}</dd>
            </div>
          </dl>
        </section>
        <div class="order-detail-grid">
          <section class="order-store-details" aria-label="配送与保养门店">
            <component :is="detailHeadingTag" class="order-detail-heading">配送与保养门店</component>
            <dl>
              <div>
                <dt>门店</dt>
                <dd>{{ store?.storeName || `服务门店 #${order.storeId}` }}</dd>
              </div>
              <template v-if="store">
                <div>
                  <dt>地址</dt>
                  <dd>{{ store.address }}</dd>
                </div>
                <div>
                  <dt>联系人</dt>
                  <dd>{{ store.contactName }}</dd>
                </div>
                <div>
                  <dt>联系电话</dt>
                  <dd>{{ store.phone }}</dd>
                </div>
              </template>
              <div v-else>
                <dt>门店资料</dt>
                <dd>暂未获取，可刷新订单重试。</dd>
              </div>
              <div v-if="audience === 'ADMIN'">
                <dt>车主账号</dt>
                <dd>#{{ order.accountId }}</dd>
              </div>
            </dl>
          </section>
          <section class="order-processing" aria-label="订单处理记录">
            <component :is="detailHeadingTag" class="order-detail-heading">处理记录</component>
            <ol>
              <li v-for="event in events" :key="event.label">
                <span>{{ event.label }}</span
                ><time :datetime="event.time!">{{ formatTime(event.time) }}</time>
              </li>
            </ol>
            <p v-if="order.verifiedByStoreId" class="order-verified-by">核销门店 #{{ order.verifiedByStoreId }}</p>
          </section>
        </div>
      </div>
    </details>
  </article>
</template>

<style scoped>
.order-card {
  --order-tone: #536174;
  --order-tint: #edf0f5;
  min-width: 0;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: var(--surface);
  overflow: hidden;
}
.order-state-pending {
  --order-tone: #8a5705;
  --order-tint: #fff5df;
}
.order-state-approved {
  --order-tone: #2356c6;
  --order-tint: #eaf0ff;
}
.order-state-delivered {
  --order-tone: #6540a3;
  --order-tint: #f1ebfb;
}
.order-state-verified {
  --order-tone: #14665f;
  --order-tint: #e2f5f1;
}
.order-state-completed {
  --order-tone: #18734f;
  --order-tint: #e6f4ec;
}
.order-state-rejected {
  --order-tone: #ae343b;
  --order-tint: #fcecef;
}
.order-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  padding: 20px 24px 0;
}
.order-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 6px;
  padding: 5px 10px;
  background: var(--order-tint);
  color: var(--order-tone);
  font-size: 12px;
  font-weight: 650;
}
.order-created {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--muted);
  font-size: 12px;
}
.order-card-overview {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 16px 24px 0;
}
.order-card-identity {
  flex: 1;
  min-width: 0;
}
.order-card-number {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  overflow-wrap: anywhere;
  line-height: 1.7;
}
.order-card-number span {
  font-weight: 500;
}
.order-product-summary {
  margin: 10px 0;
  font-size: 14px;
}
.order-product-summary > span {
  display: inline-block;
  margin-left: 12px;
  color: var(--muted);
  font-size: 12px;
  white-space: nowrap;
}
.order-store {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin: 0;
  color: var(--muted);
  font-size: 12px;
}
.order-store .ui-icon {
  margin-top: 2px;
}
.order-card-amount {
  flex-shrink: 0;
  display: grid;
  gap: 2px;
  text-align: right;
}
.order-card-amount > span,
.order-card-amount small {
  color: var(--muted);
  font-size: 12px;
}
.order-card-amount > strong {
  font-size: 26px;
  line-height: 1.3;
  letter-spacing: -0.02em;
  font-weight: 650;
}
.order-card-amount strong > span {
  margin-right: 3px;
  font-size: 16px;
  font-weight: 500;
}
.order-card-amount strong,
.order-item-total,
.order-costs dd,
.order-created time,
.order-processing time {
  font-variant-numeric: tabular-nums;
}
.order-status-hint {
  margin: 14px 24px 20px;
  color: var(--muted);
  font-size: 12px;
}
.order-rejection {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 0 24px 20px;
  padding: 12px 16px;
  border-radius: 8px;
  background: #fcecef;
  color: #9c303a;
  font-size: 13px;
}
.order-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
  margin: 0 24px;
  padding: 16px 0;
  border-top: 1px solid var(--line);
}
.order-code {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  color: #14665f;
  font-size: 12px;
}
.order-code strong {
  padding: 6px 12px;
  border-radius: 6px;
  background: #e2f5f1;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 0.1em;
  font-variant-numeric: tabular-nums;
}
.order-code.is-used {
  color: var(--muted);
}
.order-code.is-used strong {
  background: #edf0f5;
  font-size: 16px;
  letter-spacing: 0.05em;
}
.order-card-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  margin-left: auto;
}
.order-details {
  border-top: 1px solid var(--line);
}
.order-details summary {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 48px;
  padding: 12px 24px;
  color: var(--blue-ink);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  list-style: none;
}
.order-details summary::-webkit-details-marker {
  display: none;
}
.order-details summary:hover {
  background: #f5f7fb;
}
.order-details summary:focus-visible {
  outline-offset: -4px;
}
.order-detail-caption {
  color: var(--muted);
  font-size: 12px;
  font-weight: 400;
}
.order-details summary .ui-icon {
  margin-left: auto;
  transition: transform 160ms ease;
}
.order-details[open] summary .ui-icon {
  transform: rotate(180deg);
}
.order-details-content {
  padding: 8px 24px 24px;
}
.order-detail-heading {
  margin: 12px 0;
  font-size: 13px;
  font-weight: 650;
}
.order-line-items ul,
.order-processing ol {
  margin: 0;
  padding: 0;
  list-style: none;
}
.order-line-items li {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(88px, auto);
  align-items: center;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid var(--line);
  font-size: 13px;
}
.order-line-items li strong {
  font-weight: 600;
  overflow-wrap: anywhere;
}
.order-line-items li p {
  margin: 4px 0 0;
  color: var(--muted);
  font-size: 12px;
}
.order-item-quantity {
  color: var(--muted);
  white-space: nowrap;
}
.order-item-total {
  text-align: right;
}
.order-costs {
  display: grid;
  gap: 8px;
  max-width: 300px;
  margin: 16px 0 0 auto;
  font-size: 13px;
}
.order-costs > div {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
}
.order-costs dt {
  color: var(--muted);
}
.order-costs dd {
  margin: 0;
}
.order-cost-total {
  padding-top: 8px;
  border-top: 1px solid var(--line);
  font-weight: 650;
}
.order-cost-total dt {
  color: var(--ink);
}
.order-detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 32px;
  margin-top: 24px;
  padding-top: 12px;
  border-top: 1px solid var(--line);
}
.order-store-details dl {
  display: grid;
  gap: 10px;
  margin: 0;
  font-size: 12px;
}
.order-store-details dl > div {
  display: grid;
  grid-template-columns: 60px minmax(0, 1fr);
  gap: 12px;
}
.order-store-details dt {
  color: var(--muted);
}
.order-store-details dd {
  margin: 0;
  overflow-wrap: anywhere;
}
.order-processing li {
  position: relative;
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 4px 16px;
  margin-left: 3px;
  padding: 0 0 16px 16px;
  border-left: 1px solid var(--line);
  font-size: 12px;
}
.order-processing li:last-child {
  padding-bottom: 0;
  border-left-color: transparent;
}
.order-processing li::before {
  content: '';
  position: absolute;
  top: 6px;
  left: -4px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--green);
}
.order-processing time,
.order-verified-by {
  color: var(--muted);
  font-size: 12px;
}
.order-verified-by {
  margin: 12px 0 0;
}
@media (max-width: 640px) {
  .order-card-header {
    padding: 16px 16px 0;
  }
  .order-card-overview {
    flex-direction: column;
    gap: 16px;
    padding: 16px 16px 0;
  }
  .order-card-amount {
    display: flex;
    align-items: baseline;
    flex-wrap: wrap;
    gap: 8px;
    text-align: left;
  }
  .order-card-amount > strong {
    font-size: 24px;
  }
  .order-status-hint {
    margin: 12px 16px 16px;
  }
  .order-rejection {
    margin: 0 16px 16px;
  }
  .order-card-footer {
    margin: 0 16px;
  }
  .order-card-actions {
    width: 100%;
    margin: 0;
    justify-content: flex-start;
  }
  .order-details summary {
    padding: 12px 16px;
  }
  .order-detail-caption {
    display: none;
  }
  .order-details-content {
    padding: 4px 16px 16px;
  }
  .order-line-items li {
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 8px 12px;
  }
  .order-line-items li > div {
    grid-column: 1 / -1;
  }
  .order-detail-grid {
    grid-template-columns: minmax(0, 1fr);
    gap: 24px;
  }
}
</style>
