<template>
    <section class="admin-page">
        <article class="card stack" style="--space: var(--s-3)">
            <h1 class="h1">{{ t('verify.title') }}</h1>

            <p v-if="!token" class="meta" role="alert">{{ t('verify.status.missing') }}</p>
            <p v-else-if="status === 'ok'" class="meta" role="status">{{ t('verify.status.ok') }}</p>

            <form v-else class="stack" style="--space: var(--s-3)" @submit.prevent="onConfirm">
                <p class="meta">{{ t('verify.intro') }}</p>
                <label class="label" for="verify-password">{{ t('auth.password') }}</label>
                <input id="verify-password" v-model="password" class="input" type="password"
                    autocomplete="current-password" required />
                <button class="btn primary" type="submit" :disabled="busy || !password">
                    {{ busy ? t('verify.status.idle') : t('verify.confirm') }}
                </button>
                <p v-if="status === 'error'" class="meta err" role="alert">{{ t('verify.status.error') }}</p>
            </form>
        </article>
    </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import * as Auth from '@/api/auth.api'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()

const token = computed(() => (typeof route.query.token === 'string' ? route.query.token : ''))
const password = ref('')
const busy = ref(false)
const status = ref<'form' | 'ok' | 'error'>('form')

// Confirming needs the link (mailbox) and the password chosen at registration, so nobody can
// activate an account that someone else registered with this address.
async function onConfirm() {
    if (!token.value || !password.value || busy.value) return
    busy.value = true
    status.value = 'form'
    try {
        await Auth.verifyEmail(token.value, password.value)
        status.value = 'ok'
        setTimeout(() => router.replace({ path: '/auth', query: { verified: '1' } }), 800)
    } catch {
        status.value = 'error'
    } finally {
        busy.value = false
    }
}
</script>


<style scoped>
:root,
.admin-page {
    --s-3: .75rem;
    --s-4: 1rem;
    --s-6: 1.5rem
}

.h1 {
    margin: 0 0 .25rem;
    font-size: 1.25rem
}

.meta {
    color: #6b7280
}

.meta.err {
    color: #b91c1c
}

.label {
    font-weight: 600
}

.input {
    width: 100%;
    padding: .5rem .65rem;
    border: 1px solid #d1d5db;
    border-radius: 10px;
    background: #fff
}

.btn {
    padding: .45rem .8rem;
    border-radius: 10px;
    border: 1px solid #d1d5db;
    cursor: pointer
}

.btn.primary {
    background: #2563eb;
    border-color: #2563eb;
    color: #fff
}

.btn[disabled] {
    opacity: .6;
    cursor: not-allowed
}

.stack>*+* {
    margin-top: var(--space, var(--s-3))
}

.admin-page {
    display: grid;
    gap: var(--s-6)
}

.card {
    max-width: 600px;
    margin-inline: auto;
    border: 1px solid #e5e7eb;
    border-radius: 12px;
    padding: var(--s-6);
    background: #fff
}
</style>
