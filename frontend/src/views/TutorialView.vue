<template>
  <section class="admin-page">
    <article class="card stack stack--s5">
      <header class="cluster cluster--between cluster--center">
        <h1 class="h1">{{ t('tutorial.quickstart') }}</h1>
      </header>

      <ol class="steps stack stack--s3">
        <li class="step">
          <strong class="step__title">{{ t('tutorial.guide.adminTitle') }}</strong>
          <span class="step__desc">{{ t('tutorial.guide.admin') }}</span>
        </li>

        <li class="step">
          <strong class="step__title">{{ t('tutorial.guide.leaderTitle') }}</strong>
          <span class="step__desc">{{ t('tutorial.guide.leader') }}</span>
        </li>

        <li class="step">
          <strong class="step__title">{{ t('tutorial.guide.memberTitle') }}</strong>
          <span class="step__desc">{{ t('tutorial.guide.member') }}</span>
        </li>

        <li class="step">
          <strong class="step__title">{{ t('tutorial.guide.anonTitle') }}</strong>
          <span class="step__desc">{{ t('tutorial.guide.anon') }}</span>
        </li>

        <li class="step">
          <strong class="step__title">{{ t('tutorial.guide.resultsTitle') }}</strong>
          <ul class="substeps">
            <li>{{ t('results.onlyLeader') }}</li>
            <li>{{ t('results.location') }}</li>
            <li>{{ t('results.download') }}</li>
            <li>{{ t('results.validity') }}</li>
          </ul>
        </li>
      </ol>

      <div class="cluster cluster--between">
        <span></span>
        <RouterLink to="/my/tokens" class="btn btn--primary">
          {{ t('form.mySurveyTokens') }}
        </RouterLink>
      </div>
    </article>
  </section>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
const { t } = useI18n()
</script>

<style scoped>
/* Spacing scale */
:global(:root) {
  --s-2: .5rem;
  --s-3: .75rem;
  --s-4: 1rem;
  --s-5: 1.25rem;
  --s-6: 1.5rem;
}

/* Typography */
.h1 {
  margin: 0;
  font-size: 1.6rem;
}

/* Layout helpers */
.stack>*+* {
  margin-top: var(--space, var(--s-4));
}

.stack--s3 {
  --space: var(--s-3);
}

.stack--s5 {
  --space: var(--s-5);
}

.cluster {
  display: flex;
  gap: var(--s-2);
}

.cluster--between {
  justify-content: space-between;
}

.cluster--center {
  align-items: center;
}

.admin-page {
  display: grid;
  gap: var(--s-6);
}

.card {
  max-width: 820px;
  margin-inline: auto;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: var(--s-6);
  background: #fff;
}

/* Steps */
.steps {
  padding-left: 1.1rem;
  margin: 0;
}

.step {
  line-height: 1.5;
}

.step__title {
  display: block;
  margin-bottom: .25rem;
}

.step__desc {
  display: block;
}

.substeps {
  margin: .25rem 0 0;
  padding-left: 1.2rem;
  list-style: disc;
}

/* Buttons */
.btn {
  padding: .45rem .8rem;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  background: #f9fafb;
  text-decoration: none;
  display: inline-block;
}

.btn--primary {
  background: #2563eb;
  border-color: #2563eb;
  color: #fff;
}
</style>
