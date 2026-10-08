import { computed, ref, watch } from 'vue'
import { getJson } from '../api/http'
import type { Product } from '../types/product'
import { toCents } from '../utils/format'

interface CartEntry { product: Product; quantity: number; unavailable?: boolean }
const entries = ref<CartEntry[]>([])
const checkingOut = ref(false)
let accountId: number | null = null

watch(entries, (value) => {
  if (accountId !== null) {
    try { localStorage.setItem(`cat-maintain:cart:${accountId}`, JSON.stringify(value)) } catch { /* 允许当前页面继续购物。 */ }
  }
}, { deep: true, flush: 'sync' })

export function setCartAccount(id: number | null) {
  accountId = id
  let saved: CartEntry[] = []
  if (id !== null) {
    try {
      const data: unknown = JSON.parse(localStorage.getItem(`cat-maintain:cart:${id}`) ?? '[]')
      if (Array.isArray(data)) saved = data.filter((item): item is CartEntry =>
        item?.product && Number.isSafeInteger(item.product.id) && item.product.id > 0
        && typeof item.product.productName === 'string' && Number.isFinite(Number(item.product.price))
        && Number.isFinite(Number(item.product.laborFee)) && Number.isInteger(item.quantity)
        && item.quantity >= 1 && item.quantity <= 99).slice(0, 50)
    } catch { saved = [] }
  }
  entries.value = saved
}

export function useCart() {
  const productAmount = computed(() => entries.value.reduce((sum, item) => sum + toCents(item.product.price) * item.quantity, 0))
  const laborAmount = computed(() => entries.value.reduce((sum, item) => sum + toCents(item.product.laborFee) * item.quantity, 0))
  const itemCount = computed(() => entries.value.reduce((sum, item) => sum + item.quantity, 0))
  function add(product: Product) {
    if (checkingOut.value) return '订单正在提交，请稍后加购。'
    if (accountId === null) return '请先使用用户账号登录。'
    const item = entries.value.find((entry) => entry.product.id === product.id)
    if ((item?.quantity ?? 0) >= Math.min(product.stock, 99)) return '已达到库存数量或单件商品数量上限。'
    if (!item && entries.value.length >= 50) return '购物车最多容纳 50 种商品。'
    if (item) { item.product = product; item.quantity++; item.unavailable = false }
    else entries.value.push({ product, quantity: 1 })
    return null
  }
  function remove(productId: number) { entries.value = entries.value.filter((item) => item.product.id !== productId) }
  function setQuantity(productId: number, quantity: number) {
    if (checkingOut.value) return '订单正在提交，请稍后调整数量。'
    const entry = entries.value.find((item) => item.product.id === productId)
    if (!entry) return '购物车商品已变化，请重新查看。'
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > 99) return '数量必须是 1–99 件之间的整数。'
    const limit = Math.min(Number.isFinite(entry.product.stock) ? entry.product.stock : 99, 99)
    if (quantity > limit) return limit === 0 ? '商品暂时缺货，请移除或稍后重试。' : `最多可购买 ${limit} 件，请调整数量。`
    entry.quantity = quantity
    return null
  }
  function clear() { entries.value = [] }
  function getAccountId() { return accountId }
  function clearForAccount(id: number) {
    try { localStorage.removeItem(`cat-maintain:cart:${id}`) } catch { /* 当前页面仍能清空。 */ }
    if (accountId === id) clear()
  }
  async function refreshPrices() {
    const results = await Promise.allSettled(entries.value.map(async (entry) => {
      try { entry.product = await getJson<Product>(`/api/products/${entry.product.id}`); entry.unavailable = false }
      catch (error) { entry.unavailable = true; throw error }
    }))
    const failed = results.find((result) => result.status === 'rejected')
    if (failed?.status === 'rejected') throw failed.reason
  }
  return { entries, checkingOut, productAmount, laborAmount, itemCount, add, remove, setQuantity, clear, clearForAccount, getAccountId, refreshPrices }
}
