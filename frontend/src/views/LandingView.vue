<template>
  <main class="landing page-shell">
    <section class="hero-card card">
      <p class="eyebrow">Team feedback, made useful</p>
      <h1>Make space for better conversations.</h1>
      <p class="lead">
        Create a team pulse, collect anonymous feedback, and see the results in a real workspace.
        Start with a private sample team and explore the full survey flow.
      </p>

      <div class="actions">
        <button class="btn primary" :disabled="busy" @click="startDemo">
          {{ busy ? 'Preparing your workspace…' : 'Try the live demo' }}
        </button>
        <RouterLink class="btn" to="/auth">Sign in or create an account</RouterLink>
      </div>

      <p class="note">
        Your demo gets its own fictional team, 18 sample responses, and editable surveys.
        Demo workspaces expire after 24 hours.
      </p>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
    </section>

    <section class="feature-grid" aria-label="What you can try">
      <article class="card">
        <h2>Create a pulse</h2>
        <p>Write five questions for a team and create a real survey in the backend.</p>
      </article>
      <article class="card">
        <h2>Invite participants</h2>
        <p>Issue private one-time links and collect anonymous responses.</p>
      </article>
      <article class="card">
        <h2>Explore the signal</h2>
        <p>Review seeded results, distributions, and downloadable data from the database.</p>
      </article>
    </section>

    <footer class="site-footer">
      <a href="https://github.com/fabsGitHub/team-analyzer-portfolio" target="_blank" rel="noreferrer">
        View the correct project repository
      </a>
    </footer>
  </main>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '@/store'

const auth = useAuthStore()
const router = useRouter()
const busy = ref(false)
const error = ref('')

async function startDemo() {
  busy.value = true
  error.value = ''
  try {
    const surveyId = await auth.startDemo()
    await router.push({ name: 'SurveyResults', params: { id: surveyId } })
  } catch (cause: any) {
    const status = cause?.response?.status
    error.value =
      status === 429
        ? 'Too many demo sessions were started from this network. Please try again in a little while.'
        : status === 404
          ? 'The live demo is not enabled yet. Please check back soon.'
          : 'The demo could not connect to its backend. Please try again in a moment.'
  } finally {
    busy.value = false
  }
}
</script>

<style scoped>
.landing {
  display: grid;
  gap: 1rem;
  max-width: 76rem;
  margin-inline: auto;
  padding: clamp(1rem, 4vw, 3rem);
}
.hero-card {
  padding: clamp(1.5rem, 5vw, 4rem);
  background:
    radial-gradient(circle at 88% 10%, rgba(207, 154, 91, .2), transparent 32%),
    var(--surface, #fff);
}
.eyebrow {
  color: var(--accent, #9c5a2b);
  font-size: .82rem;
  font-weight: 700;
  letter-spacing: .12em;
  text-transform: uppercase;
}
h1 {
  max-width: 15ch;
  margin: .5rem 0;
  font-size: clamp(2.4rem, 7vw, 5rem);
  line-height: 1.02;
}
.lead {
  max-width: 62ch;
  color: var(--muted, #5b6470);
  font-size: 1.15rem;
  line-height: 1.65;
}
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: .75rem;
  margin-block: 1.5rem;
}
.note {
  color: var(--muted, #5b6470);
  max-width: 70ch;
}
.feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 15rem), 1fr));
  gap: 1rem;
}
.feature-grid .card {
  padding: 1.25rem;
}
.site-footer {
  padding-block: 1rem;
  text-align: center;
}
.error {
  color: #9b2424;
  font-weight: 600;
}
</style>
