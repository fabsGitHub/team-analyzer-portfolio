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

export type Role = 'USER' | 'ADMIN' | 'LEADER'

export const Roles = {
  ADMIN: 'ADMIN',
  LEADER: 'LEADER',
  USER: 'USER',
} as const

/** One anonymous response: Likert value (1..5) per question id. */
export type SingleSurveyResultDto = {
  responseId: string
  answers: Record<string, number>
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
