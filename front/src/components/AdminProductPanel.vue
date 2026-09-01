<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { adjustProductStock, createProduct, listAdminProducts, updateProductStatus } from '../api/products'
import type { Product, ProductStatus } from '../types/product'

const products = ref<Product[]>([])
const isLoading = ref(false)
const actingId = ref<number | null>(null)
const feedback = ref('')
const form = ref({ sku: '', productName: '', brand: '', price: '0.00', laborFee: '0.00', stock: 0, description: '' })

async function refresh() {
  isLoading.value = true
  feedback.value = ''
  try {
    products.value = (await listAdminProducts()).content
  } catch (error) {
    feedback.value = error instanceof Error ? error.message : '商品管理数据加载失败。'
  } finally {
    isLoading.value = false
  }
}

async function addProduct() {
  isLoading.value = true
  feedback.value = ''
  try {
    await createProduct(form.value)
    feedback.value = '商品已创建。'
    form.value = { sku: '', productName: '', brand: '', price: '0.00', laborFee: '0.00', stock: 0, description: '' }
    await refresh()
  } catch (error) {
    feedback.value = error instanceof Error ? error.message : '商品创建失败。'
  } finally {
    isLoading.value = false
  }
}

async function changeStatus(product: Product) {
  const status: ProductStatus = product.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  await act(product.id, () => updateProductStatus(product.id, status), '商品状态已更新。')
}

async function changeStock(product: Product, delta: number) {
  await act(product.id, () => adjustProductStock(product.id, delta), '库存已更新。')
}

async function act(id: number, action: () => Promise<Product>, success: string) {
  actingId.value = id
  feedback.value = ''
  try {
    await action()
    feedback.value = success
    await refresh()
  } catch (error) {
    feedback.value = error instanceof Error ? error.message : '操作失败。'
  } finally {
    actingId.value = null
  }
}

onMounted(refresh)
</script>

<template>
  <section class="admin-panel">
    <div class="admin-panel-heading">
      <div>
        <p class="eyebrow">ADMIN INVENTORY</p>
        <h2>商品与库存</h2>
      </div>
      <button class="secondary-button" type="button" :disabled="isLoading" @click="refresh">刷新商品</button>
    </div>
    <p v-if="feedback" class="feedback" :class="feedback.includes('失败') || feedback.includes('错误') ? 'error' : 'success'" role="status">{{ feedback }}</p>
    <form class="product-form" @submit.prevent="addProduct">
      <input v-model.trim="form.sku" placeholder="SKU" required maxlength="50" />
      <input v-model.trim="form.productName" placeholder="商品名称" required maxlength="100" />
      <input v-model.trim="form.brand" placeholder="品牌" maxlength="50" />
      <input v-model="form.price" placeholder="配件价格" type="number" min="0" step="0.01" required />
      <input v-model="form.laborFee" placeholder="工时费" type="number" min="0" step="0.01" required />
      <input v-model.number="form.stock" placeholder="初始库存" type="number" min="0" required />
      <input v-model.trim="form.description" class="product-form-wide" placeholder="描述（可选）" maxlength="2000" />
      <button class="primary-button" type="submit" :disabled="isLoading">新增商品</button>
    </form>
    <p v-if="!isLoading && products.length === 0" class="admin-empty">暂无商品。</p>
    <div v-else class="admin-product-list">
      <article v-for="product in products" :key="product.id" class="admin-product-item">
        <div>
          <strong>{{ product.productName }}</strong>
          <p>{{ product.sku }} · ¥{{ product.price }} + 工时费 ¥{{ product.laborFee }} · 库存 {{ product.stock }}</p>
        </div>
        <div class="application-actions">
          <button class="secondary-button compact-button" type="button" :disabled="actingId !== null" @click="changeStock(product, 1)">+1</button>
          <button class="secondary-button compact-button" type="button" :disabled="actingId !== null || product.stock === 0" @click="changeStock(product, -1)">-1</button>
          <button class="approve-button" type="button" :disabled="actingId !== null" @click="changeStatus(product)">{{ product.status === 'ACTIVE' ? '下架' : '上架' }}</button>
        </div>
      </article>
    </div>
  </section>
</template>
