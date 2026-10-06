// src/api/auth.api.ts
import { http, ClientUtils } from './client'

export async function register(email: string, password: string): Promise<void> {
  await http.post(
    '/auth/register',
    { email, password },
    { skipAuthHeader: true },
  )
}

export async function login(
  email: string,
  password: string,
): Promise<{ accessToken: string }> {
  const { data } = await http.post<{ accessToken: string }>(
    '/auth/login',
    { email, password },
    { skipAuthHeader: true },
  )
  return data
}

export async function logout(): Promise<void> {
  await http.post('/auth/logout', null, { skipAuthHeader: true })
}

export async function me(): Promise<{
  email: string
  roles: string[]
  id: string
  isLeader: boolean
}> {
  await ClientUtils.prewarmSession()
  const data = await ClientUtils.fetchMeOnce()
  return data
}

/** „Soft“ /me – 401 erlaubt (kein Refresh). */
export async function meAnonymousOk(): Promise<void> {
  await ClientUtils.fetchMeOnce(true)
}

export async function resetPassword(email: string): Promise<void> {
  await http.post('/auth/reset', { email }, { skipAuthHeader: true })
}

export async function verifyEmail(token: string): Promise<void> {
  await http.post('/auth/verify', { token }, { skipAuthHeader: true })
}
