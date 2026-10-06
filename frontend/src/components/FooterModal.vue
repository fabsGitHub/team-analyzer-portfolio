<!-- frontend/src/components/FooterModal.vue -->
<template>
    <footer class="footer">
        <ul class="links">
            <li><RouterLink to="/imprint" rel="nofollow">{{ t('footer.imprint') }}</RouterLink></li>
            <li><RouterLink to="/privacy" rel="nofollow">{{ t('footer.privacy') }}</RouterLink></li>
            <li><RouterLink to="/help" rel="nofollow">{{ t('footer.help') }}</RouterLink></li>
            <li><RouterLink to="/shortcuts" rel="nofollow">{{ t('footer.shortcuts') }}</RouterLink></li>
            <li><RouterLink to="/about" rel="nofollow">{{ t('footer.about') }}</RouterLink></li>
        </ul>
    </footer>
</template>
<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
const { t } = useI18n()
</script>
<style scoped>
.footer {
    width: 100%;
    background: var(--surface);
    border-top: 1px solid var(--border);
    color: var(--muted);
    font-size: 0.97em;
    padding: 0.5rem 0;
    position: fixed;
    left: 0;
    bottom: 0;
    z-index: 20;
    display: flex;
    justify-content: center;
}

.links {
    display: flex;
    gap: 1.2rem;
    list-style: none;
    margin: 0;
    padding: 0;
}

.links li {
    display: inline;
}

a, .router-link-active {
    color: inherit;
    text-decoration: underline;
    transition: color 0.15s;
}
a:hover, .router-link-active:hover {
    color: var(--accent);
}
</style>
