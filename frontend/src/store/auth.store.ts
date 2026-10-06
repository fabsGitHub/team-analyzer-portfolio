// src/store/auth.store.ts
import { reactive } from 'vue'
import { useAuthToken } from '@/api/client'
import * as AuthApi from '@/api/auth.api'
import { i18n } from '@/i18n'

const TOAST_AUTOHIDE_MS = 2500 as const
const $t = (k: string) => (i18n.global as any).t(k) as string

export interface Toast {
  id: string
  text: string
  type?: 'info' | 'success' | 'warning' | 'error'
}

function uuid() {
  return crypto.randomUUID?.() ?? Math.random().toString(36).slice(2)
}

export interface AuthState {
  token: string
  user: {
    email: string
    roles: string[]
    id?: string
    isLeader?: boolean
  } | null
  loading: boolean
  error: string
  language: 'de' | 'en'
  toasts: Toast[]
}

const state: AuthState = reactive({
  token: '',
  user: null,
  loading: false,
  error: '',
  language: 'de',
  toasts: [],
})

// wire token <-> client
useAuthToken(
  () => state.token,
  (t) => {
    state.token = t
  },
)

function dismiss(id: string) {
  state.toasts = state.toasts.filter((t) => t.id !== id)
}
function toast(text: string, type?: Toast['type']) {
  const id = uuid()
  state.toasts.push({ id, text, type })
  setTimeout(() => dismiss(id), TOAST_AUTOHIDE_MS)
}

function setI18nLocale(lang: string) {
  const loc = (i18n.global as any).locale
  if (loc && typeof loc === 'object' && 'value' in loc) loc.value = lang
  else (i18n.global as any).locale = lang
}

export function useAuthStore() {
  // init ohne Refresh
  async function init() {
    try {
      const saved =
        typeof window !== 'undefined'
          ? sessionStorage.getItem('app.lang') ||
            localStorage.getItem('app.lang')
          : null
      if (saved === 'de' || saved === 'en') {
        state.language = saved
        setI18nLocale(saved)
      }
    } catch {}
    state.loading = true
    state.error = ''
    try {
      await AuthApi.meAnonymousOk()
    } finally {
      state.loading = false
    }
  }

  async function login(email: string, password: string) {
    const { accessToken } = await AuthApi.login(email, password)
    state.token = accessToken // 👈 Token speichern (useAuthToken getter liefert das danach)
    state.user = await AuthApi.me() // klappt jetzt, weil Interceptor den Bearer anhängt
    toast($t('toast.signedin'), 'success')
  }

  async function register(email: string, password: string) {
    await AuthApi.register(email, password)
    toast($t('auth.check_mail'), 'info')
  }

  async function logout() {
    try {
      await AuthApi.logout()
    } finally {
      state.user = null
      state.token = ''
      toast($t('toast.signedout'), 'success')
    }
  }

  async function resetPassword(email: string) {
    await AuthApi.resetPassword(email)
    toast($t('auth.reset_sent'), 'info')
  }

  async function refreshUser() {
    state.user = await AuthApi.me()
    return state.user
  }

  function setLanguage(lang: 'de' | 'en') {
    state.language = lang
    setI18nLocale(lang)
    try {
      sessionStorage.setItem('app.lang', lang)
    } catch {}
  }

  function setUser(u: AuthState['user'] | null) {
    state.user = u
  }

  return {
    state,
    // ui
    toast,
    dismiss,
    // auth
    init,
    login,
    register,
    logout,
    resetPassword,
    refreshUser,
    setLanguage,
    setUser,
  }
}
export type AuthStore = ReturnType<typeof useAuthStore>
