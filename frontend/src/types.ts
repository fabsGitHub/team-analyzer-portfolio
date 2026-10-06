export type TeamId = string

export interface SurveyQuestion {
  id: string
  idx: number
  text: string
}
export interface SurveyDto {
  id: string
  title: string
  questions: SurveyQuestion[]
}
export interface SubmitSurveyRequest {
  token: string
  q1: number
  q2: number
  q3: number
  q4: number
  q5: number
}
export interface CreateSurveyRequest {
  teamId: string
  title: string
  createdBy: string
  questions: [string, string, string, string, string]
}
export interface TeamLite {
  id: string
  name: string
}
export interface TeamMemberDto {
  userId: string
  leader: boolean
}
export interface TeamAdminDto extends TeamLite {
  members: TeamMemberDto[]
}

// falls noch nicht vorhanden:
export interface Toast {
  id: string
  text: string
  type?: 'success' | 'danger' | 'info' | 'warning'
}

export type Role = 'USER' | 'ADMIN' | 'LEADER'

export const Roles = {
  ADMIN: 'ADMIN',
  LEADER: 'LEADER',
  USER: 'USER',
} as const

export type SingleSurveyResultDto = {
  q1: number
  q2: number
  q3: number
  q4: number
  q5: number
}
export type SurveyResultsDto = {
  a1: number
  a2: number
  a3: number
  a4: number
  a5: number
  n: number
  items: SingleSurveyResultDto[]
}

export type MyOpenToken = {
  tokenId: string
  surveyId: string
  surveyTitle: string
  issuedAt: string
}
