<template>
  <div class="card">
    <h2>{{ t('footer.help') }}</h2>
    <p>Hilfe-Inhalt…</p>
  </div>
</template>
<script setup lang="ts">
import { useI18n } from 'vue-i18n'
const { t } = useI18n()
</script>