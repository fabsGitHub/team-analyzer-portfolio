<template>
  <article class="card info-page">
    <h1>{{ t('footer.imprint') }}</h1>
    <p>{{ t('imprintPage.description') }}</p>
    <h2>{{ t('imprintPage.sourceHeading') }}</h2>
    <p>
      <a :href="SOURCE_REPOSITORY_URL" target="_blank" rel="noopener noreferrer">
        {{ t('imprintPage.sourceLink') }}
      </a>
    </p>
    <p>{{ t('imprintPage.contact') }}</p>
    <p class="scope-note">{{ t('imprintPage.scope') }}</p>
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
.scope-note {
  margin-block-start: 1.5rem;
  font-size: 0.92em;
}
</style>
