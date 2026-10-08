<script setup lang="ts">
import { toastSuccess } from '../composables/useToast'
import { computed, nextTick, ref } from 'vue'
import { login, logout, registerStore, registerUser } from '../api/auth'
import type { AccountResponse } from '../types/auth'

const props = defineProps<{ account: AccountResponse | null }>()
const emit = defineEmits<{
  loggedIn: [account: AccountResponse]
  loggedOut: []
}>()

type FormMode = 'login' | 'register'
type RegisterRole = 'user' | 'store'
type FieldName = 'username' | 'password' | 'phone' | 'storeName' | 'contactName' | 'address'

const formMode = ref<FormMode>('login')
const registerRole = ref<RegisterRole>('user')
const isSubmitting = ref(false)
const feedback = ref<{ kind: 'error'; message: string } | null>(null)

const username = ref('')
const password = ref('')
const phone = ref('')
const storeName = ref('')
const contactName = ref('')
const address = ref('')
const formElement = ref<HTMLFormElement | null>(null)
const touchedFields = ref<Partial<Record<FieldName, boolean>>>({})

const activeFields = computed<FieldName[]>(() => {
  if (formMode.value === 'login') return ['username', 'password']
  if (registerRole.value === 'user') return ['username', 'password', 'phone']
  return ['username', 'password', 'phone', 'storeName', 'contactName', 'address']
})

function validateText(value: string, label: string, min: number, max?: number) {
  if (!value.trim()) return `请输入${label}。`
  if (value.length < min) return `${label}至少需要 ${min} 个字符。`
  if (max !== undefined && value.length > max) return `${label}不能超过 ${max} 个字符。`
  return ''
}

const fieldErrors = computed<Record<FieldName, string>>(() => {
  const isRegistering = formMode.value === 'register'
  let passwordError = validateText(password.value, '密码', isRegistering ? 8 : 1, isRegistering ? 72 : undefined)
  if (isRegistering && !passwordError && new TextEncoder().encode(password.value).length > 72) {
    passwordError = '密码过长，请减少字符；中文和表情会占用更多长度。'
  }
  return {
    username: validateText(username.value, '用户名', isRegistering ? 3 : 1, 50),
    password: passwordError,
    phone: validateText(phone.value, registerRole.value === 'store' ? '联系电话' : '手机号', 1, 30),
    storeName: validateText(storeName.value, '店面名称', 1, 100),
    contactName: validateText(contactName.value, '联系人', 1, 50),
    address: validateText(address.value, '店面地址', 1, 255),
  }
})

const visibleErrors = computed(() =>
  activeFields.value.reduce<Partial<Record<FieldName, string>>>((errors, field) => {
    if (touchedFields.value[field]) errors[field] = fieldErrors.value[field]
    return errors
  }, {}),
)
const isFormValid = computed(() => activeFields.value.every((field) => !fieldErrors.value[field]))

function resetValidation() {
  touchedFields.value = {}
}

function touchField(field: FieldName) {
  touchedFields.value[field] = true
  resetFeedback()
}

function resetFeedback() {
  feedback.value = null
}

function selectMode(mode: FormMode) {
  if (formMode.value === mode) return
  formMode.value = mode
  resetValidation()
  resetFeedback()
}

function selectRegisterRole(role: RegisterRole) {
  if (registerRole.value === role) return
  registerRole.value = role
  resetValidation()
  resetFeedback()
}

