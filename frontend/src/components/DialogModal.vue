<!-- frontend/src/components/DialogModal.vue -->
<template>
  <div v-if="open" class="modal" role="dialog" aria-modal="true" :aria-labelledby="titleId"
    @keydown.esc.prevent="close">
    <div class="backdrop" @click="close"></div>
    <div class="sheet card" ref="panel" tabindex="-1">
      <button class="iconbtn close" @click="close" :aria-label="t('dialog.closeAria')">✕</button>

      <h2 :id="titleId">
        <slot name="title" />
      </h2>

      <div class="content">
        <slot />
      </div>

      <div class="row" style="justify-content:flex-end">
        <button class="btn" @click="close">{{ cancelLabel }}</button>
        <button class="btn danger" v-if="danger" @click="$emit('confirm')">{{ confirmLabel }}</button>
        <button class="btn primary" v-else @click="$emit('confirm')">{{ confirmLabel }}</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch, computed, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  open: boolean
  danger?: boolean
  cancelText?: string
  confirmText?: string
}>()

const emit = defineEmits<{ (e: 'close'): void; (e: 'confirm'): void }>()
const { t } = useI18n()

const panel = ref<HTMLDivElement | null>(null)
const titleId = `dialog-${Math.random().toString(36).slice(2)}`
const close = () => emit('close')

watch(() => props.open, async (v) => {
  if (v) {
    await nextTick()
    panel.value?.focus({ preventScroll: true })
  }
})
onMounted(() => { if (props.open) panel.value?.focus() })

// i18n-Defaults für Buttons
const cancelLabel = computed(() => props.cancelText ?? t('form.cancel'))
const confirmLabel = computed(() => props.confirmText ?? t('form.confirm'))
</script>

<style scoped>
.modal {
  position: fixed;
  inset: 0;
  display: grid;
  place-items: center;
  z-index: 40;
}

.backdrop {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, .35)
}

.sheet {
  position: relative;
  max-width: 34rem;
  width: calc(100% - 2rem);
  max-height: 90vh;
  overflow: auto;
}

.close {
  position: absolute;
  top: .5rem;
  right: .5rem;
}

@media (max-width:720px) {
  .sheet {
    width: 100vw;
    height: 100vh;
    max-width: none;
    border-radius: 0
  }
}
</style>
