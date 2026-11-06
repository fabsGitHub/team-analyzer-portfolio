<!-- src/App.vue -->
<template>
  <div class="app" :class="{ 'app-landing': isLanding }">
    <HeaderNav />
    <p v-if="backendWaking" class="waking-notice" role="status">{{ t('common.wakingUp') }}</p>
    <main id="main" class="container" tabindex="-1">
      <RouterView />
    </main>
    <Toast />
    <FooterModal />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/store'
import { backendWaking } from '@/api/wakeup'

import HeaderNav from '@/components/HeaderNav.vue'
import Toast from '@/components/Toast.vue'
import FooterModal from '@/components/FooterModal.vue'

const { t } = useI18n()
const { init } = useAuthStore()
const route = useRoute()
const isLanding = computed(() => route.path === '/')
onMounted(() => { void init() })
</script>

<style>
.waking-notice {
  margin: 0;
  padding: 0.6rem 1rem;
  text-align: center;
  color: #5b3a1d;
  background: #f6ead9;
  border-bottom: 1px solid #ead7bd;
}

.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.container {
  width: min(52rem, 100%);
  margin-inline: auto;
  padding: 1rem 1rem 2rem;
  flex: 1;
}

.app-landing .container {
  width: min(70rem, 100%);
  max-width: 70rem;
}
</style>
