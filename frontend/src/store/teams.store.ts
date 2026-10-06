// src/store/teams.store.ts
import { reactive } from 'vue'
import * as TeamsApi from '@/api/teams.api'
import type { TeamLite, TeamAdminDto } from '@/types'
import { i18n } from '@/i18n'
import { useAuthStore } from './auth.store'

const $t = (k: string) => (i18n.global as any).t(k) as string

function uuid() {
  return crypto.randomUUID?.() ?? Math.random().toString(36).slice(2)
}
const latestRun: Record<string, string> = {}
function newRun(key: string) {
  const id = uuid()
  latestRun[key] = id
  return id
}
function isStale(key: string, id: string) {
  return latestRun[key] !== id
}
function isCanceledError(e: any) {
  const msg = String(e?.message || '').toLowerCase()
  return (
    e?.code === 'ERR_CANCELED' ||
    msg.includes('abort') ||
    msg.includes('canceled')
  )
}

export interface TeamsState {
  myTeams: TeamLite[]
  leaderLoading: boolean
  leaderError: string

  teamsAdmin: TeamAdminDto[]
  adminLoading: boolean
  adminError: string
}

const state: TeamsState = reactive({
  myTeams: [],
  leaderLoading: false,
  leaderError: '',
  teamsAdmin: [],
  adminLoading: false,
  adminError: '',
})

export function useTeamsStore() {
  const auth = useAuthStore()

  async function loadMyTeams() {
    const run = newRun('me/teams')
    state.leaderLoading = true
    state.leaderError = ''
    try {
      const teams = await TeamsApi.myTeams()
      if (isStale('me/teams', run)) return state.myTeams
      state.myTeams = teams
      return teams
    } catch (e: any) {
      if (isCanceledError(e) || isStale('me/teams', run)) return state.myTeams
      state.leaderError = e?.message || String(e)
      throw e
    } finally {
      if (!isStale('me/teams', run)) state.leaderLoading = false
    }
  }

  async function loadTeamsAdmin() {
    const run = newRun('admin/teams')
    state.adminLoading = true
    state.adminError = ''
    try {
      const teams = await TeamsApi.listTeamsAdmin()
      if (isStale('admin/teams', run)) return state.teamsAdmin
      state.teamsAdmin = teams
      return teams
    } catch (e: any) {
      if (isCanceledError(e) || isStale('admin/teams', run))
        return state.teamsAdmin
      state.adminError = e?.message || String(e)
      throw e
    } finally {
      if (!isStale('admin/teams', run)) state.adminLoading = false
    }
  }

  async function createTeamAdmin(name: string, leaderUserId: string) {
    await TeamsApi.createTeamAdmin(name, leaderUserId)
    auth.toast($t('team.created') || 'Team erstellt.', 'success')
    await loadTeamsAdmin()
  }
  async function addMemberAdmin(
    teamId: string,
    userId: string,
    leader = false,
  ) {
    await TeamsApi.addOrUpdateMemberAdmin(teamId, userId, leader)
    auth.toast($t('team.member_added') || 'Mitglied hinzugefügt.', 'success')
    await loadTeamsAdmin()
  }
  async function setLeaderAdmin(
    teamId: string,
    userId: string,
    leader: boolean,
  ) {
    await TeamsApi.addOrUpdateMemberAdmin(teamId, userId, leader)
    auth.toast($t('team.leader_updated') || 'Leader aktualisiert.', 'success')
    await loadTeamsAdmin()
  }
  async function removeMemberAdmin(teamId: string, userId: string) {
    await TeamsApi.removeMemberAdmin(teamId, userId)
    auth.toast($t('team.member_removed') || 'Mitglied entfernt.', 'success')
    await loadTeamsAdmin()
  }
  async function deleteTeamAdmin(teamId: string) {
    await TeamsApi.deleteTeamAdmin(teamId)
    await loadTeamsAdmin()
  }

  return {
    state,
    loadMyTeams,
    loadTeamsAdmin,
    createTeamAdmin,
    addMemberAdmin,
    setLeaderAdmin,
    removeMemberAdmin,
    deleteTeamAdmin,
  }
}
export type TeamsStore = ReturnType<typeof useTeamsStore>
