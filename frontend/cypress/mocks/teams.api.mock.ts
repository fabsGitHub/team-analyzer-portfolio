export type Member = { userId: string; leader: boolean }
export type Team = { id: string; name: string; members: Member[] }
export const __isTeamsApiMock = true

let db: Team[] = [
  {
    id: 't-1', name: 'Initial Team', members: [
      { userId: 'u-1', leader: true }, { userId: 'u-2', leader: false },
    ]
  },
]

const delay = (ms = 0) => new Promise(r => setTimeout(r, ms))

export async function listTeamsAdmin(): Promise<Team[]> {
  await delay(); return JSON.parse(JSON.stringify(db))
}
export async function createTeamAdmin(name: string, leaderUserId: string) {
  await delay(); const id = `t-${Math.random().toString(36).slice(2, 8)}`
  db = [...db, { id, name, members: leaderUserId ? [{ userId: leaderUserId, leader: true }] : [] }]
  return id
}
export async function addOrUpdateMemberAdmin(teamId: string, userId: string, leader: boolean) {
  await delay(); const t = db.find(x => x.id === teamId); if (!t) return
  const ex = t.members.find(m => m.userId === userId); ex ? ex.leader = leader : t.members.push({ userId, leader })
}
export async function removeMemberAdmin(teamId: string, userId: string) {
  await delay(); const t = db.find(x => x.id === teamId); if (!t) return
  t.members = t.members.filter(m => m.userId !== userId)
}
export async function deleteTeamAdmin(teamId: string) {
  await delay(); db = db.filter(t => t.id !== teamId)
}
