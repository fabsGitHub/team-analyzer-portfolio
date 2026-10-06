// src/composables/useSurveyResults.ts
import { ref, computed } from 'vue'
import * as Surveys from '@/api/surveys.api'
import type {
  SurveyDto,
  SurveyResultsDto,
  SingleSurveyResultDto,
} from '@/types'
import { useI18n } from 'vue-i18n'

export function useSurveyResults(surveyId: string) {
  const { t } = useI18n()
  const survey = ref<SurveyDto | null>(null)
  const results = ref<SurveyResultsDto | null>(null)
  const loading = ref(true)
  const error = ref<string | null>(null)
  const errorStatus = ref<number | null>(null)

  async function load() {
    loading.value = true
    error.value = null
    errorStatus.value = null
    try {
      const [s, r] = await Promise.all([
        Surveys.getSurvey(surveyId),
        Surveys.getSurveyResults(surveyId),
      ])
      survey.value = s
      results.value = r
    } catch (e: any) {
      error.value =
        e?.response?.data?.message || e?.message || 'Unbekannter Fehler'
      errorStatus.value = e?.response?.status ?? null
    } finally {
      loading.value = false
    }
  }

  type QNorm = {
    label: string
    counts: number[]
    average: number
    total: number
  }
  function countsForQuestion(
    items: SingleSurveyResultDto[],
    qIndex1: number,
  ): number[] {
    const counts = [0, 0, 0, 0, 0]
    const key = `q${qIndex1}` as keyof SingleSurveyResultDto
    for (const it of items) {
      const v = Number((it as any)[key])
      if (v >= 1 && v <= 5) counts[v - 1]++
    }
    return counts
  }

  const norm = computed<QNorm[]>(() => {
    if (!results.value || !survey.value) return []
    const labels =
      survey.value.questions?.map((q) => q.text) ??
      Array.from({ length: 5 }, (_, i) => `Frage ${i + 1}`)
    const items = results.value.items ?? []
    const n = results.value.n ?? items.length
    const avgs = [
      results.value.a1,
      results.value.a2,
      results.value.a3,
      results.value.a4,
      results.value.a5,
    ]
    return labels.slice(0, 5).map((label, idx) => {
      const qi = idx + 1
      const counts = countsForQuestion(items, qi)
      const total = n
      let average = avgs[idx]
      if (typeof average !== 'number' || Number.isNaN(average)) {
        const sum = counts.reduce((acc, c, i) => acc + c * (i + 1), 0)
        average = total ? sum / total : 0
      }
      return { label, counts, average, total }
    })
  })

  async function downloadJson() {
    const url = await Surveys.getResultsDownloadLink(surveyId)
    window.location.href = url
  }

  function downloadCsv() {
    const lines = [
      'Question,Average,Total,Count_1,Count_2,Count_3,Count_4,Count_5',
      ...norm.value.map(
        (q) =>
          `"${q.label.replace(/"/g, '""')}",${q.average.toFixed(3)},${q.total},${q.counts.join(',')}`,
      ),
    ]
    const blob = new Blob([lines.join('\n')], {
      type: 'text/csv;charset=utf-8',
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = (survey.value?.title || `survey-${surveyId}`) + '-results.csv'
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  }

  const metaLine = computed(() => {
    const n = results.value?.n
    return typeof n === 'number' ? t('results.participants', { n }) : ''
  })

  return {
    survey,
    results,
    norm,
    loading,
    error,
    errorStatus,
    metaLine,
    load,
    downloadJson,
    downloadCsv,
  }
}
