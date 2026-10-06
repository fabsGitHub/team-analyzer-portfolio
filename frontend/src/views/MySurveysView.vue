<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useTeamsStore, useSurveysStore } from '@/store'
import * as SurveysApi from '@/api/surveys.api'
import type { TeamLite } from '@/types'

const { t } = useI18n()
const teamsStore = useTeamsStore()
const surveysStore = useSurveysStore()

const teamId = ref<string>('')
const myTeams = ref<TeamLite[]>([])

onMounted(async () => {
    myTeams.value = await teamsStore.loadMyTeams()
    teamId.value = myTeams.value[0]?.id ?? ''
    await surveysStore.listMySurveys()
})

async function downloadJson(id: string) {
    const data = await SurveysApi.getSurveyResults(id)
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `survey-${id}-results.json`
    document.body.appendChild(a); a.click(); a.remove()
    URL.revokeObjectURL(url)
}
</script>

<template>
    <section class="admin-page">
        <article class="card stack" style="--space: var(--s-4)">
            <header class="cluster between center">
                <h2 class="h2">{{ t('surveys.mine.title') }}</h2>
                <span class="meta">{{ t('surveys.mine.entries', { n: surveysStore.state.mySurveys.length }) }}</span>
            </header>

            <div v-if="surveysStore.state.mySurveys.length" class="table-wrap">
                <table class="table">
                    <thead>
                        <tr>
                            <th>{{ t('surveys.create.fields.title') }}</th>
                            <th>{{ t('form.team') }}</th>
                            <th class="w-0">{{ t('admin.teams.actions') }}</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="s in surveysStore.state.mySurveys" :key="s.id">
                            <td>{{ s.title }}</td>
                            <td>{{ s.teamName || t('surveys.mine.teamFallback') }}</td>
                            <td class="w-0">
                                <div class="cluster wrap">
                                    <RouterLink class="btn" :to="`/surveys/${s.id}`">{{
                                        t('surveys.mine.actions.details') }}</RouterLink>
                                    <RouterLink class="btn" :to="`/surveys/${s.id}/results`">{{
                                        t('surveys.mine.actions.results') }}</RouterLink>
                                    <button class="btn" @click="downloadJson(s.id)">{{
                                        t('surveys.mine.actions.jsonExport') }}</button>
                                </div>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <p v-else class="empty">Noch keine Surveys vorhanden.</p>
        </article>
    </section>
</template>
