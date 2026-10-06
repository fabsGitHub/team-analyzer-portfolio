// src/composables/useAdminTeams.ts
import { ref } from 'vue'
import type { TeamAdminDto } from '@/types'
import * as Teams from '@/api/teams.api'

export function useAdminTeams() {
  const teams = ref<TeamAdminDto[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function load() {
    loading.value = true
    error.value = null
    try {
      teams.value = await Teams.listTeamsAdmin()
    } catch (e: any) {
      error.value = e?.response?.data?.message || e?.message || 'Load failed'
    } finally {
      loading.value = false
    }
  }

  async function createTeam(name: string, leaderUserId: string) {
    await Teams.createTeamAdmin(name, leaderUserId)
    await load()
  }
  async function addMember(teamId: string, userId: string, leader = false) {
    await Teams.addOrUpdateMemberAdmin(teamId, userId, leader)
    await load()
  }
  async function setLeader(teamId: string, userId: string, leader: boolean) {
    await Teams.addOrUpdateMemberAdmin(teamId, userId, leader)
    await load()
  }
  async function removeMember(teamId: string, userId: string) {
    await Teams.removeMemberAdmin(teamId, userId)
    await load()
  }
  async function deleteTeam(teamId: string) {
    await Teams.deleteTeamAdmin(teamId)
    await load()
  }

  return {
    teams,
    loading,
    error,
    load,
    createTeam,
    addMember,
    setLeader,
    removeMember,
    deleteTeam,
  }
}
