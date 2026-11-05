<template>
  <section class="admin-page">
    <article v-if="checkingSession" class="card auth-session" role="status" aria-live="polite">
      <span class="spinner" aria-hidden="true"></span>
      <div>
        <h1 class="h1">{{ t('auth.connecting') }}</h1>
        <p>{{ sessionSlow ? t('auth.slowRequest') : t('auth.firstRequest') }}</p>
      </div>
    </article>

    <article v-else class="card auth stack" style="--space: var(--s-4)">
      <header class="cluster between center">
        <h1 class="h1">{{ t('auth.title') }}</h1>
      </header>

      <p v-if="serviceUnavailable" class="note err" role="alert">{{ t('auth.serverUnavailable') }}</p>

      <form @submit.prevent="onSignIn" class="stack" style="--space: var(--s-3)" aria-describedby="auth-help">
        <div class="stack" style="--space: var(--s-1)">
          <label class="label" :for="idEmail">{{ t('auth.email') }}</label>
          <input :id="idEmail" class="input" v-model="email" type="email" required autocomplete="email" />
        </div>

        <div class="stack" style="--space: var(--s-1)">
          <label class="label" :for="idPwd">{{ t('auth.password') }}</label>
          <input :id="idPwd" class="input" v-model="password" type="password" required autocomplete="current-password"
            minlength="10" />
        </div>

        <div class="cluster between wrap">
          <div id="auth-help" class="meta" role="status">{{ busy ? t('auth.requestInProgress') : '' }}</div>
          <div class="cluster wrap">
            <button class="btn primary" :disabled="busy">{{ busy ? t('auth.signingin') : t('auth.signin') }}</button>
            <button class="btn" type="button" :disabled="busy" @click="onSignUp">{{ t('auth.signup') }}</button>
            <button class="btn ghost" type="button" :disabled="busy" @click="onReset">{{ t('auth.reset') }}</button>
          </div>
        </div>

        <p v-if="msg" class="note ok" role="status">{{ msg }}</p>
        <p v-if="err" class="note err" role="alert">{{ err }}</p>
      </form>
    </article>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/store'
import { ClientUtils } from '@/api/client'
import * as AuthApi from '@/api/auth.api'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const { login, register, resetPassword } = auth

const idEmail = 'email-input'
const idPwd = 'password-input'
const email = ref('')
const password = ref('')
const busy = ref(false)
const checkingSession = ref(true)
const sessionSlow = ref(false)
const serviceUnavailable = ref(false)
const err = ref('')
const msg = ref('')
let slowTimer: ReturnType<typeof setTimeout> | undefined

function destination(fallback = '/tutorial') {
  const redirect = route.query.redirect
  return typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')
    ? redirect
    : fallback
}

function isServiceFailure(error: any) {
  const status = error?.response?.status
  return status >= 500 || !error?.response || error?.code === 'ECONNABORTED' || error?.code === 'ERR_CANCELED'
}

function showRequestError(error: any) {
  return isServiceFailure(error)
    ? t('auth.serverUnavailable')
    : error?.response?.data?.detail || error?.response?.data?.message || error?.message || String(error)
}

function validateEmail() {
  const value = email.value.trim()
  if (!value) {
    err.value = t('auth.enter_email')
    return false
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
    err.value = t('auth.invalidEmail')
    return false
  }
  return true
}

async function onSignIn() {
  err.value = ''
  msg.value = ''
  serviceUnavailable.value = false
  if (!validateEmail()) return
  if (!password.value) {
    err.value = t('auth.enter_password')
    return
  }
  busy.value = true
  try {
    await login(email.value.trim().toLowerCase(), password.value)
    await router.push(destination())
  } catch (error: any) {
    err.value = error?.response?.status === 401
      ? t('auth.invalidCredentials')
      : showRequestError(error)
  } finally {
    busy.value = false
  }
}

