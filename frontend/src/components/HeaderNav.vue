<!-- frontend/src/components/HeaderNav.vue -->
<template>
    <header class="nav" role="banner">
        <div class="navwrap">
            <div class="brand" aria-label="App" @click="router.push('/my/tokens')" style="cursor:pointer" tabindex="0"
                @keydown.enter="router.push('/my/tokens')">
                {{ t('app') }}
            </div>

            <nav v-if="store.state.user" aria-label="Hauptnavigation" class="tabs">
                <RouterLink v-if="store.state.user?.roles.includes(Roles.ADMIN)" class="tab"
                    :aria-current="route.path === '/admin/teams' ? 'page' : undefined" to="/admin/teams">
                    {{ t('nav.admin') }}
                </RouterLink>

                <RouterLink v-if="store.state.user?.roles.includes(Roles.LEADER)" class="tab"
                    :aria-current="route.path === '/leader/surveys' ? 'page' : undefined" to="/leader/surveys">
                    {{ t('nav.createSurvey') }}
                </RouterLink>

                <RouterLink v-if="store.state.user?.roles.includes(Roles.LEADER)" class="tab"
                    :aria-current="route.path === '/surveys' ? 'page' : undefined" to="/surveys">
                    {{ t('nav.mySurveys') }}
                </RouterLink>

                <RouterLink class="tab" :aria-current="route.path === '/my/tokens' ? 'page' : undefined"
                    to="/my/tokens">
                    {{ t('nav.myTokens') }}
                </RouterLink>

                <RouterLink class="tab" :aria-current="route.path === '/tutorial' ? 'page' : undefined" to="/tutorial">
                    {{ t('nav.tutorial') }}
                </RouterLink>
            </nav>

            <div v-if="store.state.user" class="user" @keydown.escape="menuOpen = false">
                <button class="iconbtn" :aria-expanded="menuOpen" aria-haspopup="menu" @click.stop="toggleMenu"
                    title="Konto">
                    <svg class="icon" viewBox="0 0 24 24" aria-hidden="true">
                        <path
                            d="M12 12a5 5 0 1 0-5-5 5 5 0 0 0 5 5Zm0 2c-5 0-9 2.5-9 5.5V22h18v-2.5C21 16.5 17 14 12 14Z" />
                    </svg>
                </button>

                <div v-show="menuOpen" class="menu" role="menu" @click.stop>
                    <div class="menu-header">
                        {{ t('user.email') }}:
                        <div class="menu-email">{{ store.state.user?.email }}</div>
                        {{ t('user.id') }}:
                        <div class="menu-email">{{ store.state.user?.id }}</div>
                        {{ t('user.roles') }}:
                        <div class="menu-email">{{ store.state.user?.roles.join(', ') }}</div>
                    </div>

                    <div class="menu-sep" />

                    <div class="menu-sep" />
                    <button class="menu-item" role="menuitem" @click="onLogout">
                        {{ t('user.logout') || 'Abmelden' }}
                    </button>
                </div>
            </div>

            <div class="lang">
                <select id="lang" class="select" @change="onLang" :value="locale" style="margin:.25rem 0 .5rem">
                    <option value="de">{{ t('nav.language.de') }}</option>
                    <option value="en">{{ t('nav.language.en') }}</option>
                </select>
            </div>
        </div>
    </header>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/store'
import { Roles } from '@/types'

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()
const store = useAuthStore()

function onLang(e: Event) {
    const lang = (e.target as HTMLSelectElement).value as 'de' | 'en'
    store.setLanguage(lang)  // persistiert & setzt i18n global
    locale.value = lang      // sofortiges local-Update
}

const menuOpen = ref(false)
const toggleMenu = () => (menuOpen.value = !menuOpen.value)
const close = () => (menuOpen.value = false)
const onWindowClick = () => close()

onMounted(() => window.addEventListener('click', onWindowClick))
onBeforeUnmount(() => window.removeEventListener('click', onWindowClick))

async function onLogout() {
    try {
        await store.logout()
    } finally {
        close()
        router.push('/auth')
    }
}
</script>

<style scoped>
.tabs {
    margin-left: auto;
    display: flex;
    gap: .25rem;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
}
</style>
