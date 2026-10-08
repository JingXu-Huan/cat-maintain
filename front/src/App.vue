<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { CalendarDays, CarFront, ClipboardCheck, MapPin, Menu, Package, QrCode, ShoppingCart, Store, Truck, UserRound, Wrench, X } from 'lucide'
import { MorphIcon } from 'morphicons/vue'
import { getCurrentAccount } from './api/auth'
import { getJson } from './api/http'
import AdminProductPanel from './components/AdminProductPanel.vue'
import AdminOrderPanel from './components/AdminOrderPanel.vue'
import AdminStorePanel from './components/AdminStorePanel.vue'
import AuthPanel from './components/AuthPanel.vue'
import CheckInPanel from './components/CheckInPanel.vue'
import DataState from './components/DataState.vue'
import OrderPanel from './components/OrderPanel.vue'
import ProductCatalogPanel from './components/ProductCatalogPanel.vue'
import StoreCatalogPanel from './components/StoreCatalogPanel.vue'
import StoreOrderPanel from './components/StoreOrderPanel.vue'
import StoreQrPanel from './components/StoreQrPanel.vue'
import StoreServicePanel from './components/StoreServicePanel.vue'
import UiIcon from './components/UiIcon.vue'
import UserServicePanel from './components/UserServicePanel.vue'
import { setCartAccount, useCart } from './composables/useCart'
import type { AccountResponse } from './types/auth'
import type { HealthResponse } from './types/health'
import type { Order } from './types/order'

type View = 'catalog' | 'stores' | 'orders' | 'services' | 'store-orders' | 'store-services' | 'store-qr' | 'admin-stores' | 'admin-products' | 'admin-orders' | 'check-in' | 'account'
type ApiStatus = 'checking' | 'up' | 'offline'

const views = {
  catalog: { label: '配件商城', title: '选好配件，再安排保养', description: '配件与工时费分别计价，下单时选择配送和保养门店。', icon: Package },
  stores: { label: '服务门店', title: '找到你的保养门店', description: '查看门店地址、联系方式和车主评价，再安排到店服务。', icon: MapPin },
  orders: { label: '购物车与订单', title: '从选购到到店，一目了然', description: '确认购物车、选择门店，随时查看订单进度和核销码。', icon: ShoppingCart },
  services: { label: '预约与保养', title: '把下一次保养安排好', description: '预约到店时间，查看保养记录，并为已完成的订单留下评价。', icon: CalendarDays },
  'store-orders': { label: '订单核销', title: '核对订单，准备接待', description: '输入用户的 8 位核销码，查询订单和关联预约。', icon: ClipboardCheck },
  'store-services': { label: '预约与保养', title: '今天的服务，从这里开始', description: '处理预约、开始保养，完成后记录服务内容与车辆里程。', icon: Wrench },
  'store-qr': { label: '到店二维码', title: '让用户扫码登记到店', description: '生成本店登记二维码，供用户到店时选择预约并核对订单。', icon: QrCode },
  'admin-stores': { label: '加盟店审核', title: '审核新门店的加盟申请', description: '核对申请资料，通过审核后门店即可登录并提供服务。', icon: Store },
  'admin-products': { label: '商品与库存', title: '维护配件，管理可售库存', description: '编辑商品信息、配件价格和工时费，调整库存与上架状态。', icon: Package },
  'admin-orders': { label: '订单与配送', title: '处理订单，推进配送', description: '审核用户订单，配送后生成供门店核销的凭据。', icon: Truck },
  'check-in': { label: '扫码到店登记', title: '已经到店？完成登记', description: '使用预约所属账号登录，选择已确认预约并输入订单核销码。', icon: QrCode },
  account: { label: '我的账户', title: '管理你的账户', description: '登录后继续购买配件、预约保养，或管理门店与平台业务。', icon: UserRound },
} satisfies Record<View, { label: string; title: string; description: string; icon: typeof Package }>

