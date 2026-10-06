// src/router.ts
import {
  createRouter,
  createWebHistory,
  type RouteLocationNormalized,
} from 'vue-router'
import AuthView from '@/views/AuthView.vue'
import TutorialView from '@/views/TutorialView.vue'
import VerifyView from '@/views/VerifyView.vue'

import ImprintView from '@/footer/ImprintView.vue'
import PrivacyView from '@/footer/PrivacyView.vue'
import HelpView from '@/footer/HelpView.vue'
import ShortcutsView from '@/footer/ShortcutsView.vue'
import AboutView from '@/footer/AboutView.vue'
import SurveyFillView from '@/views/SurveyFillView.vue'
import SurveyCreateView from '@/views/SurveyCreateView.vue'
import AdminTeamView from '@/views/AdminTeamView.vue'
import MyTokensView from '@/views/MyTokensView.vue'
import ResetPasswordView from '@/views/ResetPasswordView.vue'

import { ClientUtils } from '@/api/client'
import * as AuthApi from '@/api/auth.api'
import { useAuthStore } from '@/store'

const routes = [
  { path: '/', redirect: '/auth' },

  // /auth: wenn Session automatisch möglich -> direkt /my/tokens
  {
    path: '/auth',
    component: AuthView,
    beforeEnter: async () => {
      const auth = useAuthStore()
      const hasSession = await ClientUtils.prewarmSession()
      if (!hasSession) return true
      if (!auth.state.user) {
        try {
          const me = await AuthApi.me()
          auth.setUser(me as any)
        } catch {
          return true
        }
      }
      return { path: '/my/tokens' }
    },
  },
  { path: '/auth/reset', component: ResetPasswordView },

  { path: '/tutorial', component: TutorialView },
  { path: '/verify', component: VerifyView },
  { path: '/imprint', component: ImprintView },
  { path: '/privacy', component: PrivacyView },
  { path: '/help', component: HelpView },
  { path: '/shortcuts', component: ShortcutsView },
  { path: '/about', component: AboutView },

  // Public 404 -> /auth
  { path: '/:pathMatch(.*)*', redirect: '/auth' },

  // Public Survey (mit Token im Query erlauben wir anonymen Zugriff)
  { path: '/surveys/:id', name: 'Survey', component: SurveyFillView },

  // Meine offenen Tokens
  { path: '/my/tokens', name: 'MyToken', component: MyTokensView },

  // Leader: Survey anlegen
  {
    path: '/leader/surveys',
    name: 'SurveyManage',
    component: SurveyCreateView,
    meta: { requiresRole: ['LEADER'] },
  },

  // Leader: eigene Surveys auflisten
  {
    path: '/surveys',
    name: 'SurveyList',
    component: () => import('@/views/MySurveysView.vue'),
    meta: { requiresRole: ['LEADER'] },
  },

  // Admin: Teams verwalten
  {
    path: '/admin/teams',
    name: 'TeamAdmin',
    component: AdminTeamView,
    meta: { requiresRole: ['ADMIN'] },
  },

  // Ergebnisse (Leader)
  {
    path: '/surveys/:id/results',
    name: 'SurveyResults',
    component: () => import('@/views/SurveyResultsView.vue'),
    meta: { requiresRole: ['LEADER'] },
  },
]

const router = createRouter({ history: createWebHistory(), routes })

const PUBLIC_PATHS = new Set<string>([
  '/auth',
  '/auth/reset',
  '/verify',
  '/imprint',
  '/privacy',
  '/help',
  '/shortcuts',
  '/about',
])

function hasRole(user: any, role: 'ADMIN' | 'LEADER') {
  if (!user) return false
  const roles: string[] = Array.isArray(user.roles) ? user.roles : []
  const isAdmin = roles.includes('ADMIN')
  if (role === 'ADMIN') return isAdmin
  if (role === 'LEADER') return roles.includes('LEADER') || isAdmin
  return false
}

// Survey ist "public", wenn ein Token im Query hängt
function isPublicSurveyRoute(to: RouteLocationNormalized) {
  return (
    to.name === 'Survey' &&
    typeof to.query?.token === 'string' &&
    (to.query.token as string).length > 0
  )
}

// Globaler Guard
router.beforeEach(async (to) => {
  const auth = useAuthStore()

  // 1) Survey mit Token: IMMER anzeigen, kein Login-Zwang.
  if (isPublicSurveyRoute(to)) {
    // best-effort: stiller Auto-Login im Hintergrund, aber niemals blockieren
    ClientUtils.prewarmSession()
      .then(async (ok: boolean) => {
        if (ok && !auth.state.user) {
          try {
            const me = await AuthApi.me()
            auth.setUser(me as any)
          } catch {
            /* ignore – Survey bleibt sichtbar */
          }
        }
      })
      .catch(() => {})
    return true
  }

  // 2) Statische Public Pages: durchlassen, nur anonymes /me probieren
  if (PUBLIC_PATHS.has(to.path)) {
    AuthApi.meAnonymousOk().catch(() => {})
    return true
  }

  // 3) Geschützte Bereiche: falls keine Userinfo, versuche Auto-Login, sonst /auth
  if (!auth.state.user) {
    const hasSession = await ClientUtils.prewarmSession()
    if (!hasSession) {
      return { path: '/auth', query: { redirect: to.fullPath } }
    }
    try {
      const me = await AuthApi.me()
      auth.setUser(me as any)
    } catch {
      return { path: '/auth', query: { redirect: to.fullPath } }
    }
  }

  // 4) Rollenpflicht (Leader/Admin)
  const required = (to.meta as any)?.requiresRole as string[] | undefined
  if (required?.length) {
    const u = auth.state.user
    const ok = required.every((r) => hasRole(u, r as any))
    if (!ok) {
      const need = required.find((r) => !hasRole(u, r as any))
      return { path: '/my/tokens', query: { denied: need ?? 'forbidden' } }
    }
  }

  return true
})

router.afterEach(() => {
  setTimeout(() => document.getElementById('main')?.focus(), 0)
})

export default router
