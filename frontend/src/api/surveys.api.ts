// src/api/surveys.api.ts
import { http } from './client'
import type {
  SurveyDto,
  SubmitSurveyRequest,
  SurveyResultsDto,
  CreateSurveyRequest,
  MyOpenToken,
} from '@/types'

export async function getSurvey(id: string): Promise<SurveyDto> {
  const { data } = await http.get<SurveyDto>(`/surveys/${id}`, {
    allowAnonymous: true,
    cancelGroup: 'survey',
    retry: 2,
    timeoutMs: 8000,
  })
  return data
}

export async function submitSurveyResponses(
  id: string,
  body: SubmitSurveyRequest,
): Promise<void> {
  await http.post(`/surveys/${id}/responses`, body, {
    allowAnonymous: true,
    cancelGroup: 'survey-submit',
    timeoutMs: 10000,
  })
}

export async function getSurveyResults(id: string): Promise<SurveyResultsDto> {
  const { data } = await http.get<SurveyResultsDto>(`/surveys/${id}/results`, {
    retry: 2,
    timeoutMs: 8000,
  })
  return data
}

export async function createSurvey(
  payload: CreateSurveyRequest,
): Promise<SurveyDto> {
  const { data } = await http.post<SurveyDto>('/surveys', payload)
  return data
}

export async function issueSurveyTokens(
  id: string,
  count: number,
): Promise<string[]> {
  const { data } = await http.post<string[]>(`/surveys/${id}/tokens/batch`, {
    count,
  })
  return data
}

export async function getResultsDownloadLink(id: string): Promise<string> {
  const { data } = await http.post<{ url: string }>(
    `/surveys/${id}/download-tokens`,
    null,
    { retry: 1, timeoutMs: 8000 },
  )
  return data.url
}

export async function ensureTokensForTeam(
  surveyId: string,
): Promise<{ created: number }> {
  const { data } = await http.post<{ created: number }>(
    `/surveys/${surveyId}/tokens/for-members`,
  )
  return data
}

export async function myTokenForSurvey(
  surveyId: string,
): Promise<{ created: boolean; inviteLink?: string }> {
  const { data } = await http.put(`/surveys/${surveyId}/my-token`, null, {
    retry: 2,
    timeoutMs: 8000,
  })
  return data
}

export async function renewMyToken(
  surveyId: string,
): Promise<{ created: boolean; inviteLink: string }> {
  const { data } = await http.post(`/surveys/${surveyId}/my-token/renew`)
  return data
}

export async function listMyOpenTokens(): Promise<MyOpenToken[]> {
  const { data } = await http.get<MyOpenToken[]>('/my/tokens', {
    cancelGroup: 'my/tokens',
    retry: 2,
    timeoutMs: 8000,
  })
  return data
}

export async function listMySurveys(): Promise<
  (SurveyDto & { teamName?: string })[]
> {
  const { data } = await http.get('/me/surveys', {
    cancelGroup: 'me/surveys',
    retry: 2,
    timeoutMs: 8000,
  })
  return data as any
}

export function buildSurveyInviteLink(surveyId: string, token: string): string {
  const base = window.location.origin
  return `${base}/surveys/${surveyId}?token=${encodeURIComponent(token)}`
}
