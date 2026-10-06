<template>
    <section class="admin-page">
        <article class="card auth stack" style="--space: var(--s-4)">
            <header class="cluster between center">
                <h1 class="h1">{{ t('auth.resetPassword.title') }}</h1>
            </header>

            <p v-if="!token" class="note err">
                {{ t('auth.resetPassword.tokenMissing') }}
                <router-link to="/auth">{{ t('auth.title') }}</router-link>
            </p>

            <form v-else class="stack" style="--space: var(--s-3)">
                <!-- Neues Passwort -->
                <div class="stack" style="--space: var(--s-1)">
                    <label class="label" for="pwd1">{{ t('auth.resetPassword.new') }}</label>
                    <div class="cluster">
                        <input id="pwd1" class="input" :type="show1 ? 'text' : 'password'" v-model.trim="pwd1"
                            minlength="10" required autocomplete="new-password" />
                        <button type="button" class="btn" @click="show1 = !show1" :aria-pressed="show1"
                            :title="t(show1 ? 'auth.resetPassword.hide' : 'auth.resetPassword.show')">
                            {{ t(show1 ? 'auth.resetPassword.hide' : 'auth.resetPassword.show') }}
                        </button>
                    </div>
                    <small class="meta">{{ t('auth.resetPassword.minlen', { n: 10 }) }}</small>
                </div>

                <!-- Wiederholung -->
                <div class="stack" style="--space: var(--s-1)">
                    <label class="label" for="pwd2">{{ t('auth.resetPassword.repeat') }}</label>
                    <div class="cluster">
                        <input id="pwd2" class="input" :type="show2 ? 'text' : 'password'" v-model.trim="pwd2"
                            minlength="10" required autocomplete="new-password" />
                        <button type="button" class="btn" @click="show2 = !show2" :aria-pressed="show2"
                            :title="t(show2 ? 'auth.resetPassword.hide' : 'auth.resetPassword.show')">
                            {{ t(show2 ? 'auth.resetPassword.hide' : 'auth.resetPassword.show') }}
                        </button>
                    </div>
                    <p v-if="pwdTouched && mismatch" class="note err">
                        {{ t('auth.resetPassword.err.mismatch') }}
                    </p>
                </div>

                <!-- Submit -->
                <button type="button" class="btn primary" :aria-disabled="!canSubmit || busy" :title="btnTitle"
                    @click="onTrySubmit">
                    {{ busy ? t('auth.resetPassword.saving') : t('auth.resetPassword.save') }}
                </button>

                <!-- Hinweise/Fehler -->
                <p v-if="attempted && !busy && !canSubmit && disabledReason" class="note err">
                    <strong>{{ t('auth.resetPassword.cantSubmit') }}</strong>
                    {{ ' ' + disabledReason }}
                </p>

                <p v-if="ok === true" class="note ok">{{ t('auth.resetPassword.ok') }}</p>
                <p v-if="ok === false" class="note err">{{ t('auth.resetPassword.badToken') }}</p>
                <p v-if="err" class="note err">{{ err }}</p>
            </form>
        </article>
    </section>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const token = computed(() => (route.query.token as string) || '')

const pwd1 = ref('')
const pwd2 = ref('')
const show1 = ref(false)
const show2 = ref(false)
const busy = ref(false)
const ok = ref<null | boolean>(null)
const err = ref('')
const attempted = ref(false)

const minLenOk = computed(() => pwd1.value.length >= 10 && pwd2.value.length >= 10)
const mismatch = computed(() => pwd1.value !== pwd2.value)
const canSubmit = computed(() => !!token.value && minLenOk.value && !mismatch.value)

const pwdTouched = ref(false)
watch([pwd1, pwd2], () => { pwdTouched.value = true })

const disabledReason = computed(() => {
    if (!token.value) return t('auth.resetPassword.tokenMissing')
    if (!minLenOk.value) return t('auth.resetPassword.err.minlen', { n: 10 })
    if (mismatch.value) return t('auth.resetPassword.err.mismatch')
    return ''
})

const btnTitle = computed(() => {
    if (busy.value) return t('auth.resetPassword.saving')
    if (!canSubmit.value) return disabledReason.value || t('auth.resetPassword.fix')
    return t('auth.resetPassword.save')
})

async function onTrySubmit() {
    attempted.value = true
    err.value = ''; ok.value = null

    if (busy.value) {
        err.value = t('auth.resetPassword.err.busy')
        return
    }
    if (!canSubmit.value) {
        // Zeige gezielt die passende Fehlermeldung
        err.value = disabledReason.value || t('auth.resetPassword.fix')
        return
    }
    await onSubmit()
}

async function onSubmit() {
    busy.value = true
    try {
        const res = await fetch('/api/auth/reset/confirm', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ token: token.value, newPassword: pwd1.value }),
        })
        ok.value = res.ok
        if (res.ok) {
            setTimeout(() => router.replace('/auth'), 1200)
        } else {
            err.value = (await res.text().catch(() => '')) || t('auth.resetPassword.genericErr')
        }
    } catch (e: any) {
        err.value = e?.message || String(e)
        ok.value = false
    } finally {
        busy.value = false
    }
}
</script>

<style scoped>
.btn[aria-disabled="true"] {
    opacity: .6;
    cursor: not-allowed;
    filter: grayscale(0.25);
}
</style>
