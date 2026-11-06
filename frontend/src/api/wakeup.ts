// src/api/wakeup.ts
import { ref } from 'vue'

/**
 * On Vercel the API runs as a container that is stopped after a few idle minutes. The next
 * request then waits while it starts again (about 20–30 seconds), longer than the per-request
 * timeouts of the API client. Before a request, the client therefore makes sure the backend is
 * awake: if it has not answered recently, one shared ping waits for it first.
 */
const PING_TIMEOUT_MS = 90_000
/** Treat the backend as awake for this long after its last answer (Vercel idles it after ~3 min). */
const AWAKE_WINDOW_MS = 60_000
/** Only tell the user about the wait once it is noticeable. */
const NOTICE_AFTER_MS = 3_000

/**
 * True while something the visitor started (a click, a page they opened) has waited a noticeable
 * time for the backend to start; drives the app-wide notice. Background warm-ups stay silent.
 */
export const backendWaking = ref(false)

let lastAnswerAt = 0
let pending: Promise<boolean> | null = null
let noticeTimer: ReturnType<typeof setTimeout> | undefined

/** Any HTTP answer below 500 shows the backend is up (5xx can come from the platform itself). */
export function noteBackendAnswer(status: number | undefined): void {
  if (status !== undefined && status > 0 && status < 500) lastAnswerAt = Date.now()
}

/**
 * Resolves once the backend answers (true) or the wake-up gave up (false); never rejects.
 * `visible`: the caller is waiting on behalf of the visitor, so a long wait is announced.
 */
export function wakeBackend(apiBase: string, visible = true): Promise<boolean> {
  if (Date.now() - lastAnswerAt < AWAKE_WINDOW_MS) return Promise.resolve(true)
  if (!pending) {
    const controller = new AbortController()
    const giveUp = setTimeout(() => controller.abort(), PING_TIMEOUT_MS)
    pending = fetch(`${apiBase}/ping`, {
      cache: 'no-store',
      credentials: 'omit',
      signal: controller.signal,
    })
      .then((r) => {
        noteBackendAnswer(r.status)
        return r.status < 500
      })
      .catch(() => false)
      .finally(() => {
        clearTimeout(giveUp)
        clearTimeout(noticeTimer)
        noticeTimer = undefined
        backendWaking.value = false
        pending = null
      })
  }
  if (visible && !noticeTimer) {
    noticeTimer = setTimeout(() => {
      backendWaking.value = true
    }, NOTICE_AFTER_MS)
  }
  return pending
}
