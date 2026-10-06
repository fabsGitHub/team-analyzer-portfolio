// src/store/surveys.store.ts
import { reactive } from 'vue'
import * as SurveysApi from '@/api/surveys.api'
import { abortGroup } from '@/api/client'
import type { SurveyDto, SurveyResultsDto, CreateSurveyRequest } from '@/types'
import { useAuthStore } from './auth.store'
import { i18n } from '@/i18n'

const $t = (k: string) => (i18n.global as any).t(k) as string

// latest-wins helper
function uuid() {
  return crypto.randomUUID?.() ?? Math.random().toString(36).slice(2)
}
const latestRun: Record<string, string> = {}
function newRun(key: string) {
  const id = uuid()
  latestRun[key] = id
  return id
}
function isStale(key: string, id: string) {
  return latestRun[key] !== id
}
function isCanceledError(e: any) {
  const msg = String(e?.message || '').toLowerCase()
  return (
    e?.code === 'ERR_CANCELED' ||
    msg.includes('abort') ||
    msg.includes('canceled')
  )
}

export interface SurveysState {
  currentSurvey: SurveyDto | null
  surveyLoading: boolean
  surveyError: string
  lastSubmittedSurveyId: string

  results: SurveyResultsDto | null
  resultsLoading: boolean
  resultsError: string

  lastCreatedSurveyId: string
  issuedTokens: string[]

  mySurveys: (SurveyDto & { teamName?: string })[]
}

const state: SurveysState = reactive({
  currentSurvey: null,
  surveyLoading: false,
  surveyError: '',
  lastSubmittedSurveyId: '',

  results: null,
  resultsLoading: false,
  resultsError: '',

  lastCreatedSurveyId: '',
  issuedTokens: [],

  mySurveys: [],
})

export function useSurveysStore() {
  const auth = useAuthStore()

  async function loadSurvey(surveyId: string) {
    const runId = newRun('survey')
    abortGroup('survey')
    state.currentSurvey = null
    state.surveyLoading = true
    state.surveyError = ''
    try {
      const dto = await SurveysApi.getSurvey(surveyId)
      if (isStale('survey', runId)) return
      state.currentSurvey = dto
    } catch (e: any) {
      if (isCanceledError(e) || isStale('survey', runId)) return
      state.surveyError = e?.message || String(e)
      throw e
    } finally {
      if (!isStale('survey', runId)) state.surveyLoading = false
    }
  }

  async function submitSurvey(
    surveyId: string,
    body: {
      token: string
      q1: number
      q2: number
      q3: number
      q4: number
      q5: number
    },
  ) {
    await SurveysApi.submitSurveyResponses(surveyId, body)
    state.lastSubmittedSurveyId = surveyId
    auth.toast($t('survey.thanks') || 'Danke für die Teilnahme!')
  }

  async function loadSurveyResults(surveyId: string) {
    const runId = newRun('results')
    state.resultsLoading = true
    state.resultsError = ''
    try {
      const r = await SurveysApi.getSurveyResults(surveyId)
      if (isStale('results', runId)) return state.results
      state.results = r
      return r
    } catch (e: any) {
      if (isCanceledError(e) || isStale('results', runId)) return state.results
      state.resultsError = e?.message || String(e)
      throw e
    } finally {
      if (!isStale('results', runId)) state.resultsLoading = false
    }
  }

  async function createSurvey(payload: CreateSurveyRequest) {
    const dto = await SurveysApi.createSurvey(payload)
    state.lastCreatedSurveyId = dto.id
    auth.toast($t('survey.created') || 'Survey erstellt.', 'success')
    return dto
  }

  async function issueTokens(surveyId: string, count: number) {
    state.issuedTokens = await SurveysApi.issueSurveyTokens(surveyId, count)
    return state.issuedTokens
  }

  function inviteLinkFor(token: string) {
    return SurveysApi.buildSurveyInviteLink(state.lastCreatedSurveyId, token)
  }

  async function listMySurveys() {
    state.mySurveys = await SurveysApi.listMySurveys()
    return state.mySurveys
  }

  return {
    state,
    loadSurvey,
    submitSurvey,
    loadSurveyResults,
    createSurvey,
    issueTokens,
    inviteLinkFor,
    listMySurveys,
  }
}
export type SurveysStore = ReturnType<typeof useSurveysStore>
