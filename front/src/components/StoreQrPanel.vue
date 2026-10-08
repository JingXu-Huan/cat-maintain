<script setup lang="ts">
import { onMounted, ref } from 'vue'
import QRCode from 'qrcode'
import { getStoreProfile } from '../api/checkIn'
import type { StoreSummary } from '../types/store'
import DataState from './DataState.vue'

const store = ref<StoreSummary | null>(null)
const siteUrl = ref(window.location.origin)
const checkInUrl = ref('')
const qrImage = ref('')
const feedback = ref('')
const busy = ref(false)
async function generate() {
  if (!store.value) return
  busy.value = true; feedback.value = ''; qrImage.value = ''; checkInUrl.value = ''
  try {
    const url = new URL(siteUrl.value)
    if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password) throw new Error('请填写有效的 HTTP 或 HTTPS 网站地址。')
    url.search = ''; url.hash = 'check-in'; url.searchParams.set('checkInStore', String(store.value.id))
    const target = url.toString()
    qrImage.value = await QRCode.toDataURL(target, { width: 280, margin: 4, errorCorrectionLevel: 'M' })
    checkInUrl.value = target
  } catch (error) { feedback.value = error instanceof Error ? error.message : '二维码生成失败。' }
  finally { busy.value = false }
}
async function loadProfile() {
  busy.value = true
  try { store.value = await getStoreProfile(); await generate() }
  catch (error) { feedback.value = error instanceof Error ? error.message : '门店资料加载失败。' }
  finally { busy.value = false }
}
onMounted(loadProfile)
</script>

<template>
  <section class="service-panel">
    <div class="section-heading"><div><h2>到店登记二维码{{ store ? ' · ' + store.storeName : '' }}</h2></div><button class="secondary-button" type="button" :disabled="busy" @click="loadProfile">{{ busy ? '生成中…' : '刷新二维码' }}</button></div>
    <p class="panel-description">用户用手机扫码，登录后选择预约并输入订单核销码完成登记。</p>
    <form class="verify-form" @submit.prevent="generate"><label>手机可访问的网站地址<input v-model.trim="siteUrl" class="inline-input" type="url" required /></label><button class="primary-button" type="submit" :disabled="busy || !store">生成二维码</button></form>
    <p v-if="feedback" class="feedback error" role="alert">{{ feedback }}</p>
    <DataState v-if="busy && !qrImage" loading title="正在准备门店二维码" />
    <div v-if="qrImage" class="qr-card"><img :src="qrImage" width="280" height="280" :alt="`${store?.storeName} 到店登记二维码`" /><div><div class="application-actions"><a :href="checkInUrl" class="secondary-button" target="_blank" rel="noopener">打开登记页面（新窗口）</a><a :href="qrImage" download="到店登记二维码.png" class="primary-button">保存二维码</a></div><p class="check-in-url">{{ checkInUrl }}</p><p>手机扫码时，请填写手机可访问的局域网地址或正式域名。localhost 只能在当前电脑访问。</p></div></div>
  </section>
</template>
