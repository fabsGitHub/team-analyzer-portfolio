// src/api/client.ts
import axios, {
  AxiosError,
  type AxiosRequestConfig,
  type AxiosResponse,
  type GenericAbortSignal,
  type InternalAxiosRequestConfig,
} from 'axios'

/** Axios-Config erweitern (Flags + interne Felder) */
declare module 'axios' {
  export interface AxiosRequestConfig<D = any> {
    allowAnonymous?: boolean
    skipAuthHeader?: boolean
    cancelGroup?: string
    retry?: number
    timeoutMs?: number
    _retry?: boolean
    _retryCount?: number
    _abortController?: AbortController
    _timeoutId?: number
  }
  export interface InternalAxiosRequestConfig<D = any>
    extends AxiosRequestConfig<D> {
    allowAnonymous?: boolean
    skipAuthHeader?: boolean
    cancelGroup?: string
    retry?: number
    timeoutMs?: number
    _retry?: boolean
    _retryCount?: number
    _abortController?: AbortController
    _timeoutId?: number
    signal?: GenericAbortSignal
  }
}

// ───────────────────────────────────────────────────────────────────────────────
// Konstanten
// ───────────────────────────────────────────────────────────────────────────────
const API_BASE = (import.meta.env.VITE_API_BASE as string) ?? '/api'
const BACKOFF_BASE_MS = 150
const BACKOFF_CAP_MS = 2000

// ───────────────────────────────────────────────────────────────────────────────
// Cancel-Gruppen
// ───────────────────────────────────────────────────────────────────────────────
const inflightByGroup = new Map<string, AbortController>()
export function abortGroup(group: string) {
  inflightByGroup.get(group)?.abort('manual abort')
  inflightByGroup.delete(group)
}

// ───────────────────────────────────────────────────────────────────────────────
// Utils
// ───────────────────────────────────────────────────────────────────────────────
function isTransientStatus(s?: number) {
  return s === 0 || s === 502 || s === 503 || s === 504
}
function backoffWithFixedJitter(
  baseMs: number,
  attempt: number,
  capMs = BACKOFF_CAP_MS,
  jitterMs = 100,
) {
  const raw = baseMs * Math.pow(2, attempt)
  const delay = Math.min(raw, capMs)
  return delay + Math.floor(Math.random() * jitterMs)
}
function chainAbortSignals(
  primary: AbortController,
  external?: GenericAbortSignal,
) {
  if (!external) return
  const sig: any = external
  if (sig.aborted) {
    primary.abort(sig.reason ?? 'upstream abort')
    return
  }
  if (typeof sig.addEventListener === 'function') {
    const onAbort = () => primary.abort(sig.reason ?? 'upstream abort')
    sig.addEventListener('abort', onAbort, { once: true })
  }
}
function parseUrl(url?: string | null): URL | null {
  if (!url) return null
  try {
    return new URL(
      url,
      typeof window !== 'undefined'
        ? window.location.origin
        : 'http://localhost',
    )
  } catch {
    return null
  }
}
const AUTH_PATHS = new Set([
  '/auth/login',
  '/auth/register',
  '/auth/refresh',
  '/auth/logout',
])
function isAuthEndpoint(url?: string | null): boolean {
  const u = parseUrl(url)
  return !!u && AUTH_PATHS.has(u.pathname)
}
function isAdminPath(url?: string | null): boolean {
  const u = parseUrl(url)
  return !!u && u.pathname.startsWith('/admin/')
}

// ───────────────────────────────────────────────────────────────────────────────
// Token-Wiring (vom Store injiziert)
// ───────────────────────────────────────────────────────────────────────────────
let getToken: () => string = () => ''
let setToken: (t: string) => void = () => {}
export function useAuthToken(
  getter: () => string,
  setter: (t: string) => void,
) {
  getToken = getter
  setToken = setter
}

