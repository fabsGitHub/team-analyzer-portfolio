<template>
  <article class="card info-page">
    <h1>{{ t('footer.privacy') }}</h1>
    <p>{{ t('privacyPage.intro') }}</p>
    <section>
      <h2>{{ t('privacyPage.accountTitle') }}</h2>
      <p>{{ t('privacyPage.account') }}</p>
    </section>
    <section>
      <h2>{{ t('privacyPage.surveyTitle') }}</h2>
      <p>{{ t('privacyPage.survey') }}</p>
    </section>
    <section>
      <h2>{{ t('privacyPage.technicalTitle') }}</h2>
      <p>{{ t('privacyPage.technical') }}</p>
    </section>
    <section>
      <h2>{{ t('privacyPage.limitsTitle') }}</h2>
      <p>{{ t('privacyPage.limits') }}</p>
    </section>
    <section>
      <h2>{{ t('privacyPage.contactTitle') }}</h2>
      <p>{{ t('privacyPage.contact') }}</p>
      <a :href="SOURCE_REPOSITORY_URL" target="_blank" rel="noopener noreferrer">
        {{ t('privacyPage.contactLink') }}
      </a>
    </section>
  </article>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { SOURCE_REPOSITORY_URL } from '@/project'

const { t } = useI18n()
</script>

<style scoped>
.info-page {
  max-width: 52rem;
  margin-inline: auto;
}
.info-page section {
  margin-block-start: 1.25rem;
}
</style>
