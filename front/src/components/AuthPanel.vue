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
      feedback.value = { kind: 'success', message: `${result.message}，当前角色：${roleLabel(result.account.role)}` }
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
      <p class="eyebrow">ACCOUNT MODULE</p>
      <h2>先把身份链路跑通</h2>
      <p>用户注册后可直接登录；加盟店注册后进入待审核状态，审核模块完成前不能登录。</p>
      <dl class="auth-rules">
        <div><dt>USER</dt><dd>注册即生效</dd></div>
        <div><dt>STORE</dt><dd>注册后待平台审核</dd></div>
        <div><dt>SESSION</dt><dd>登录后使用服务端会话</dd></div>
      </dl>
    </div>

    <div class="auth-card">
      <div v-if="props.account" class="auth-session">
        <p>当前登录：<strong>{{ props.account.username }}</strong>（{{ roleLabel(props.account.role) }}）</p>
        <button type="button" :disabled="isSubmitting" @click="signOut">
          {{ isSubmitting ? '处理中…' : '退出登录' }}
        </button>
      </div>

      <template v-else>
        <div class="auth-tabs" role="tablist" aria-label="账户操作">
          <button type="button" :class="{ active: formMode === 'login' }" @click="selectMode('login')">登录</button>
          <button type="button" :class="{ active: formMode === 'register' }" @click="selectMode('register')">注册</button>
        </div>

        <div v-if="formMode === 'register'" class="role-tabs" role="tablist" aria-label="注册身份">
          <button type="button" :class="{ active: registerRole === 'user' }" @click="selectRegisterRole('user')">用户</button>
          <button type="button" :class="{ active: registerRole === 'store' }" @click="selectRegisterRole('store')">加盟店</button>
        </div>

        <form class="auth-form" @submit.prevent="submit">
          <label>
            <span>登录用户名</span>
            <input v-model.trim="username" required minlength="3" maxlength="50" autocomplete="username" placeholder="请输入用户名" />
          </label>

          <label>
            <span>密码</span>
            <input v-model="password" required minlength="8" maxlength="72" type="password" autocomplete="current-password" placeholder="至少 8 个字符" />
          </label>

          <label v-if="formMode === 'register'">
            <span>手机号</span>
            <input v-model.trim="phone" required maxlength="30" autocomplete="tel" placeholder="用于联系和后续服务通知" />
          </label>

          <template v-if="formMode === 'register' && registerRole === 'store'">
            <label>
              <span>店面名称</span>
              <input v-model.trim="storeName" required maxlength="100" placeholder="例如：明珠汽车保养店" />
            </label>
            <label>
              <span>联系人</span>
              <input v-model.trim="contactName" required maxlength="50" placeholder="请输入联系人姓名" />
            </label>
            <label>
              <span>店面地址</span>
              <input v-model.trim="address" required maxlength="255" placeholder="请输入店面地址" />
            </label>
          </template>

          <button class="auth-submit" type="submit" :disabled="isSubmitting">
            {{ isSubmitting ? '提交中…' : formMode === 'login' ? '登录账户' : registerRole === 'user' ? '注册用户' : '提交加盟店申请' }}
          </button>
        </form>
      </template>

      <p v-if="feedback" class="feedback" :class="feedback.kind" role="status">{{ feedback.message }}</p>
    </div>
  </section>
</template>