async function onSignUp() {
  err.value = ''
  msg.value = ''
  serviceUnavailable.value = false
  if (!validateEmail()) return
  if (password.value.length < 10) {
    err.value = t('auth.passwordTooShort')
    return
  }
  busy.value = true
  try {
    await register(email.value.trim().toLowerCase(), password.value)
    msg.value = t('auth.check_inbox')
  } catch (error: any) {
    err.value = error?.response?.status === 409
      ? t('auth.exists')
      : showRequestError(error)
  } finally {
    busy.value = false
  }
}

async function onReset() {
  err.value = ''
  msg.value = ''
  serviceUnavailable.value = false
  if (!validateEmail()) return
  busy.value = true
  try {
    await resetPassword(email.value.trim().toLowerCase())
    msg.value = t('auth.reset_sent')
  } catch (error: any) {
    err.value = showRequestError(error)
  } finally {
    busy.value = false
  }
}

onMounted(async () => {
  const token = typeof route.query.token === 'string' ? route.query.token : ''
  if (token) {
    // Confirmation links belong to the /verify page, which also asks for the password.
    await router.replace({ path: '/verify', query: { token } })
    return
  }

  if (route.query.verified === '1') msg.value = t('auth.verified_ok')
  slowTimer = setTimeout(() => {
    if (checkingSession.value) sessionSlow.value = true
  }, 2_500)

  try {
    const hasSession = await ClientUtils.prewarmSession()
    if (hasSession) {
      try {
        const user = await AuthApi.me()
        auth.setUser(user)
        await router.replace(destination('/my/tokens'))
        return
      } catch (error: any) {
        if (isServiceFailure(error)) serviceUnavailable.value = true
      }
    } else if (isServiceFailure(ClientUtils.lastPrewarmSessionError())) {
      serviceUnavailable.value = true
    }
  } finally {
    if (slowTimer) clearTimeout(slowTimer)
    checkingSession.value = false
  }
})

onBeforeUnmount(() => {
  if (slowTimer) clearTimeout(slowTimer)
})
</script>

<style scoped>
:root,
.admin-page {
  --s-1: .25rem;
  --s-2: .5rem;
  --s-3: .75rem;
  --s-4: 1rem;
  --s-5: 1.25rem;
  --s-6: 1.5rem
}

.h1 {
  margin: 0;
  font-size: 1.375rem
}

.meta {
  color: #6b7280;
  font-size: .9rem;
  min-height: 1.2em;
}

.stack>*+* {
  margin-top: var(--space, var(--s-4))
}

.cluster {
  display: flex;
  gap: var(--s-2)
}

.cluster.between {
  justify-content: space-between
}

.cluster.wrap {
  flex-wrap: wrap
}

.admin-page {
  display: grid;
  gap: var(--s-6)
}

.card {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: var(--s-6);
  background: #fff
}

.auth {
  max-width: 480px;
  margin-inline: auto
}

.auth-session {
  max-width: 520px;
  margin-inline: auto;
  display: flex;
  align-items: flex-start;
  gap: 1rem;
}

.auth-session h1 {
  margin: 0 0 .4rem;
  font-size: 1.2rem;
}

.auth-session p {
  margin: 0;
  color: #5c6470;
  line-height: 1.55;
}

.spinner {
  width: 1.4rem;
  height: 1.4rem;
  flex: 0 0 auto;
  border: 3px solid #e7e9ed;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin .8s linear infinite;
  margin-top: .1rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.input {
  width: 100%;
  padding: .5rem .65rem;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  background: #fff
}

.label {
  font-weight: 600
}

.btn {
  padding: .45rem .8rem;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  background: #f9fafb
}

.btn.primary {
  background: #2563eb;
  border-color: #2563eb;
  color: #fff
}

.btn.ghost {
  background: transparent
}

.note {
  margin: 0;
  color: #374151
}

.note.ok {
  color: #065f46
}

.note.err {
  color: #b00020
}
</style>
