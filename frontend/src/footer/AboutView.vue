<template>
  <div class="card">
    <h2>{{ t('footer.about') }}</h2>
    <p>Über Team Radar…</p>
  </div>
</template>
<script setup lang="ts">
import { useI18n } from 'vue-i18n'
const { t } = useI18n()
</script>