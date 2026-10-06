<template>
  <div class="card">
    <h2>{{ t('footer.privacy') }}</h2>
    <p>Datenschutz-Inhalt…</p>
  </div>
</template>
<script setup lang="ts">
import { useI18n } from 'vue-i18n'
const { t } = useI18n()
</script>