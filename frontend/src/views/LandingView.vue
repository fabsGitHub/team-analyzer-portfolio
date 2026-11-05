<template>
  <div class="landing">
    <section class="hero" aria-labelledby="landing-title">
      <div class="hero-copy">
        <p class="eyebrow">{{ t('landing.eyebrow') }}</p>
        <h1 id="landing-title">{{ t('landing.title') }}</h1>
        <p class="intro">{{ t('landing.intro') }}</p>
        <div class="actions">
          <button type="button" class="button primary" :disabled="busy" @click="startDemo">
            {{ busy ? t('landing.preparingDemo') : t('landing.tryDemo') }}
          </button>
          <RouterLink class="button secondary" to="/auth">{{ t('landing.openApp') }}</RouterLink>
        </div>
        <p class="demo-note">{{ t('landing.demoNote') }}</p>
        <p v-if="error" class="demo-error" role="alert">{{ error }}</p>
        <a class="source-link" href="https://github.com/fabsGitHub/team-analyzer-portfolio" target="_blank" rel="noopener noreferrer">
          {{ t('landing.source') }}
        </a>
      </div>
      <aside class="project-facts" :aria-label="t('landing.workflowTitle')">
        <p class="eyebrow">TEAM ANALYZER</p>
        <div class="fact-row"><span>01</span><p>{{ t('landing.step1Title') }}</p></div>
        <div class="fact-row"><span>02</span><p>{{ t('landing.step2Title') }}</p></div>
        <div class="fact-row"><span>03</span><p>{{ t('landing.step3Title') }}</p></div>
        <p class="fact-footnote">Vue 3 · Spring Boot · MySQL</p>
      </aside>
    </section>

    <section id="about-demo" class="section overview" aria-labelledby="overview-title">
      <p class="eyebrow">{{ t('landing.aboutLabel') }}</p>
      <h2 id="overview-title">{{ t('landing.aboutTitle') }}</h2>
      <p class="section-copy">{{ t('landing.aboutBody') }}</p>
    </section>

    <section class="section workflow" aria-labelledby="workflow-title">
      <h2 id="workflow-title">{{ t('landing.workflowTitle') }}</h2>
      <div class="steps">
        <article class="step">
          <span class="step-number">01</span>
          <h3>{{ t('landing.step1Title') }}</h3>
          <p>{{ t('landing.step1Body') }}</p>
        </article>
        <article class="step">
          <span class="step-number">02</span>
          <h3>{{ t('landing.step2Title') }}</h3>
          <p>{{ t('landing.step2Body') }}</p>
        </article>
        <article class="step">
          <span class="step-number">03</span>
          <h3>{{ t('landing.step3Title') }}</h3>
          <p>{{ t('landing.step3Body') }}</p>
        </article>
      </div>
    </section>

    <section class="section architecture" aria-labelledby="architecture-title">
      <div>
        <p class="eyebrow">{{ t('landing.architectureLabel') }}</p>
        <h2 id="architecture-title">{{ t('landing.architectureTitle') }}</h2>
      </div>
      <div>
        <p class="section-copy">{{ t('landing.architectureBody') }}</p>
        <p class="privacy-note">{{ t('landing.privacyNote') }}</p>
      </div>
    </section>

    <section class="getting-started" aria-labelledby="getting-started-title">
      <div>
        <h2 id="getting-started-title" class="eyebrow">{{ t('landing.gettingStartedTitle') }}</h2>
        <p class="getting-copy">{{ t('landing.gettingStartedBody') }}</p>
      </div>
      <RouterLink class="button primary" to="/auth">{{ t('landing.openApp') }}</RouterLink>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/store'

const { t } = useI18n()
const auth = useAuthStore()
const router = useRouter()
const busy = ref(false)
const error = ref('')

async function startDemo() {
  if (busy.value) return
  busy.value = true
  error.value = ''
  try {
    const surveyId = await auth.startDemo()
    await router.push({ name: 'SurveyResults', params: { id: surveyId } })
  } catch (cause: any) {
    const status = cause?.response?.status
    error.value =
      status === 429
        ? t('landing.demoRateLimited')
        : status === 404
          ? t('landing.demoDisabled')
          : t('landing.demoUnavailable')
  } finally {
    busy.value = false
  }
}
</script>

