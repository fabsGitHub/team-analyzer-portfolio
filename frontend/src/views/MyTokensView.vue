<script setup lang="ts">
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useMyTokens } from '@/composables/useMyTokens'

const { t } = useI18n()
const { tokens, loading, error, busyId, load, goToSurvey } = useMyTokens()

onMounted(load)
</script>


<template>
    <section class="admin-page">
        <article class="card stack">
            <header class="cluster between center">
                <h1 class="h1">{{ t('tokens.title') }}</h1>
                <button class="btn" @click="load" :disabled="loading">
                    {{ t('form.refresh') }}
                </button>
            </header>

            <!-- Ladezustand -->
            <div v-if="loading" class="stack">
                <div class="skeleton row"></div>
                <div class="skeleton row"></div>
                <div class="skeleton row"></div>
            </div>

            <!-- Fehler -->
            <div v-else-if="error" class="alert">
                <div class="cluster between wrap center">
                    <span>{{ error }}</span>
                    <button class="btn" @click="load">{{ t('form.retry') }}</button>
                </div>
            </div>

            <!-- Inhalt -->
            <template v-else>
                <div v-if="tokens.length === 0" class="empty-state">
                    {{ t('tokens.none') }}
                </div>

                <ul v-else class="list">
                    <li v-for="tk in tokens" :key="tk.tokenId" class="row cluster between center">
                        <div class="stack">
                            <strong>{{ tk.surveyTitle }}</strong>
                            <span class="meta">
                                {{ t('tokens.issuedAt') }}
                                {{ new Date(tk.issuedAt).toLocaleString() }}
                            </span>
                        </div>

                        <button class="btn primary" @click="goToSurvey(tk)" :disabled="busyId === tk.surveyId"
                            :aria-busy="busyId === tk.surveyId">
                            {{ busyId === tk.surveyId ? t('form.opening') : t('tokens.openSurvey') }}
                        </button>
                    </li>
                </ul>
            </template>
        </article>
    </section>
</template>

<style scoped>
/* Re-Use deiner Utilities */
:root,
.admin-page {
    --s-2: .5rem;
    --s-3: .75rem;
    --s-4: 1rem;
    --s-5: 1.25rem;
    --s-6: 1.5rem;
}

.h1 {
    margin: 0;
    font-size: 1.375rem;
    line-height: 1.2;
}

.stack>*+* {
    margin-top: var(--space, var(--s-4));
}

.cluster {
    display: flex;
    gap: var(--s-2);
}

.cluster.center {
    align-items: center;
}

.cluster.between {
    justify-content: space-between;
}

.cluster.wrap {
    flex-wrap: wrap;
}

.admin-page {
    display: grid;
    gap: var(--s-6);
}

.card {
    border: 1px solid #e5e7eb;
    border-radius: 12px;
    padding: var(--s-6);
    background: #fff;
}

/* Liste + Rows */
.list {
    list-style: none;
    padding: 0;
    margin: 0;
}

.row {
    padding: .75rem;
    border: 1px solid #f1f5f9;
    border-radius: 10px;
}

.row+.row {
    margin-top: var(--s-3);
}

.meta {
    color: #6b7280;
    font-size: .9rem;
}

/* Buttons */
.btn {
    padding: .45rem .8rem;
    border-radius: 10px;
    border: 1px solid #d1d5db;
    background: #f9fafb;
    cursor: pointer;
}

.btn:hover {
    background: #f3f4f6;
}

.btn.primary {
    background: #2563eb;
    border-color: #2563eb;
    color: #fff;
}

.btn.primary[disabled] {
    opacity: .7;
    cursor: not-allowed;
}

/* States */
.empty-state {
    text-align: center;
    color: #6b7280;
    padding: var(--s-6);
    border: 1px dashed #e5e7eb;
    border-radius: 12px;
}

.alert {
    border: 1px solid #fde68a;
    background: #fffbeb;
    color: #92400e;
    padding: .75rem;
    border-radius: 10px;
}

/* Skeletons */
.skeleton {
    background: linear-gradient(90deg, #f3f4f6 25%, #e5e7eb 37%, #f3f4f6 63%);
    background-size: 400% 100%;
    animation: shimmer 1.2s infinite;
    border-radius: 10px;
}

.skeleton.row {
    height: 56px;
}

@keyframes shimmer {
    0% {
        background-position: 100% 0;
    }

    100% {
        background-position: 0 0;
    }
}
</style>
