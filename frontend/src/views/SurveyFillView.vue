<template>
  <section class="admin-page">
    <article class="card stack" style="--space: var(--s-5)">
      <header class="cluster between center">
        <h1 class="h1">
          {{ survey?.title || t('survey.untitled') }}
        </h1>
      </header>

      <!-- Fehlerhinweis -->
      <p v-if="error" class="error">{{ error }}</p>

      <!-- Ladezustand -->
      <p v-else-if="loading" class="meta">{{ t('survey.fill.loading') }}</p>

      <!-- Inhalt -->
      <div v-else-if="survey" class="stack" style="--space: var(--s-4)">
        <ol class="q-list stack" style="--space: var(--s-4)">
          <li v-for="(q, i) in (survey.questions ?? [])" :key="q.id ?? i" class="q stack" style="--space: var(--s-2)">
            <div class="cluster center" style="gap: var(--s-3)">
              <span class="meta index">{{ i + 1 }}.</span>
              <p class="label">{{ q?.text || t('survey.noQuestionText') }}</p>
            </div>
            <LikertScale :name="'q' + i" v-model="answers[i]" />
          </li>

          <li v-if="!(survey.questions?.length)">
            <span class="meta">{{ t('survey.noQuestions') }}</span>
          </li>
        </ol>

        <div class="stack" style="--space: var(--s-2)">
          <label class="label" for="token">{{ t('survey.fill.token.label') }}</label>
          <input id="token" v-model="token" class="input" type="text" :placeholder="t('survey.fill.token.placeholder')"
            required />
          <small class="meta">{{ t('survey.fill.token.help') }}</small>
        </div>

        <div class="cluster between wrap">
          <span class="meta">{{ t('survey.fill.answered', { n: answersFilled }) }}</span>
          <button class="btn primary" type="submit" :disabled="!valid || submitting" @click.prevent="submit">
            {{ submitting ? t('common.saving') : t('survey.fill.submit') }}
          </button>
        </div>
      </div>

      <!-- Fallback falls gar nichts passt -->
      <p v-else class="meta">{{ t('survey.fill.loading') }}</p>
    </article>
  </section>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, onUnmounted } from "vue";
import { useRoute, useRouter, onBeforeRouteLeave } from "vue-router";
import * as Surveys from "@/api/surveys.api";
import type { SurveyDto } from "@/types";
import LikertScale from "@/components/LikertScale.vue";
import { useI18n } from 'vue-i18n'
import { abortGroup } from '@/api/client'

onBeforeRouteLeave(() => abortGroup('survey'))
onUnmounted(() => abortGroup('survey'))

const { t } = useI18n()
const route = useRoute();
const router = useRouter();

const survey = ref<SurveyDto | null>(null);
const loading = ref<boolean>(false);
const error = ref<string | null>(null);
const submitting = ref<boolean>(false);
const answers = ref<number[]>([]);
const token = ref<string>("");

async function load() {
  loading.value = true; error.value = null; survey.value = null; answers.value = [];
  try {
    token.value = (route.query.token as string) ?? "";
    const id = String(route.params.id || "");
    if (!id) throw new Error("Missing survey id");
    const s = await Surveys.getSurvey(id);
    survey.value = s;
    const qLen = Array.isArray(s.questions) ? s.questions.length : 0;
    answers.value = Array.from({ length: qLen }, () => 0);
  } catch (e: any) {
    error.value = e?.message || "Failed to load survey";
  } finally {
    loading.value = false;
  }
}
onMounted(load)
watch(() => route.fullPath, load)

const answersFilled = computed(() => answers.value.filter(v => v >= 1 && v <= 5).length)
const requiresCount = 5
const valid = computed(() => {
  const tokOk = token.value.trim().length > 0
  const need = Math.min(answers.value.length, requiresCount)
  const ok = answers.value.slice(0, need).every(v => v >= 1 && v <= 5)
  return tokOk && ok && need === requiresCount
})

async function submit() {
  if (!survey.value || !valid.value) return
  submitting.value = true
  try {
    const id = String(route.params.id || "")
    const [q1, q2, q3, q4, q5] = answers.value
    await Surveys.submitSurveyResponses(id, { token: token.value.trim(), q1, q2, q3, q4, q5 })
    router.replace({ name: "MyToken", query: { submitted: "1" } })
  } catch (e: any) {
    error.value = e?.message ?? "Submit failed"
  } finally {
    submitting.value = false
  }
}
</script>


<style scoped>
/* ---- Scale & utilities ---- */
:root,
.admin-page {
  --s-1: .25rem;
  --s-2: .5rem;
  --s-3: .75rem;
  --s-4: 1rem;
  --s-5: 1.25rem;
  --s-6: 1.5rem;
  --s-8: 2rem;
}

.h1 {
  font-size: 1.375rem;
  line-height: 1.2;
  margin: 0
}

.meta {
  color: #6b7280;
  font-size: .9rem
}

.error {
  color: #b91c1c;
  background: #fee2e2;
  border: 1px solid #fecaca;
  padding: .5rem .75rem;
  border-radius: 8px;
}

/* layout utils */
.stack>*+* {
  margin-top: var(--space, var(--s-4))
}

.cluster {
  display: flex;
  gap: var(--s-2)
}

.cluster.center {
  align-items: center
}

.cluster.between {
  justify-content: space-between
}

.cluster.wrap {
  flex-wrap: wrap
}

/* ---- Card & inputs ---- */
.admin-page {
  display: grid;
  gap: var(--s-6)
}

.card {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: var(--s-6);
  background: #fff
}

.input {
  width: 100%;
  padding: .5rem .65rem;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  background: #fff
}

.label {
  font-weight: 600;
  margin: 0
}

.btn {
  padding: .45rem .8rem;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  background: #f9fafb;
  cursor: pointer
}

.btn:hover {
  background: #f3f4f6
}

.btn.primary {
  background: #2563eb;
  border-color: #2563eb;
  color: #fff
}

.btn[disabled] {
  opacity: .6;
  cursor: not-allowed
}

/* ---- Questions ---- */
.q-list {
  list-style: none;
  margin: 0;
  padding: 0
}

.index {
  min-width: 2ch
}
</style>