const initialHash = window.location.hash.slice(1) as View
const checkInStoreId = Number(new URLSearchParams(window.location.search).get('checkInStore'))
const hasCheckIn = Number.isSafeInteger(checkInStoreId) && checkInStoreId > 0
const activeView = ref<View>(hasCheckIn ? 'check-in' : initialHash in views ? initialHash : 'catalog')
const visited = ref(new Set<View>([activeView.value]))
const mobileMenuOpen = ref(false)
const mobileMenuToggle = ref<HTMLButtonElement | null>(null)
const workspace = ref<HTMLElement | null>(null)
const sessionReady = ref(false)
const apiStatus = ref<ApiStatus>('checking')
const currentAccount = ref<AccountResponse | null>(null)
const orderToBook = ref<Order | null>(null)
const serviceRevision = ref(0)
const orderPanel = ref<InstanceType<typeof OrderPanel> | null>(null)
const { itemCount } = useCart()
const roleLabel = computed(() => currentAccount.value ? { USER: '车主服务', STORE: '门店工作台', ADMIN: '平台工作台' }[currentAccount.value.role] : '汽车保养服务')
const navigation = computed<View[]>(() => {
  const role = currentAccount.value?.role
  const items: View[] = role === 'ADMIN' ? ['admin-stores', 'admin-products', 'admin-orders']
    : role === 'STORE' ? ['store-orders', 'store-services', 'store-qr']
      : role === 'USER' ? ['catalog', 'stores', 'orders', 'services'] : ['catalog', 'stores']
  if (hasCheckIn) items.push('check-in')
  items.push('account')
  return items
})
const activePage = computed(() => views[activeView.value])

function navigate(view: View, focus = true, replace = false) {
  if (!navigation.value.includes(view)) return
  activeView.value = view
  visited.value.add(view)
  mobileMenuOpen.value = false
  if (replace || window.location.hash !== `#${view}`) {
    window.history[replace ? 'replaceState' : 'pushState'](null, '', `${window.location.pathname}${window.location.search}#${view}`)
  }
  document.title = `${views[view].label} · 汽车保养平台`
  if (focus) void nextTick(() => {
    workspace.value?.focus({ preventScroll: true })
    window.scrollTo({ top: 0, behavior: 'instant' })
  })
}

function followHash() {
  const hash = window.location.hash.slice(1) as View
  if (hash in views) navigate(hash, true, true)
}

function closeMobileMenu() {
  if (!mobileMenuOpen.value) return
  mobileMenuOpen.value = false
  void nextTick(() => mobileMenuToggle.value?.focus())
}

watch(() => currentAccount.value?.id, () => {
  setCartAccount(currentAccount.value?.role === 'USER' ? currentAccount.value.id : null)
  orderToBook.value = null
  serviceRevision.value++
  if (sessionReady.value) {
    const destination: View = hasCheckIn && (!currentAccount.value || currentAccount.value.role === 'USER') ? 'check-in' : navigation.value[0]!
    visited.value = new Set([destination])
    navigate(destination)
  }
}, { immediate: true, flush: 'sync' })

function checkInChanged() { serviceRevision.value++; void orderPanel.value?.refresh() }
function bookOrder(order: Order) { orderToBook.value = { ...order }; navigate('services') }

async function checkApi() {
  apiStatus.value = 'checking'
  try {
    const health = await getJson<HealthResponse>('/api/health')
    apiStatus.value = health.status === 'UP' ? 'up' : 'offline'
  } catch { apiStatus.value = 'offline' }
}

onMounted(async () => {
  window.addEventListener('hashchange', followHash)
  await Promise.all([
    checkApi(),
    getCurrentAccount().then((account) => { currentAccount.value = account }).catch(() => { currentAccount.value = null }),
  ])
  sessionReady.value = true
  if (!navigation.value.includes(activeView.value)) activeView.value = navigation.value[0]!
  visited.value = new Set([activeView.value])
  navigate(activeView.value, false, true)
})
onBeforeUnmount(() => window.removeEventListener('hashchange', followHash))
</script>

