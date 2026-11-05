// src/api/errors.ts
type Translate = (key: string, named: Record<string, unknown>) => string

/** Limits the API reports for demo workspaces, as `code: "demo.limit.<kind>"` in the problem detail. */
const DEMO_LIMITS = new Set(['address', 'hourly', 'capacity', 'surveys', 'tokens'])

function problemOf(error: unknown): any {
  return (error as any)?.response?.data
}

/** The translated message for a demo limit the API reported, or null for any other error. */
export function demoLimitMessage(error: unknown, t: Translate): string | null {
  const problem = problemOf(error)
  const kind =
    typeof problem?.code === 'string' && problem.code.startsWith('demo.limit.')
      ? problem.code.slice('demo.limit.'.length)
      : ''
  return DEMO_LIMITS.has(kind) ? t(`demoLimits.${kind}`, { limit: problem.limit }) : null
}

/** A message for a failed API call: a demo limit, else the API's own detail, else the fallback. */
export function apiErrorMessage(error: unknown, t: Translate, fallback: string): string {
  const problem = problemOf(error)
  return (
    demoLimitMessage(error, t) ||
    problem?.detail ||
    problem?.message ||
    (error as any)?.message ||
    fallback
  )
}