// ───────────────────────────────────────────────────────────────────────────────
// Axios-Instanz
// ───────────────────────────────────────────────────────────────────────────────
export const http = axios.create({
  baseURL: API_BASE,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

// ───────────────────────────────────────────────────────────────────────────────
// Header-Helper
// ───────────────────────────────────────────────────────────────────────────────
function setAuthHeader(headers: any, token?: string) {
  const h = headers ?? {}
  if (token) h['Authorization'] = `Bearer ${token}`
  else delete h['Authorization']
  return h
}

// ───────────────────────────────────────────────────────────────────────────────
// Refresh-Pipeline (Single-Flight)
// ───────────────────────────────────────────────────────────────────────────────
async function refreshAccessToken(): Promise<string> {
  const { data } = await http.post<{ accessToken: string }>(
    '/auth/refresh',
    undefined,
    { skipAuthHeader: true },
  )
  if (!data || typeof data.accessToken !== 'string' || !data.accessToken) {
    throw new Error('Invalid refresh response')
  }
  setToken(data.accessToken)
  http.defaults.headers.common = setAuthHeader(
    http.defaults.headers.common,
    data.accessToken,
  )
  return data.accessToken
}
let refreshPromise: Promise<string> | null = null
function ensureRefresh(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = refreshAccessToken()
      .then((tok) => {
        refreshPromise = null
        return tok
      })
      .catch((e) => {
        refreshPromise = null
        setToken('')
        delete (http.defaults.headers as any).common?.Authorization
        throw e
      })
  }
  return refreshPromise
}

/** Session per Refresh-Cookie vorwärmen (falls kein Token im Speicher). */
export async function prewarmSession(): Promise<boolean> {
  if (getToken()) return true
  try {
    await ensureRefresh()
    return !!getToken()
  } catch {
    return false
  }
}

function redirectToLogin(override?: string) {
  if (typeof window === 'undefined') return
  const here =
    override ??
    window.location.pathname + window.location.search + window.location.hash
  // Loop-Schutz: Wenn wir schon auf /auth sind, nichts tun
  const alreadyOnAuth = window.location.pathname.startsWith('/auth')
  if (alreadyOnAuth) return

  const target =
    '/auth' +
    (here && !here.startsWith('/auth')
      ? `?redirect=${encodeURIComponent(here)}`
      : '')

  try {
    if (window.location.pathname + window.location.search !== target) {
      window.location.replace(target)
    }
  } catch {
    // no-op (SSR o. Ä.)
  }
}

// /me nur einmal parallel laden
let mePromise: Promise<any> | null = null
export async function fetchMeOnce(allowAnonymous = false) {
  if (!mePromise) {
    if (!allowAnonymous) {
      await ensureRefresh().catch(() => {
        redirectToLogin()
        // throw err
      })
    }
    mePromise = http
      .get('/me', {
        allowAnonymous,
        cancelGroup: 'me',
        retry: 0,
        timeoutMs: 8000,
      })
      .then((r) => r.data)
      .finally(() => {
        mePromise = null
      })
  }
  return mePromise
}

// Vor Request: gültigen Bearer sicherstellen (außer bei Auth/allowAnonymous/skipAuthHeader)
async function ensureAccessTokenForRequest(
  cfg: InternalAxiosRequestConfig,
): Promise<string | undefined> {
  if (isAuthEndpoint(cfg.url) || cfg.skipAuthHeader || cfg.allowAnonymous)
    return undefined
  const existing = getToken()
  if (existing) return existing
  try {
    return await ensureRefresh()
  } catch {
    return undefined
  }
}

// REQUEST-INTERCEPTOR: CancelGroup, Timeout, Signal, Authorization
http.interceptors.request.use(async (config: InternalAxiosRequestConfig) => {
  // Cancel-Group
  let controller: AbortController | undefined
  if (config.cancelGroup) {
    inflightByGroup.get(config.cancelGroup)?.abort('newer request')
    controller = new AbortController()
    inflightByGroup.set(config.cancelGroup, controller)
    ;(config as any)._abortController = controller
  }

  // Per-Request Timeout
  if (config.timeoutMs) {
    controller = controller ?? new AbortController()
    const tid = window.setTimeout(
      () => controller?.abort('timeout'),
      config.timeoutMs,
    )
    ;(config as any)._timeoutId = tid
  }

  // Externes Signal verknoten
  if (controller) {
    chainAbortSignals(
      controller,
      config.signal as GenericAbortSignal | undefined,
    )
    config.signal = controller.signal as unknown as GenericAbortSignal
  }

  // Bearer anhängen (außer Auth/skipAuthHeader)
  if (!isAuthEndpoint(config.url) && !config.skipAuthHeader) {
    const token = (await ensureAccessTokenForRequest(config)) ?? getToken()
    config.headers = setAuthHeader(config.headers, token)
  }

  return config
})

// RESPONSE-INTERCEPTOR: Cleanup + Refresh + Auto-Retry
function cleanupAfterResponse(
  cfg?: InternalAxiosRequestConfig | AxiosRequestConfig,
) {
  if (!cfg) return
  const g = (cfg as any).cancelGroup as string | undefined
  const ctrl = (cfg as any)._abortController as AbortController | undefined
  if (g && inflightByGroup.get(g) === ctrl) inflightByGroup.delete(g)
  const tid = (cfg as any)._timeoutId as number | undefined
  if (tid) clearTimeout(tid)
}

http.interceptors.response.use(
  (r) => {
    cleanupAfterResponse(r.config as InternalAxiosRequestConfig)
    return r
  },
  async (error: AxiosError) => {
    const cfg = error.config as InternalAxiosRequestConfig | undefined
    const resp = error.response
    cleanupAfterResponse(cfg)
    if (!cfg) return Promise.reject(error)

    const urlStr = (cfg.url ?? '').toString()
    const authCall = isAuthEndpoint(urlStr)

    // 403: beispielhafter Redirect, wenn Admin-Endpunkt verweigert wurde
    if (resp?.status === 403) {
      if (isAdminPath(urlStr)) {
        try {
          const target = '/my/tokens?denied=admin'
          if (window.location.pathname + window.location.search !== target) {
            window.location.replace(target)
          }
        } catch {}
      }
      return Promise.reject(error)
    }

    const isUnauth = resp?.status === 401 || resp?.status === 419

    // 401/419 bei allowAnonymous → stilles 204
    if (isUnauth && cfg.allowAnonymous) {
      const synthetic: AxiosResponse<undefined> = {
        data: undefined,
        status: 204,
        statusText: 'No Content (anonymous)',
        headers: {},
        config: cfg as InternalAxiosRequestConfig,
      }
      return Promise.resolve(synthetic)
    }

    // 401/419 → genau einmal refreshen + retry (nicht bei Auth-Endpunkten)
    if (isUnauth && !authCall && !cfg._retry) {
      cfg._retry = true
      try {
        const newTok = await ensureRefresh()
        cfg.headers = setAuthHeader(cfg.headers, newTok)
        return http.request(cfg)
      } catch (e) {
        return Promise.reject(e)
      }
    }

    // Auto-Retry bei transienten Fehlern (nur GET)
    const method = (cfg.method ?? 'get').toString().toLowerCase()
    const attempt = (cfg._retryCount ?? 0) + 1
    const max = cfg.retry ?? 0
    const maybeTransient =
      isTransientStatus(resp?.status) ||
      error.code === 'ECONNABORTED' ||
      String(error.message || '')
        .toLowerCase()
        .includes('timeout')
    const canceled =
      error.code === 'ERR_CANCELED' ||
      error.message === 'newer request' ||
      error.message === 'manual abort'
    if (
      method === 'get' &&
      max > 0 &&
      attempt <= max &&
      maybeTransient &&
      !canceled
    ) {
      cfg._retryCount = attempt
      const wait = backoffWithFixedJitter(
        BACKOFF_BASE_MS,
        attempt - 1,
        BACKOFF_CAP_MS,
        100,
      )
      await new Promise((res) => setTimeout(res, wait))
      return http.request(cfg)
    }

    return Promise.reject(error)
  },
)

// Re-Exports nützlicher Helfer für Domänen-APIs
export const ClientUtils = {
  prewarmSession,
  fetchMeOnce,
  abortGroup,
}