<template>
  <a class="skip-link" href="#workspace">跳到主要内容</a>
  <div class="app-shell">
    <aside class="sidebar" aria-label="应用导航">
      <a class="brand" :href="`#${navigation[0]}`" @click.prevent="navigate(navigation[0]!)">
        <span class="brand-mark"><UiIcon :icon="CarFront" :size="23" /></span>
        <span><strong>汽车保养平台</strong><small>配件购买 · 到店保养</small></span>
      </a>
      <button ref="mobileMenuToggle" class="icon-button mobile-menu-toggle" type="button" :aria-expanded="mobileMenuOpen" aria-controls="workspace-navigation" :aria-label="mobileMenuOpen ? '收起导航' : '展开导航'" @click="mobileMenuOpen = !mobileMenuOpen">
        <MorphIcon :icon="mobileMenuOpen ? X : Menu" :size="22" spring="snappy" reduced-motion="user" />
      </button>
      <div id="workspace-navigation" class="sidebar-content" :class="{ 'is-open': mobileMenuOpen }" @keydown.esc="closeMobileMenu">
        <p class="nav-caption">{{ roleLabel }}</p>
        <nav class="workspace-nav" aria-label="工作台页面">
          <a v-for="view in navigation" :key="view" :href="`#${view}`" class="nav-link" :class="{ active: activeView === view }" :aria-current="activeView === view ? 'page' : undefined" @click.prevent="navigate(view)">
            <UiIcon :icon="views[view].icon" :size="19" />
            <span>{{ view === 'account' && !currentAccount ? '登录 / 注册' : views[view].label }}</span>
            <span v-if="view === 'orders' && itemCount" class="nav-count" :aria-label="`购物车 ${itemCount} 件`">{{ itemCount }}</span>
          </a>
        </nav>
        <div class="sidebar-bottom">
          <a class="sidebar-account" href="#account" @click.prevent="navigate('account')"><span class="account-avatar"><UiIcon :icon="UserRound" :size="18" /></span><span><strong>{{ currentAccount?.username || '尚未登录' }}</strong><small>{{ currentAccount ? '查看账户与登录状态' : '登录后安排保养服务' }}</small></span></a>
          <p>让每一次保养，有据可查。</p>
        </div>
      </div>
    </aside>

    <div class="workspace-shell">
      <header class="topbar">
        <span class="workspace-context">{{ roleLabel }}<span aria-hidden="true"> / </span><strong>{{ activePage.label }}</strong></span>
        <div class="topbar-actions">
          <a v-if="currentAccount?.role === 'USER'" class="cart-shortcut" href="#orders" @click.prevent="navigate('orders')"><UiIcon :icon="ShoppingCart" :size="18" /><span>购物车</span><span class="nav-count">{{ itemCount }}</span></a>
          <button class="status-button" type="button" :disabled="apiStatus === 'checking'" :aria-label="apiStatus === 'checking' ? '正在连接服务' : '重新检查服务连接'" @click="checkApi"><span class="status-dot" :class="apiStatus" aria-hidden="true"></span>{{ apiStatus === 'checking' ? '连接中' : apiStatus === 'up' ? '服务已连接' : '服务未连接' }}</button>
        </div>
      </header>

      <main id="workspace" ref="workspace" class="workspace-main" tabindex="-1">
        <div class="page-heading"><h1>{{ activePage.title }}</h1><p>{{ activePage.description }}</p></div>
        <div v-if="apiStatus === 'offline'" class="connection-notice" role="status"><span><strong>服务暂时无法连接</strong><span>数据加载或操作失败时，请稍后重试。</span></span><button class="secondary-button compact-button" type="button" @click="checkApi">重新连接</button></div>
        <DataState v-if="!sessionReady" loading title="正在加载你的工作台" />
        <div v-else :key="currentAccount?.id ?? 'guest'" class="workspace-content">
          <ProductCatalogPanel v-if="visited.has('catalog')" v-show="activeView === 'catalog'" :can-shop="currentAccount?.role === 'USER'" @sign-in="navigate('account')" />
          <StoreCatalogPanel v-if="visited.has('stores')" v-show="activeView === 'stores'" />
          <AuthPanel v-if="visited.has('account')" v-show="activeView === 'account'" :account="currentAccount" @logged-in="currentAccount = $event" @logged-out="currentAccount = null" />
          <CheckInPanel v-if="hasCheckIn && visited.has('check-in')" v-show="activeView === 'check-in'" :store-id="checkInStoreId" :account="currentAccount" @changed="checkInChanged" @sign-in="navigate('account')" />
          <template v-if="currentAccount?.role === 'USER'">
            <OrderPanel v-if="visited.has('orders')" v-show="activeView === 'orders'" ref="orderPanel" @changed="serviceRevision++" @book="bookOrder" @browse="navigate('catalog')" />
            <UserServicePanel v-if="visited.has('services')" v-show="activeView === 'services'" :order-to-book="orderToBook" :revision="serviceRevision" @changed="orderPanel?.refresh()" />
          </template>
          <template v-if="currentAccount?.role === 'ADMIN'">
            <AdminStorePanel v-if="visited.has('admin-stores')" v-show="activeView === 'admin-stores'" />
            <AdminProductPanel v-if="visited.has('admin-products')" v-show="activeView === 'admin-products'" />
            <AdminOrderPanel v-if="visited.has('admin-orders')" v-show="activeView === 'admin-orders'" />
          </template>
          <template v-if="currentAccount?.role === 'STORE'">
            <StoreOrderPanel v-if="visited.has('store-orders')" v-show="activeView === 'store-orders'" @changed="serviceRevision++" />
            <StoreServicePanel v-if="visited.has('store-services')" v-show="activeView === 'store-services'" :revision="serviceRevision" />
            <StoreQrPanel v-if="visited.has('store-qr')" v-show="activeView === 'store-qr'" />
          </template>
        </div>
      </main>
      <footer class="footer"><span>汽车保养平台</span><span>配件购买与到店保养服务</span></footer>
    </div>
  </div>
</template>
