<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getCurrentAccount } from './api/auth'
import { getJson } from './api/http'
import AdminProductPanel from './components/AdminProductPanel.vue'
import AdminOrderPanel from './components/AdminOrderPanel.vue'
import AdminStorePanel from './components/AdminStorePanel.vue'
import AuthPanel from './components/AuthPanel.vue'
import OrderPanel from './components/OrderPanel.vue'
import ProductCatalogPanel from './components/ProductCatalogPanel.vue'
import StoreOrderPanel from './components/StoreOrderPanel.vue'
import type { AccountResponse } from './types/auth'
import type { HealthResponse } from './types/health'

type ApiStatus = 'checking' | 'up' | 'offline'

interface PlatformModule {
  label: string
  title: string
  description: string
  accent: string
}

const apiStatus = ref<ApiStatus>('checking')
const apiMessage = ref('正在检查后端接口')
const currentAccount = ref<AccountResponse | null>(null)

const platformModules: PlatformModule[] = [
  {
    label: '用户端',
    title: '选配件 · 下单 · 预约',
    description: '覆盖注册登录、商品浏览、购物车、订单、到店预约和保养记录。',
    accent: 'blue',
  },
  {
    label: '加盟店端',
    title: '接单 · 核销 · 留存',
    description: '支持加盟店注册，凭数字凭据调取订单与预约，完成到店保养登记。',
    accent: 'green',
  },
  {
    label: '平台管理端',
    title: '审核 · 商品 · 库存',
    description: '平台审核加盟店和用户订单，维护汽车零配件、价格及保养工时费。',
    accent: 'orange',
  },
]

async function checkApi() {
  apiStatus.value = 'checking'
  apiMessage.value = '正在检查后端接口'

  try {
    const health = await getJson<HealthResponse>('/api/health')
    apiStatus.value = health.status === 'UP' ? 'up' : 'offline'
    apiMessage.value = apiStatus.value === 'up' ? '后端已连接' : '后端状态异常'
  } catch (error) {
    apiStatus.value = 'offline'
    apiMessage.value = error instanceof Error ? error.message : '暂时无法连接后端'
  }
}

async function loadCurrentAccount() {
  try {
    currentAccount.value = await getCurrentAccount()
  } catch {
    currentAccount.value = null
  }
}

onMounted(async () => {
  await checkApi()
  await loadCurrentAccount()
})
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <a class="brand" href="/" aria-label="汽车保养平台首页">
        <span class="brand-mark">CM</span>
        <span>
          <strong>汽车保养平台</strong>
          <small>Car Maintain</small>
        </span>
      </a>

      <div class="topbar-actions">
        <span class="environment-tag">初始化环境</span>
        <button class="status-button" type="button" @click="checkApi">
          <span class="status-dot" :class="apiStatus" aria-hidden="true"></span>
          {{ apiMessage }}
        </button>
      </div>
    </header>

    <main>
      <section class="hero-section">
        <div class="hero-copy">
          <p class="eyebrow">JAVA ENTERPRISE PROJECT PRACTICE</p>
          <h1>让每一次保养，<span>有据可查。</span></h1>
          <p class="hero-description">
            面向用户、加盟店与平台管理人员的汽车配件购买和到店保养协同平台。
            当前前后端基础工程已就绪，后续功能按角色和业务流程逐步接入。
          </p>
          <div class="hero-actions">
            <button class="primary-button" type="button" @click="checkApi">检查后端连接</button>
            <a class="secondary-button" href="#modules">查看模块规划</a>
          </div>
        </div>

        <div class="hero-card" aria-label="项目状态">
          <div class="hero-card-header">
            <span>PROJECT STATUS</span>
            <span class="live-label"><i></i> DEV</span>
          </div>
          <div class="hero-card-number">01</div>
          <div class="hero-card-title">基础工程初始化</div>
          <div class="progress-track"><span></span></div>
          <div class="hero-card-footer">
            <span>Spring Boot + MyBatis</span>
            <span>Vue + Vite</span>
          </div>
        </div>
      </section>

      <section id="modules" class="modules-section">
        <div class="section-heading">
          <div>
            <p class="eyebrow">DOMAIN MODULES</p>
            <h2>从三类角色出发组织业务</h2>
          </div>
          <p>按任务书中的注册、审核、购买、配送、预约、核销和保养记录拆分。</p>
        </div>

        <div class="module-grid">
          <article v-for="module in platformModules" :key="module.label" class="module-card">
            <div class="module-icon" :class="module.accent">{{ module.label.slice(0, 1) }}</div>
            <p class="module-label">{{ module.label }}</p>
            <h3>{{ module.title }}</h3>
            <p>{{ module.description }}</p>
            <span class="module-arrow" aria-hidden="true">↗</span>
          </article>
        </div>
      </section>

      <AuthPanel
        :account="currentAccount"
        @logged-in="currentAccount = $event"
        @logged-out="currentAccount = null"
      />

      <ProductCatalogPanel />

      <OrderPanel v-if="currentAccount?.role === 'USER'" />

      <AdminStorePanel v-if="currentAccount?.role === 'ADMIN'" />
      <AdminProductPanel v-if="currentAccount?.role === 'ADMIN'" />
      <AdminOrderPanel v-if="currentAccount?.role === 'ADMIN'" />
      <StoreOrderPanel v-if="currentAccount?.role === 'STORE'" />

      <section class="stack-section">
        <div class="stack-heading">
          <p class="eyebrow">CURRENT STACK</p>
          <h2>清晰的前后端边界</h2>
        </div>
        <div class="stack-list">
          <div><strong>01</strong><span>Spring Boot 4</span><small>Java 21 · REST API</small></div>
          <div><strong>02</strong><span>后端数据层</span><small>MyBatis · MySQL</small></div>
          <div><strong>03</strong><span>Vue 3</span><small>TypeScript · Vite</small></div>
        </div>
      </section>
    </main>

    <footer class="footer">
      <span>cat-maintain</span>
      <span>前端目录：D:\WorkSpace\cat-maintain\front</span>
    </footer>
  </div>
</template>