async function submit() {
  if (isSubmitting.value) return
  resetFeedback()
  activeFields.value.forEach((field) => {
    touchedFields.value[field] = true
  })
  if (!isFormValid.value) {
    await nextTick()
    formElement.value?.querySelector<HTMLInputElement>('[aria-invalid="true"]')?.focus()
    return
  }
  isSubmitting.value = true

  try {
    if (formMode.value === 'login') {
      const result = await login({ username: username.value, password: password.value })
      toastSuccess('登录成功。')
      emit('loggedIn', result.account)
    } else if (registerRole.value === 'user') {
      await registerUser({ username: username.value, password: password.value, phone: phone.value })
      toastSuccess('用户注册成功，现在可以登录。')
      formMode.value = 'login'
      resetValidation()
    } else {
      const result = await registerStore({
        username: username.value,
        password: password.value,
        storeName: storeName.value,
        contactName: contactName.value,
        phone: phone.value,
        address: address.value,
      })
      toastSuccess(result.message)
      formMode.value = 'login'
      resetValidation()
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
    toastSuccess('已退出登录。')
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
      <p>
        {{
          props.account
            ? '订单、预约和保养记录会保留在当前账户中。更换账号前，请先退出登录。'
            : '车主可以购买配件、预约到店并查看保养记录。加盟店使用同一入口登录，处理订单与服务。'
        }}
      </p>
      <dl class="auth-rules">
        <div>
          <dt>车主账户</dt>
          <dd>注册后即可登录，开始选购和预约。</dd>
        </div>
        <div>
          <dt>加盟店账户</dt>
          <dd>提交门店资料，平台审核通过后即可登录。</dd>
        </div>
        <div>
          <dt>平台管理员</dt>
          <dd>使用已分配的账号登录，管理门店、商品与订单。</dd>
        </div>
      </dl>
    </div>

    <div class="auth-card">
      <div v-if="props.account" class="auth-session">
        <p>已登录</p>
        <strong>{{ props.account.username }}</strong>
        <dl class="session-details">
          <div>
            <dt>身份</dt>
            <dd>{{ roleLabel(props.account.role) }}</dd>
          </div>
          <div>
            <dt>手机号</dt>
            <dd>{{ props.account.phone || '未填写' }}</dd>
          </div>
        </dl>
        <button class="secondary-button" type="button" :disabled="isSubmitting" @click="signOut">
          {{ isSubmitting ? '处理中…' : '退出登录' }}
        </button>
      </div>

      <template v-else>
        <div class="auth-tabs" role="group" aria-label="账户操作">
          <button
            type="button"
            :aria-pressed="formMode === 'login'"
            :disabled="isSubmitting"
            :class="{ active: formMode === 'login' }"
            @click="selectMode('login')"
          >
            登录
          </button>
          <button
            type="button"
            :aria-pressed="formMode === 'register'"
            :disabled="isSubmitting"
            :class="{ active: formMode === 'register' }"
            @click="selectMode('register')"
          >
            注册
          </button>
        </div>

        <div v-if="formMode === 'register'" class="role-tabs" role="group" aria-label="注册身份">
          <button
            type="button"
            :aria-pressed="registerRole === 'user'"
            :disabled="isSubmitting"
            :class="{ active: registerRole === 'user' }"
            @click="selectRegisterRole('user')"
          >
            车主账户
          </button>
          <button
            type="button"
            :aria-pressed="registerRole === 'store'"
            :disabled="isSubmitting"
            :class="{ active: registerRole === 'store' }"
            @click="selectRegisterRole('store')"
          >
            加盟店
          </button>
        </div>

        <form ref="formElement" class="auth-form" novalidate :aria-busy="isSubmitting" @submit.prevent="submit">
          <div class="auth-field">
            <label for="auth-username">用户名</label>
            <input
              id="auth-username"
              v-model.trim="username"
              required
              :minlength="formMode === 'register' ? 3 : undefined"
              maxlength="50"
              autocomplete="username"
              :disabled="isSubmitting"
              :placeholder="formMode === 'register' ? '3–50 个字符' : '请输入用户名'"
              :aria-invalid="Boolean(visibleErrors.username)"
              :aria-describedby="visibleErrors.username ? 'auth-username-error' : undefined"
              @input="touchField('username')"
              @blur="touchField('username')"
            />
            <p id="auth-username-error" class="auth-field-error" aria-live="polite">{{ visibleErrors.username }}</p>
          </div>

          <div class="auth-field">
            <label for="auth-password">密码</label>
            <input
              id="auth-password"
              v-model="password"
              required
              :minlength="formMode === 'register' ? 8 : undefined"
              :maxlength="formMode === 'register' ? 72 : undefined"
              type="password"
              :autocomplete="formMode === 'register' ? 'new-password' : 'current-password'"
              :disabled="isSubmitting"
              :placeholder="formMode === 'register' ? '至少 8 个字符' : '请输入密码'"
              :aria-invalid="Boolean(visibleErrors.password)"
              :aria-describedby="visibleErrors.password ? 'auth-password-error' : undefined"
              @input="touchField('password')"
              @blur="touchField('password')"
            />
            <p id="auth-password-error" class="auth-field-error" aria-live="polite">{{ visibleErrors.password }}</p>
          </div>

          <div v-if="formMode === 'register'" class="auth-field">
            <label for="auth-phone">{{ registerRole === 'store' ? '联系电话' : '手机号' }}</label>
            <input
              id="auth-phone"
              v-model.trim="phone"
              required
              maxlength="30"
              type="tel"
              autocomplete="tel"
              :disabled="isSubmitting"
              placeholder="请输入联系电话"
              :aria-invalid="Boolean(visibleErrors.phone)"
              :aria-describedby="visibleErrors.phone ? 'auth-phone-error' : undefined"
              @input="touchField('phone')"
              @blur="touchField('phone')"
            />
            <p id="auth-phone-error" class="auth-field-error" aria-live="polite">{{ visibleErrors.phone }}</p>
          </div>

          <template v-if="formMode === 'register' && registerRole === 'store'">
            <div class="auth-field">
              <label for="auth-store-name">店面名称</label>
              <input
                id="auth-store-name"
                v-model.trim="storeName"
                required
                maxlength="100"
                autocomplete="organization"
                :disabled="isSubmitting"
                placeholder="例如：明珠汽车保养店"
                :aria-invalid="Boolean(visibleErrors.storeName)"
                :aria-describedby="visibleErrors.storeName ? 'auth-store-name-error' : undefined"
                @input="touchField('storeName')"
                @blur="touchField('storeName')"
              />
              <p id="auth-store-name-error" class="auth-field-error" aria-live="polite">
                {{ visibleErrors.storeName }}
              </p>
            </div>
            <div class="auth-field">
              <label for="auth-contact-name">联系人</label>
              <input
                id="auth-contact-name"
                v-model.trim="contactName"
                required
                maxlength="50"
                autocomplete="name"
                :disabled="isSubmitting"
                placeholder="请输入联系人姓名"
                :aria-invalid="Boolean(visibleErrors.contactName)"
                :aria-describedby="visibleErrors.contactName ? 'auth-contact-name-error' : undefined"
                @input="touchField('contactName')"
                @blur="touchField('contactName')"
              />
              <p id="auth-contact-name-error" class="auth-field-error" aria-live="polite">
                {{ visibleErrors.contactName }}
              </p>
            </div>
            <div class="auth-field">
              <label for="auth-address">店面地址</label>
              <input
                id="auth-address"
                v-model.trim="address"
                required
                maxlength="255"
                autocomplete="street-address"
                :disabled="isSubmitting"
                placeholder="请输入店面地址"
                :aria-invalid="Boolean(visibleErrors.address)"
                :aria-describedby="visibleErrors.address ? 'auth-address-error' : undefined"
                @input="touchField('address')"
                @blur="touchField('address')"
              />
              <p id="auth-address-error" class="auth-field-error" aria-live="polite">{{ visibleErrors.address }}</p>
            </div>
          </template>

          <button class="auth-submit" type="submit" :disabled="isSubmitting || !isFormValid">
            {{
              isSubmitting
                ? '提交中…'
                : formMode === 'login'
                  ? '登录账户'
                  : registerRole === 'user'
                    ? '注册车主账户'
                    : '提交加盟店申请'
            }}
          </button>
        </form>
      </template>

      <p v-if="feedback" class="feedback error" role="alert">
        {{ feedback.message }}
      </p>
    </div>
  </section>
</template>
