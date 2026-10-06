<template>
    <section class="admin-page">
        <!-- Erstellen -->
        <article class="card stack" style="--space: var(--s-6)">
            <header class="cluster between center">
                <h1 class="h1">{{ t('surveys.create.title') }}</h1>
            </header>

            <form class="stack" style="--space: var(--s-4)" @submit.prevent="create">
                <h2 class="h2">{{ t('surveys.create.meta') }}</h2>

                <!-- Titel + Team + Anlegen -->
                <div class="grid">
                    <input v-model="title" class="input" :placeholder="t('surveys.create.fields.title')" required
                        :aria-label="t('surveys.create.fields.title')" />
                    <select v-model="teamId" class="input select" required
                        :aria-label="t('surveys.create.fields.team')">
                        <option v-for="t in myTeams" :key="t.id" :value="t.id">
                            {{ t.name }}
                        </option>
                    </select>
                    <button class="btn primary" type="submit" :aria-label="t('surveys.create.fields.submit')">
                        {{ t('surveys.create.fields.submit') }}
                    </button>
                </div>

                <!-- Fragen -->
                <div class="stack" style="--space: var(--s-3)">
                    <h2 class="h2">{{ t('surveys.create.questions.title') }}</h2>
                    <div class="stack" style="--space: var(--s-2)">
                        <label v-for="i in 5" :key="i" class="cluster center" style="gap: var(--s-3)">
                            <span class="meta w-0" style="min-width: 3ch;">{{ i }}.</span>
                            <input v-model="questions[i - 1]" class="input"
                                :placeholder="t('surveys.create.questions.placeholder', { i })" required
                                :aria-label="t('surveys.create.questions.placeholder', { i })" />
                        </label>
                    </div>
                </div>
            </form>

            <p v-if="error" class="error">{{ error }}</p>
        </article>
    </section>
</template>

<script setup lang="ts">
import { onMounted, ref, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore, useTeamsStore, useSurveysStore } from '@/store'
import type { TeamLite } from '@/types'

const { t } = useI18n()
const auth = useAuthStore()
const teams = useTeamsStore()
const surveys = useSurveysStore()

const title = ref('')
const teamId = ref<string>('')
const myTeams = ref<TeamLite[]>([])
const questions = ref<string[]>(['', '', '', '', ''])
const surveyId = ref<string>('')
const error = ref<string | null>(null)

onMounted(async () => {
    try {
        myTeams.value = await teams.loadMyTeams()
        await nextTick()
        teamId.value = myTeams.value[0]?.id ?? ''
    } catch (e: any) {
        error.value = e?.response?.data?.message || e?.message || 'Failed to load teams'
    }
})

async function create() {
    const createdBy = auth.state.user?.id || 'unknown-user'
    const dto = await surveys.createSurvey({
        teamId: teamId.value,
        title: title.value.trim(),
        createdBy,
        questions: [
            questions.value[0].trim(),
            questions.value[1].trim(),
            questions.value[2].trim(),
            questions.value[3].trim(),
            questions.value[4].trim(),
        ],
    })
    surveyId.value = dto.id
    await issueTeam()
    title.value = ''
    questions.value = ['', '', '', '', '']
}

async function issueTeam() {
    if (!surveyId.value) return
    try {
        const { created } = await import('@/api/surveys.api').then(m => m.ensureTokensForTeam(surveyId.value))
        alert(t('surveys.create.issuedTeamTokensToast', { n: created }))
    } catch (e: any) {
        error.value = e?.response?.data?.message || e?.message || 'Token issue failed'
    }
}
</script>


<style scoped>
/* -------- Spacing- & Typo-Scale (4-px Grid) -------- */
:root,
.admin-page {
    --s-1: 0.25rem;
    --s-2: 0.5rem;
    --s-3: 0.75rem;
    --s-4: 1rem;
    --s-5: 1.25rem;
    --s-6: 1.5rem;
    --s-8: 2rem;
}

.h1 {
    font-size: 1.375rem;
    line-height: 1.2;
    margin: 0;
}

.h2 {
    font-size: 1.125rem;
    line-height: 1.25;
    margin: 0;
}

.meta {
    color: #6b7280;
    font-size: .9rem;
}

/* -------- Layout-Utilities -------- */
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

.grid {
    display: grid;
    grid-template-columns: 1fr 1fr auto;
    gap: var(--s-2);
}

@media (max-width: 720px) {
    .grid {
        grid-template-columns: 1fr;
    }
}

/* -------- Cards & Basics -------- */
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

.input,
.select {
    width: 100%;
    padding: 0.5rem 0.65rem;
    border: 1px solid #d1d5db;
    border-radius: 10px;
    background: #fff;
    font-size: 1rem;
}

.btn {
    padding: 0.45rem 0.8rem;
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

.btn.primary:hover {
    filter: brightness(0.95);
}

.error {
    color: #b00020;
    background: #ffd8df;
    border: 1px solid #ffb3bf;
    padding: .6rem .75rem;
    border-radius: 8px;
}

.w-0 {
    white-space: nowrap;
}
</style>
