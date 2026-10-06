<template>
    <section class="card">
        <header class="header">
            <div>
                <h1 class="title">{{ title || t('results.title') }}</h1>
                <p v-if="subtitle" class="subtitle">{{ subtitle }}</p>
                <p v-if="metaLine" class="meta">{{ metaLine }}</p>
            </div>
            <div class="actions">
                <RouterLink class="btn" to="/surveys">{{ t('results.back') }}</RouterLink>
                <button class="btn" @click="downloadJson">
                    {{ t('results.json') }}
                </button>
                <button class="btn" @click="downloadCsv" :disabled="norm.length === 0">{{ t('results.csv') }}</button>
                <button class="btn" @click="reload" :disabled="loading">{{ t('results.reload') }}</button>
            </div>
        </header>

        <div v-if="loading" class="loading">{{ t('results.loading') }}</div>
        <div v-else-if="error" class="error">
            <strong>{{ t('results.errorTitle') }}</strong> {{ error }}
            <div v-if="errorStatus === 401" class="hint">{{ t('results.reauthHint') }}</div>
        </div>
        <div v-else>
            <div v-if="norm.length === 0" class="empty">{{ t('results.empty') }}</div>
            <ul class="q-list">
                <li v-for="(q, idx) in norm" :key="idx" class="q-item">
                    <div class="q-head">
                        <h3 class="q-title">{{ q.label }}</h3>
                        <div class="q-stats">
                            <span class="badge">Ø {{ q.average.toFixed(2) }}</span>
                            <span class="muted">n={{ q.total }}</span>
                        </div>
                    </div>
                    <!-- Stacked distribution (1..5) -->
                    <div class="bar" :class="{ empty: q.total === 0 }">
                        <div v-for="(c, i) in q.counts" :key="i" v-show="c > 0" class="seg"
                            :title="t('results.starsTooltip', { n: i + 1, count: c })"
                            :style="{ flexGrow: c, background: 'var(--accent, #b66a2b)' }">
                            <span>{{ Math.round((c / q.total) * 100) }}%</span>
                        </div>
                    </div>
                    <!-- Tabular breakdown -->
                    <div class="gridtable">
                        <div class="row header">
                            <div class="cell">{{ t('results.value') }}</div>
                            <div class="cell" v-for="n in 5" :key="n">{{ n }}</div>
                            <div class="cell right">{{ t('results.total') }}</div>
                        </div>
                        <div class="row">
                            <div class="cell">{{ t('results.count') }}</div>
                            <div class="cell" v-for="(c, i) in q.counts" :key="i">{{ c }}</div>
                            <div class="cell right">{{ q.total }}</div>
                        </div>
                    </div>
                </li>
            </ul>
        </div>
    </section>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useSurveyResults } from '@/composables/useSurveyResults'

const { t } = useI18n()
const route = useRoute()
const surveyId = route.params.id as string

const { survey, norm, loading, error, errorStatus, metaLine, load, downloadJson, downloadCsv } =
    useSurveyResults(surveyId)

const title = computed(() => survey.value?.title ?? '')
const subtitle = computed(() => '')

onMounted(load)
const reload = () => load()
</script>


<style scoped>
.header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 1rem;
    margin-bottom: 1rem;
}

.title {
    margin: 0;
}

.subtitle {
    margin: .25rem 0 0;
    color: var(--muted);
}

.meta {
    margin: .25rem 0 0;
    font-size: .9em;
    color: var(--muted);
}

.actions {
    display: flex;
    flex-wrap: wrap;
    gap: .5rem;
}

.loading,
.empty {
    color: var(--muted);
    padding: .5rem 0;
}

.error {
    color: #b00020;
    background: #ffd8df;
    border: 1px solid #ffb3bf;
    padding: .75rem;
    border-radius: 8px;
}

.hint {
    margin-top: .5rem;
}

.q-list {
    list-style: none;
    padding: 0;
    margin: 0;
    display: grid;
    gap: 1rem;
}

.q-item {
    border: 1px solid var(--border);
    border-radius: 12px;
    padding: .75rem;
    background: var(--surface);
}

.q-head {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: 1rem;
}

.q-title {
    margin: 0 0 .25rem;
}

.q-stats {
    display: flex;
    align-items: center;
    gap: .5rem;
}

.badge {
    display: inline-block;
    padding: .15rem .5rem;
    border-radius: 999px;
    border: 1px solid var(--border);
}

.muted {
    color: var(--muted);
}

.bar {
    display: flex;
    gap: 2px;
    height: 26px;
    align-items: stretch;
    border-radius: 8px;
    overflow: hidden;
    background: #f1f1f1;
    margin: .5rem 0 .75rem;
}

.bar.empty {
    opacity: .6;
}

.seg {
    display: flex;
    align-items: center;
    justify-content: center;
    min-width: 2px;
    font-size: .8em;
    background: var(--accent, #d0d7ff);
}

.seg:nth-child(1) {
    filter: brightness(1.00);
}

.seg:nth-child(2) {
    filter: brightness(0.95);
}

.seg:nth-child(3) {
    filter: brightness(0.90);
}

.seg:nth-child(4) {
    filter: brightness(0.85);
}

.seg:nth-child(5) {
    filter: brightness(0.80);
}

.seg span {
    padding: 0 .25rem;
    color: #000;
    mix-blend-mode: multiply;
}

.gridtable {
    border: 1px solid var(--border);
    border-radius: 8px;
    overflow: hidden;
}

.row {
    display: grid;
    grid-template-columns: 6rem repeat(5, 1fr) 5rem;
}

.row+.row {
    border-top: 1px solid var(--border);
}

.cell {
    padding: .4rem .5rem;
    border-left: 1px solid var(--border);
}

.cell:first-child {
    border-left: none;
}

.row.header {
    background: #fafafa;
    font-weight: 600;
}

.cell.right {
    text-align: right;
}
</style>
