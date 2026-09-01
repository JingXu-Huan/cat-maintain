<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { approveStore, listStoreApplications, rejectStore } from '../api/admin'
import type { StoreApplication } from '../types/store'

const applications = ref<StoreApplication[]>([])
const isLoading = ref(false)
const actingStoreId = ref<number | null>(null)
const rejectReason = ref('资料审核未通过')
const feedback = ref('')

async function refresh() {
  isLoading.value = true
  feedback.value = ''
  try {
    applications.value = await listStoreApplications()
  } catch (error) {
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
    feedback.value = `已通过「${application.storeName}」的申请。`
    await refresh()
  } catch (error) {
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
    feedback.value = `已拒绝「${application.storeName}」的申请。`
    await refresh()
  } catch (error) {
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
        <p class="eyebrow">ADMIN REVIEW</p>
        <h2>加盟店审核</h2>
      </div>
      <button class="secondary-button" type="button" :disabled="isLoading" @click="refresh">
        {{ isLoading ? '加载中…' : '刷新申请' }}
      </button>
    </div>

    <p v-if="feedback" class="feedback" :class="feedback.includes('失败') || feedback.includes('错误') ? 'error' : 'success'" role="status">
      {{ feedback }}
    </p>

    <label class="reject-reason">
      <span>拒绝备注</span>
      <input v-model.trim="rejectReason" maxlength="255" />
    </label>

    <p v-if="!isLoading && applications.length === 0" class="admin-empty">当前没有待审核加盟店。</p>
    <div v-else class="application-list">
      <article v-for="application in applications" :key="application.id" class="application-item">
        <div>
          <strong>{{ application.storeName }}</strong>
          <p>{{ application.username }} · {{ application.contactName }} · {{ application.phone }}</p>
          <small>{{ application.address }}</small>
        </div>
        <div class="application-actions">
          <button class="approve-button" type="button" :disabled="actingStoreId !== null" @click="approve(application)">通过</button>
          <button class="reject-button" type="button" :disabled="actingStoreId !== null" @click="reject(application)">拒绝</button>
        </div>
      </article>
    </div>
  </section>
</template>
