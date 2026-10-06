import { defineConfig } from 'cypress'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

const modalStub = fileURLToPath(new URL('./cypress/stubs/DialogModal.stub.vue', import.meta.url))
const teamsApiMock = fileURLToPath(new URL('./cypress/mocks/teams.api.mock.ts', import.meta.url))

// absoluter Pfad zur *einen* Vue-Instanz im Projekt
const vueRuntime = fileURLToPath(
  new URL('./node_modules/vue/dist/vue.runtime.esm-bundler.js', import.meta.url)
)

export default defineConfig({
  component: {
    devServer: {
      framework: 'vue',
      bundler: 'vite',
      viteConfig: {
        plugins: [vue()],
        resolve: {
          // *** Wichtig: dedupe + harter Alias auf *eine* Vue-Datei ***
          dedupe: ['vue', 'vue-i18n'],
          alias: [
            // -> eine Vue-Instanz für alles
            { find: 'vue', replacement: vueRuntime },

            // API *hart* mocken (Composable bleibt echt & reaktiv)
            { find: '@/api/teams.api', replacement: teamsApiMock },
            { find: /^@\/api\/teams\.api(?:\.ts)?$/, replacement: teamsApiMock },

            // DialogModal *hart* stubben (SFC-Datei)
            { find: '@/components/DialogModal.vue', replacement: modalStub },
            { find: /^@\/components\/DialogModal\.vue$/, replacement: modalStub },

            // generischer Alias zuletzt
            { find: '@', replacement: fileURLToPath(new URL('./src', import.meta.url)) },
          ],
        },
        optimizeDeps: {
          // nie das echte API-Modul prebundlen
          exclude: ['@/api/teams.api'],
          // stelle sicher, dass Vite die *eine* Vue nimmt
          include: ['vue', 'vue-i18n'],
        },
      },
    },
    specPattern: 'cypress/component/**/*.cy.{ts,tsx}',
    supportFile: 'cypress/support/component.ts',
  },
})
