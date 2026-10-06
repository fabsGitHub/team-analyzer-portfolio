<!-- frontend/src/components/Toast.vue -->
<template>
    <div class="toasts" aria-live="polite" aria-atomic="true">
        <div
            v-for="t in state.toasts as import('../types').Toast[]"
            :key="t.id"
            class="toast-card"
            :class="t.type ? t.type : ''"
        >
            <span class="toast-text">{{ t.text }}</span>
            <button class="toast-close" @click="dismiss(t.id)" aria-label="Schließen">×</button>
        </div>
    </div>
</template>
<script setup lang="ts">
import { useAuthStore } from '@/store';

const { state, dismiss } = useAuthStore()
</script>
<style scoped>
.toasts {
    position: fixed;
    bottom: 1.5rem;
    left: 0;
    right: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: .75rem;
    pointer-events: none;
    z-index: 1000;
}
.toast-card {
    pointer-events: auto;
    min-width: 220px;
    max-width: 90vw;
    background: var(--surface);
    border: 2px solid var(--accent);
    color: var(--fg);
    border-radius: 10px;
    box-shadow: 0 4px 24px 0 rgba(0,0,0,0.13);
    padding: 1rem 2.5rem 1rem 1.2rem;
    font-size: 1.08em;
    font-weight: 500;
    display: flex;
    align-items: center;
    position: relative;
    transition: transform 0.15s, box-shadow 0.15s;
    animation: toast-in 0.25s;
}
.toast-card.danger {
    border-color: var(--danger);
    background: #fff0f0;
    color: var(--danger);
}
.toast-card.success {
    border-color: var(--ok);
    background: #f0fff7;
    color: var(--ok);
}
.toast-text {
    flex: 1;
    word-break: break-word;
}
.toast-close {
    position: absolute;
    top: .5rem;
    right: .7rem;
    background: none;
    border: none;
    color: inherit;
    font-size: 1.3em;
    cursor: pointer;
    padding: 0;
    line-height: 1;
    opacity: 0.7;
    transition: opacity 0.15s;
}
.toast-close:hover {
    opacity: 1;
}
@keyframes toast-in {
    from { transform: translateY(30px) scale(0.98); opacity: 0; }
    to   { transform: none; opacity: 1; }
}
</style>
