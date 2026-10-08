import { http } from './client'

export interface DemoSession {
  accessToken: string
  surveyId: string
}

export async function createDemoSession(): Promise<DemoSession> {
  const { data } = await http.post<DemoSession>(
    '/demo/session',
    null,
    { skipAuthHeader: true, retry: 0, timeoutMs: 15000 },
  )
  return data
}
