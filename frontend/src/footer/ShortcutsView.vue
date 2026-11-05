<template>
  <article class="card info-page">
    <h1>{{ t('footer.shortcuts') }}</h1>
    <p>{{ t('shortcutsPage.intro') }}</p>
    <ul>
      <li>{{ t('shortcutsPage.navigation') }}</li>
      <li>{{ t('shortcutsPage.escape') }}</li>
    </ul>
    <p>{{ t('shortcutsPage.custom') }}</p>
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
</style>
