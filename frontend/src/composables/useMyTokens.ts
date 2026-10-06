// src/composables/useMyTokens.ts
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import * as Surveys from '@/api/surveys.api'

type MyOpenToken = Awaited<ReturnType<typeof Surveys.listMyOpenTokens>>[number]

export function useMyTokens() {
  const { t } = useI18n()
  const tokens = ref<MyOpenToken[]>([])
  const loading = ref(true)
  const error = ref<string | null>(null)
  const busyId = ref<string | null>(null)

  async function load() {
    loading.value = true
    error.value = null
    try {
      tokens.value = await Surveys.listMyOpenTokens()
    } catch (e: any) {
      error.value =
        e?.response?.data?.message || e?.message || t('tokens.loadError')
    } finally {
      loading.value = false
    }
  }

  async function goToSurvey(tk: MyOpenToken) {
    busyId.value = tk.surveyId
    try {
      let { inviteLink } = await Surveys.myTokenForSurvey(tk.surveyId)
      if (!inviteLink) {
        const renewed = await Surveys.renewMyToken(tk.surveyId)
        inviteLink = renewed.inviteLink
      }
      if (!inviteLink) throw new Error(t('tokens.noInviteLink'))
      window.location.href = inviteLink
    } catch (e: any) {
      error.value =
        e?.response?.data?.message || e?.message || t('tokens.openError')
    } finally {
      busyId.value = null
    }
  }

  return { tokens, loading, error, busyId, load, goToSurvey }
}
