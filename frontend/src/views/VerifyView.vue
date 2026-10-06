<template>
    <section class="admin-page">
        <article class="card stack" style="--space: var(--s-3)">
            <h1 class="h1">{{ t('verify.title') }}</h1>
            <p v-if="status === 'idle'" class="meta">{{ t('verify.status.idle') }}</p>
            <p v-else-if="status === 'missing'" class="meta">{{ t('verify.status.missing') }}</p>
            <p v-else-if="status === 'error'" class="meta">{{ t('verify.status.error') }}</p>
            <p v-else class="meta">{{ t('verify.status.ok') }}</p>
        </article>
    </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import * as Auth from '@/api/auth.api'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const status = ref<'idle' | 'ok' | 'missing' | 'error'>('idle')

onMounted(async () => {
    const token = (route.query.token as string | undefined) ?? ''
    if (!token) { status.value = 'missing'; return }
    try {
        await Auth.verifyEmail(token)
        status.value = 'ok'
        setTimeout(() => router.replace({ path: '/auth', query: { verified: '1' } }), 800)

    } catch {
        status.value = 'error'
    }
})
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
