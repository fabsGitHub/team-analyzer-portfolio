<template>
  <article class="card info-page">
    <h1>{{ t('footer.help') }}</h1>
    <p>{{ t('helpPage.intro') }}</p>
    <section>
      <h2>{{ t('helpPage.accountTitle') }}</h2>
      <p>{{ t('helpPage.accountBody') }}</p>
    </section>
    <section>
      <h2>{{ t('helpPage.workflowTitle') }}</h2>
      <ul>
        <li>{{ t('helpPage.admin') }}</li>
        <li>{{ t('helpPage.leader') }}</li>
        <li>{{ t('helpPage.participant') }}</li>
      </ul>
    </section>
    <section>
      <h2>{{ t('helpPage.resultsTitle') }}</h2>
      <p>{{ t('helpPage.resultsBody') }}</p>
    </section>
    <section>
      <h2>{{ t('helpPage.safetyTitle') }}</h2>
      <p>{{ t('helpPage.safetyBody') }}</p>
    </section>
  </article>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
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