<style scoped>
.landing { display:flex; flex-direction:column; gap:2rem; padding-block:.5rem 2rem; color:#20262b; }
.hero {
  display:grid; grid-template-columns:minmax(0,1.25fr) minmax(15rem,.75fr);
  gap:clamp(1.5rem,5vw,4rem); align-items:center; padding:clamp(1.5rem,5vw,4rem);
  border:1px solid #e4ded6; border-radius:26px;
  background:radial-gradient(circle at 100% 0%,rgba(226,185,142,.24),transparent 35%),linear-gradient(135deg,#fffdfb 0%,#f7f2ec 100%);
}
.hero-copy { max-width:39rem; }
.eyebrow,.step-number { color:#8e552a; font-size:.76rem; font-weight:750; letter-spacing:.12em; text-transform:uppercase; }
.hero h1 { max-width:12ch; margin:.7rem 0 1rem; font-size:clamp(2.4rem,6vw,4.2rem); line-height:1.02; letter-spacing:-.055em; }
.intro,.section-copy,.step p,.getting-copy,.privacy-note { color:#596168; font-size:1.02rem; line-height:1.7; }
.intro { max-width:55ch; margin:0; }
.actions { display:flex; flex-wrap:wrap; gap:.75rem; margin-top:1.6rem; }
.button {
  display:inline-flex; align-items:center; justify-content:center; min-height:2.8rem; padding:.7rem 1rem;
  border:1px solid transparent; border-radius:999px; font-weight:700; text-decoration:none;
  transition:transform .16s ease,background-color .16s ease;
}
.button { font:inherit; cursor:pointer; }
.button:hover:not(:disabled) { transform:translateY(-1px); }
.button:disabled { cursor:progress; opacity:.75; }
.button.primary { color:#fff; background:#8e552a; }
.button.primary:hover { background:#75431f; }
.button.secondary { color:#30363b; border-color:#d8cec2; background:rgba(255,255,255,.65); }
.demo-note { max-width:55ch; margin:1rem 0 0; color:#596168; font-size:.92rem; line-height:1.6; }
.demo-error { margin:.75rem 0 0; color:#9b2424; font-weight:650; }
.source-link { display:inline-block; margin-top:1rem; color:#8e552a; font-weight:650; }
.project-facts { padding:1.4rem; border:1px solid rgba(142,85,42,.18); border-radius:20px; background:rgba(255,255,255,.82); box-shadow:0 18px 50px rgba(79,58,40,.08); }
.project-facts>.eyebrow { margin:0 0 1rem; }
.fact-row { display:flex; gap:.9rem; align-items:center; padding:.85rem 0; border-top:1px solid #eee7df; }
.fact-row span { color:#a47953; font-size:.78rem; font-weight:800; }
.fact-row p { margin:0; font-weight:650; }
.fact-footnote { margin:1rem 0 0; color:#73777a; font-size:.85rem; }
.section { scroll-margin-top:1.5rem; padding:.75rem clamp(.5rem,2vw,1.5rem); }
.section h2 { margin:.5rem 0 .75rem; font-size:clamp(1.7rem,3.4vw,2.5rem); line-height:1.12; letter-spacing:-.035em; }
.overview { max-width:58rem; }
.section-copy { max-width:74ch; margin:0; }
.steps { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:1rem; margin-top:1rem; }
.step { min-height:12rem; padding:1.25rem; border:1px solid #e4ded6; border-radius:16px; background:#fff; }
.step h3 { margin:.7rem 0 .4rem; font-size:1.1rem; }
.step p { margin:0; font-size:.94rem; }
.architecture { display:grid; grid-template-columns:minmax(12rem,.75fr) minmax(0,1.25fr); gap:2rem; padding-block:1.5rem; border-block:1px solid #e4ded6; }
.architecture h2 { font-size:1.4rem; }
.privacy-note { margin:1rem 0 0; font-size:.88rem; }
.getting-started { display:flex; flex-wrap:wrap; align-items:center; justify-content:space-between; gap:1.25rem; padding:1.4rem; border-radius:18px; background:#f0e7dd; }
.getting-started .eyebrow { margin:0; }
.getting-copy { max-width:78ch; margin:.5rem 0 0; font-size:.92rem; }
@media (max-width:760px) {
  .hero,.architecture { grid-template-columns:1fr; }
  .steps { grid-template-columns:1fr; }
  .hero h1 { max-width:14ch; }
  .step { min-height:auto; }
}
</style>
