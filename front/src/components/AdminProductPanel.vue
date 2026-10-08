<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { adjustProductStock, createProduct, deleteProduct, listAdminProducts, updateProduct, updateProductStatus } from '../api/products'
import type { Product, ProductStatus } from '../types/product'
import PaginationControls from './PaginationControls.vue'
import ConfirmDialog from './ConfirmDialog.vue'
import DataState from './DataState.vue'

const products = ref<Product[]>([])
const isLoading = ref(false)
const actingId = ref<number | null>(null)
const feedback = ref('')
const hasError = ref(false)
const editingId = ref<number | null>(null)
const deletingProduct = ref<Product | null>(null)
const confirmingDelete = ref(false)
const page = ref(0)
const total = ref(0)
const form = ref({ sku: '', productName: '', brand: '', price: '0.00', laborFee: '0.00', stock: 0, description: '' })

async function refresh(nextPage = page.value): Promise<void> {
  isLoading.value = true
  if (hasError.value) { feedback.value = ''; hasError.value = false }
  try {
    const response = await listAdminProducts(undefined, nextPage, 20)
    const lastPage = Math.max(0, Math.ceil(response.total / 20) - 1)
    if (nextPage > lastPage) { await refresh(lastPage); return }
    products.value = response.content
    page.value = nextPage
    total.value = response.total
  } catch (error) {
    hasError.value = true
    feedback.value = error instanceof Error ? error.message : '商品管理数据加载失败。'
  } finally {
    isLoading.value = false
  }
}

async function addProduct() {
  isLoading.value = true
  feedback.value = ''
  try {
    const editing = editingId.value !== null
    if (editingId.value !== null) await updateProduct(editingId.value, form.value)
    else await createProduct(form.value)
    resetForm()
    hasError.value = false
    feedback.value = editing ? '商品已保存。' : '商品已创建。'
    await refresh(editing ? page.value : 0)
  } catch (error) {
    hasError.value = true
    feedback.value = error instanceof Error ? error.message : '商品创建失败。'
  } finally {
    isLoading.value = false
  }
}

function resetForm() {
  editingId.value = null
  form.value = { sku: '', productName: '', brand: '', price: '0.00', laborFee: '0.00', stock: 0, description: '' }
}

function editProduct(product: Product) {
  editingId.value = product.id
  form.value = { sku: product.sku, productName: product.productName, brand: product.brand ?? '',
    price: String(product.price), laborFee: String(product.laborFee), stock: product.stock, description: product.description ?? '' }
  document.getElementById('product-sku')?.focus()
}

function requestDelete(product: Product) {
  deletingProduct.value = product
  confirmingDelete.value = true
}

function confirmDelete() {
  if (deletingProduct.value) void removeProduct(deletingProduct.value)
}

async function changeStatus(product: Product) {
  const status: ProductStatus = product.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  await act(product.id, () => updateProductStatus(product.id, status), '商品状态已更新。')
}

async function changeStock(product: Product, delta: number) {
  await act(product.id, () => adjustProductStock(product.id, delta), '库存已更新。')
}

async function removeProduct(product: Product) {
  await act(product.id, () => deleteProduct(product.id), '商品已删除。')
  if (!products.value.some((item) => item.id === editingId.value)) resetForm()
}

async function act(id: number, action: () => Promise<unknown>, success: string) {
  actingId.value = id
  feedback.value = ''
  try {
    await action()
    hasError.value = false
    feedback.value = success
    await refresh()
  } catch (error) {
    hasError.value = true
    feedback.value = error instanceof Error ? error.message : '操作失败。'
  } finally {
    actingId.value = null
  }
}

onMounted(() => refresh())
</script>

