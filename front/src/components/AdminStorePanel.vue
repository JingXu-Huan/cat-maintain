<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { approveStore, listStoreApplications, rejectStore } from '../api/admin'
import type { StoreApplication } from '../types/store'
import DataState from './DataState.vue'

const applications = ref<StoreApplication[]>([])
const isLoading = ref(false)
const actingStoreId = ref<number | null>(null)
const rejectReason = ref('资料审核未通过')
const feedback = ref('')
const hasError = ref(false)

async function refresh() {
  isLoading.value = true
  try {
    applications.value = await listStoreApplications()
  } catch (error) {
    hasError.value = true
    feedback.value = error instanceof Error ? error.message : '加载加盟店申请失败。'
  } finally {
    isLoading.value = false
  }
}

async function approve(application: StoreApplication) {
  actingStoreId.value = application.id
  feedback.value = ''
  try {
    await approveStore(application.id)
    hasError.value = false
    feedback.value = `已通过「${application.storeName}」的申请。`
    await refresh()
  } catch (error) {
    hasError.value = true
    feedback.value = error instanceof Error ? error.message : '审核失败。'
  } finally {
    actingStoreId.value = null
  }
}

async function reject(application: StoreApplication) {
  actingStoreId.value = application.id
  feedback.value = ''
  try {
    await rejectStore(application.id, rejectReason.value)
    hasError.value = false
    feedback.value = `已拒绝「${application.storeName}」的申请。`
    await refresh()
  } catch (error) {
    hasError.value = true
    feedback.value = error instanceof Error ? error.message : '审核失败。'
  } finally {
    actingStoreId.value = null
  }
}

onMounted(refresh)
</script>

<template>
  <section class="admin-panel">
    <div class="admin-panel-heading">
      <div>
        <h2>加盟店审核</h2>
      </div>
      <button class="secondary-button" type="button" :disabled="isLoading || actingStoreId !== null" @click="feedback = ''; hasError = false; refresh()">
        {{ isLoading ? '加载中…' : '刷新申请' }}
      </button>
    </div>

    <p v-if="feedback" class="feedback" :class="hasError ? 'error' : 'success'" :role="hasError ? 'alert' : 'status'">
      {{ feedback }}
    </p>

    <label class="reject-reason">
      <span>拒绝申请时的说明</span>
      <input v-model.trim="rejectReason" maxlength="255" :disabled="isLoading || actingStoreId !== null" />
    </label>

    <DataState v-if="isLoading && !applications.length" loading title="正在加载申请" />
    <DataState v-else-if="!applications.length" :title="hasError ? '申请暂时未能加载' : '没有待审核的加盟店'" :description="hasError ? '请点击刷新申请重试。' : '新门店提交资料后，申请会显示在这里。'" />
    <div v-else class="application-list">
      <article v-for="application in applications" :key="application.id" class="application-item">
        <div>
          <strong>{{ application.storeName }}</strong>
          <p>{{ application.username }} · {{ application.contactName }} · {{ application.phone }}</p>
          <small>{{ application.address }}</small>
        </div>
        <div class="application-actions">
          <button class="approve-button" type="button" :disabled="isLoading || actingStoreId !== null" @click="approve(application)">通过申请</button>
          <button class="reject-button" type="button" :disabled="isLoading || actingStoreId !== null || !rejectReason" @click="reject(application)">拒绝申请</button>
        </div>
      </article>
    </div>
  </section>
</template>
