<script setup lang="ts">
import { ref } from 'vue'
import { login, logout, registerStore, registerUser } from '../api/auth'
import type { AccountResponse } from '../types/auth'

const props = defineProps<{ account: AccountResponse | null }>()
const emit = defineEmits<{
  loggedIn: [account: AccountResponse]
  loggedOut: []
}>()

type FormMode = 'login' | 'register'
type RegisterRole = 'user' | 'store'
type FeedbackKind = 'success' | 'error'

const formMode = ref<FormMode>('login')
const registerRole = ref<RegisterRole>('user')
const isSubmitting = ref(false)
const feedback = ref<{ kind: FeedbackKind; message: string } | null>(null)

const username = ref('')
const password = ref('')
const phone = ref('')
const storeName = ref('')
const contactName = ref('')
const address = ref('')

function resetFeedback() {
  feedback.value = null
}

function selectMode(mode: FormMode) {
  formMode.value = mode
  resetFeedback()
}

function selectRegisterRole(role: RegisterRole) {
  registerRole.value = role
  resetFeedback()
}

async function submit() {
  resetFeedback()
  isSubmitting.value = true

  try {
    if (formMode.value === 'login') {
      const result = await login({ username: username.value, password: password.value })
      feedback.value = { kind: 'success', message: '登录成功。' }
      emit('loggedIn', result.account)
    } else if (registerRole.value === 'user') {
      await registerUser({ username: username.value, password: password.value, phone: phone.value })
      feedback.value = { kind: 'success', message: '用户注册成功，现在可以登录。' }
      formMode.value = 'login'
    } else {
      const result = await registerStore({
        username: username.value,
        password: password.value,
        storeName: storeName.value,
        contactName: contactName.value,
        phone: phone.value,
        address: address.value,
      })
      feedback.value = { kind: 'success', message: result.message }
      formMode.value = 'login'
    }
  } catch (error) {
    feedback.value = {
      kind: 'error',
      message: error instanceof Error ? error.message : '请求失败，请稍后重试。',
    }
  } finally {
    isSubmitting.value = false
  }
}

async function signOut() {
  isSubmitting.value = true
  resetFeedback()
  try {
    await logout()
    emit('loggedOut')
    feedback.value = { kind: 'success', message: '已退出登录。' }
  } catch (error) {
    feedback.value = {
      kind: 'error',
      message: error instanceof Error ? error.message : '退出登录失败，请稍后重试。',
    }
  } finally {
    isSubmitting.value = false
  }
}

function roleLabel(role: 'USER' | 'STORE' | 'ADMIN') {
  return { USER: '用户', STORE: '加盟店', ADMIN: '平台管理员' }[role]
}
</script>

<template>
  <section class="auth-panel">
    <div class="auth-intro">
      <h2>{{ props.account ? '你的账户信息' : '登录，继续安排保养' }}</h2>
      <p>{{ props.account ? '订单、预约和保养记录会保留在当前账户中。更换账号前，请先退出登录。' : '车主可以购买配件、预约到店并查看保养记录。加盟店使用同一入口登录，处理订单与服务。' }}</p>
      <dl class="auth-rules">
        <div><dt>车主账户</dt><dd>注册后即可登录，开始选购和预约。</dd></div>
        <div><dt>加盟店账户</dt><dd>提交门店资料，平台审核通过后即可登录。</dd></div>
        <div><dt>平台管理员</dt><dd>使用已分配的账号登录，管理门店、商品与订单。</dd></div>
      </dl>
    </div>

    <div class="auth-card">
      <div v-if="props.account" class="auth-session">
        <p>已登录</p>
        <strong>{{ props.account.username }}</strong>
        <dl class="session-details"><div><dt>身份</dt><dd>{{ roleLabel(props.account.role) }}</dd></div><div><dt>手机号</dt><dd>{{ props.account.phone || '未填写' }}</dd></div></dl>
        <button class="secondary-button" type="button" :disabled="isSubmitting" @click="signOut">
          {{ isSubmitting ? '处理中…' : '退出登录' }}
        </button>
      </div>

      <template v-else>
        <div class="auth-tabs" role="group" aria-label="账户操作">
          <button type="button" :aria-pressed="formMode === 'login'" :disabled="isSubmitting" :class="{ active: formMode === 'login' }" @click="selectMode('login')">登录</button>
          <button type="button" :aria-pressed="formMode === 'register'" :disabled="isSubmitting" :class="{ active: formMode === 'register' }" @click="selectMode('register')">注册</button>
        </div>

        <div v-if="formMode === 'register'" class="role-tabs" role="group" aria-label="注册身份">
          <button type="button" :aria-pressed="registerRole === 'user'" :disabled="isSubmitting" :class="{ active: registerRole === 'user' }" @click="selectRegisterRole('user')">车主账户</button>
          <button type="button" :aria-pressed="registerRole === 'store'" :disabled="isSubmitting" :class="{ active: registerRole === 'store' }" @click="selectRegisterRole('store')">加盟店</button>
        </div>

        <form class="auth-form" :aria-busy="isSubmitting" @submit.prevent="submit">
          <label>
            <span>用户名</span>
            <input v-model.trim="username" required minlength="3" maxlength="50" autocomplete="username" :disabled="isSubmitting" placeholder="3–50 个字符" />
          </label>

          <label>
            <span>密码</span>
            <input v-model="password" required minlength="8" maxlength="72" type="password" :autocomplete="formMode === 'register' ? 'new-password' : 'current-password'" :disabled="isSubmitting" placeholder="至少 8 个字符" />
          </label>

          <label v-if="formMode === 'register'">
            <span>手机号</span>
            <input v-model.trim="phone" required maxlength="30" type="tel" autocomplete="tel" :disabled="isSubmitting" placeholder="请输入联系电话" />
          </label>

          <template v-if="formMode === 'register' && registerRole === 'store'">
            <label>
              <span>店面名称</span>
              <input v-model.trim="storeName" required maxlength="100" autocomplete="organization" :disabled="isSubmitting" placeholder="例如：明珠汽车保养店" />
            </label>
            <label>
              <span>联系人</span>
              <input v-model.trim="contactName" required maxlength="50" autocomplete="name" :disabled="isSubmitting" placeholder="请输入联系人姓名" />
            </label>
            <label>
              <span>店面地址</span>
              <input v-model.trim="address" required maxlength="255" autocomplete="street-address" :disabled="isSubmitting" placeholder="请输入店面地址" />
            </label>
          </template>

          <button class="auth-submit" type="submit" :disabled="isSubmitting">
            {{ isSubmitting ? '提交中…' : formMode === 'login' ? '登录账户' : registerRole === 'user' ? '注册车主账户' : '提交加盟店申请' }}
          </button>
        </form>
      </template>

      <p v-if="feedback" class="feedback" :class="feedback.kind" :role="feedback.kind === 'error' ? 'alert' : 'status'">{{ feedback.message }}</p>
    </div>
  </section>
</template>
