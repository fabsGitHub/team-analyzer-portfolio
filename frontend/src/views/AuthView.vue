<template>
  <section class="admin-page">
    <article class="card auth stack" style="--space: var(--s-4)">
      <header class="cluster between center">
        <h1 class="h1">{{ t('auth.title') }}</h1>
      </header>

      <form @submit.prevent="onSignIn" class="stack" style="--space: var(--s-3)" aria-describedby="auth-help">
        <div class="stack" style="--space: var(--s-1)">
          <label class="label" :for="idEmail">{{ t('auth.email') }}</label>
          <input :id="idEmail" class="input" v-model="email" required autocomplete="email" />
        </div>

        <div class="stack" style="--space: var(--s-1)">
          <label class="label" :for="idPwd">{{ t('auth.password') }}</label>
          <input :id="idPwd" class="input" v-model="password" type="password" required autocomplete="current-password"
            minlength="10" />
        </div>

        <div class="cluster between wrap">
          <div id="auth-help" class="meta"></div>
          <div class="cluster wrap">
            <button class="btn primary" :disabled="busy">{{ busy ? t('auth.signingin') : t('auth.signin') }}</button>
            <button class="btn" type="button" :disabled="busy" @click="onSignUp">{{ t('auth.signup') }}</button>
            <button class="btn ghost" type="button" :disabled="busy" @click="onReset">{{ t('auth.reset') }}</button>
          </div>
        </div>

        <p v-if="msg" class="note ok">{{ msg }}</p>
        <p v-if="err" class="note err">{{ err }}</p>
      </form>
    </article>
  </section>
</template>

<script setup lang="ts">
import { useAuthStore } from "@/store"
import { ref, onMounted } from "vue"
import { useI18n } from "vue-i18n"
import { useRouter, useRoute } from "vue-router"

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const { login, register, resetPassword } = useAuthStore()

const idEmail = "email-input"
const idPwd = "password-input"
const email = ref("")
const password = ref("")
const busy = ref(false)
const err = ref("")
const msg = ref("")

onMounted(async () => {
  const token = (route.query.token as string | undefined) ?? ""
  if (token) {
    busy.value = true
    try {
      const res = await fetch("/api/auth/verify?token=" + encodeURIComponent(token), { method: "GET", credentials: "include" })
      if (res.status === 204 || res.ok) {
        msg.value = t("auth.verified_ok") || "E-Mail bestätigt – bitte anmelden."
        router.replace({ path: "/auth", query: { verified: "1" } })
        return
      } else {
        err.value = t("auth.verified_failed") || "Verifizierungslink ist ungültig oder abgelaufen."
      }
    } catch (e: any) {
      err.value = e?.message || String(e)
    } finally {
      busy.value = false
    }
  } else if (route.query.verified === "1") {
    msg.value = t("auth.verified_ok") || "E-Mail bestätigt – bitte anmelden."
  }
})

async function onSignIn() {
  err.value = ""; msg.value = ""; busy.value = true
  try {
    await login(email.value.trim().toLowerCase(), password.value)
    router.push("/tutorial")
  } catch (e: any) {
    err.value = e?.response?.data?.message || e?.message || String(e)
  } finally { busy.value = false }
}

async function onSignUp() {
  err.value = ""; msg.value = ""; busy.value = true
  try {
    await register(email.value.trim().toLowerCase(), password.value)
    msg.value = t("auth.check_inbox") || "Registriert. Bitte E-Mail bestätigen."
  } catch (e: any) {
    if (e?.response?.status === 409) {
      err.value = t("auth.exists") || "Diese E-Mail ist bereits registriert."
    } else {
      err.value = e?.response?.data?.message || e?.message || String(e)
    }
  } finally { busy.value = false }
}

async function onReset() {
  err.value = ""; msg.value = ""
  if (!email.value) { err.value = t("auth.enter_email") || "Bitte E-Mail-Adresse eingeben."; return }
  busy.value = true
  try {
    await resetPassword(email.value.trim().toLowerCase())
    msg.value = t("auth.reset_sent") || "Falls registriert, wurde eine E-Mail gesendet."
  } catch (e: any) {
    err.value = e?.response?.data?.message || e?.message || String(e)
  } finally { busy.value = false }
}
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
  font-size: .9rem
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