<template>
  <section class="admin-panel">
    <div class="admin-panel-heading">
      <div>
        <h2>商品与库存</h2>
      </div>
      <button class="secondary-button" type="button" :disabled="isLoading || actingId !== null" @click="refresh()">刷新商品</button>
    </div>
    <p v-if="feedback" class="feedback" :class="hasError ? 'error' : 'success'" :role="hasError ? 'alert' : 'status'">{{ feedback }}</p>
    <h3 id="product-editor">{{ editingId === null ? '新增商品' : '编辑商品' }}</h3>
    <form class="product-form" :aria-busy="isLoading" @submit.prevent="addProduct">
      <label>商品编码（SKU）<input id="product-sku" v-model.trim="form.sku" placeholder="例如：OIL-5W30-4L" required maxlength="50" :disabled="isLoading || actingId !== null" /></label>
      <label>商品名称<input v-model.trim="form.productName" placeholder="填写配件名称" required maxlength="100" :disabled="isLoading || actingId !== null" /></label>
      <label>品牌（可选）<input v-model.trim="form.brand" placeholder="填写品牌" maxlength="50" :disabled="isLoading || actingId !== null" /></label>
      <label>配件价格（元）<input v-model="form.price" type="number" inputmode="decimal" min="0" step="0.01" required :disabled="isLoading || actingId !== null" /></label>
      <label>单件工时费（元）<input v-model="form.laborFee" type="number" inputmode="decimal" min="0" step="0.01" required :disabled="isLoading || actingId !== null" /></label>
      <label>{{ editingId === null ? '初始库存（件）' : '库存（件）' }}<input v-model.number="form.stock" type="number" min="0" step="1" required :disabled="isLoading || actingId !== null" /></label>
      <label class="product-form-wide">商品描述（可选）<textarea v-model.trim="form.description" placeholder="用途、适配信息或规格说明" maxlength="2000" :disabled="isLoading || actingId !== null"></textarea></label>
      <div class="form-actions"><button class="primary-button" type="submit" :disabled="isLoading || actingId !== null">{{ isLoading ? '处理中…' : editingId === null ? '新增商品' : '保存修改' }}</button>
      <button v-if="editingId !== null" class="secondary-button" type="button" :disabled="isLoading" @click="resetForm">取消编辑</button></div>
    </form>
    <h3>商品列表</h3>
    <DataState v-if="isLoading && !products.length" loading title="正在加载商品" />
    <DataState v-else-if="!products.length" :title="hasError ? '商品暂时未能加载' : '尚未创建商品'" :description="hasError ? '请点击刷新商品重试。' : '使用上方表单添加第一款配件。'" />
    <div v-else class="admin-product-list">
      <article v-for="product in products" :key="product.id" class="admin-product-item">
        <div>
          <strong>{{ product.productName }} <span class="status-badge" :class="product.status === 'ACTIVE' ? 'success' : ''">{{ product.status === 'ACTIVE' ? '已上架' : '已下架' }}</span></strong>
          <p>{{ product.sku }} · ¥{{ product.price }} + 工时费 ¥{{ product.laborFee }} · 库存 {{ product.stock }}</p>
        </div>
        <div class="application-actions">
          <button class="secondary-button compact-button" type="button" :disabled="isLoading || actingId !== null" @click="editProduct(product)">编辑</button>
          <button class="secondary-button compact-button" type="button" :aria-label="`${product.productName} 库存增加 1 件`" :disabled="isLoading || actingId !== null" @click="changeStock(product, 1)">库存 +1</button>
          <button class="secondary-button compact-button" type="button" :aria-label="`${product.productName} 库存减少 1 件`" :disabled="isLoading || actingId !== null || product.stock === 0" @click="changeStock(product, -1)">库存 −1</button>
          <button class="secondary-button compact-button" type="button" :disabled="isLoading || actingId !== null" @click="changeStatus(product)">{{ product.status === 'ACTIVE' ? '下架' : '上架' }}</button>
          <button class="reject-button" type="button" :disabled="isLoading || actingId !== null" @click="requestDelete(product)">删除</button>
        </div>
      </article>
    </div>
    <PaginationControls :page="page" :total="total" :disabled="isLoading || actingId !== null" @change="refresh" />
    <ConfirmDialog v-model:open="confirmingDelete" title="删除这款商品？" :description="`删除「${deletingProduct?.productName ?? ''}」后，将无法在商品列表中找回。已被订单引用的商品不能删除，可改为下架。`" @confirm="confirmDelete" />
  </section>
</template>
