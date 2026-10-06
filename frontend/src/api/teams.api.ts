// src/api/teams.api.ts
import { http } from './client'
import type { TeamLite, TeamAdminDto } from '@/types'

export async function myTeams(leaderOnly = true): Promise<TeamLite[]> {
  const { data } = await http.get<TeamLite[]>('/me/teams', {
    params: { leaderOnly },
    cancelGroup: 'me/teams',
    retry: 2,
    timeoutMs: 8000,
  })
  return data
}

export async function listTeamsAdmin(): Promise<TeamAdminDto[]> {
  const { data } = await http.get<TeamAdminDto[]>('/admin/teams', {
    cancelGroup: 'admin/teams',
    retry: 2,
    timeoutMs: 8000,
  })
  return data
}

export async function createTeamAdmin(
  name: string,
  leaderUserId: string,
): Promise<void> {
  await http.post('/admin/teams', { name, leaderUserId })
}

export async function addOrUpdateMemberAdmin(
  teamId: string,
  userId: string,
  leader = false,
): Promise<void> {
  await http.put(`/admin/teams/${teamId}/members/${userId}`, { leader })
}

export async function removeMemberAdmin(
  teamId: string,
  userId: string,
): Promise<void> {
  await http.delete(`/admin/teams/${teamId}/members/${userId}`)
}

export async function deleteTeamAdmin(teamId: string): Promise<void> {
  await http.delete(`/admin/teams/${teamId}`)
}
