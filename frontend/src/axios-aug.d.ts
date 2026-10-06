import 'axios'

declare module 'axios' {
  interface AxiosRequestConfig {
    /** Requests derselben Gruppe: vorherigen abbrechen (z.B. "survey") */
    cancelGroup?: string
    /** Anzahl Auto-Retries bei *transienten* Fehlern (GET) */
    retry?: number
    /** Per-Request Timeout (ms) */
    timeoutMs?: number
    /** 401 einfach durchreichen, keinen Refresh anstoßen */
    allowAnonymous?: boolean
    /** Für diesen Request keinen Authorization-Bearer anhängen */
    skipAuthHeader?: boolean
  }
}
