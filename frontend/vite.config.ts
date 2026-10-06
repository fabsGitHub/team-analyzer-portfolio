// frontend/vite.config.ts
import { defineConfig, type PluginOption } from 'vite'
import vue from '@vitejs/plugin-vue'
import { createHtmlPlugin } from 'vite-plugin-html'
import vueDevTools from 'vite-plugin-vue-devtools'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ command }) => {
  const plugins: PluginOption[] = [
    vue(),
    createHtmlPlugin({}),
  ]

  // nur im Dev-Server laden
  if (command === 'serve') {
    const p = vueDevTools()
    // vueDevTools kann Plugin ODER Plugin[] zurückgeben
    if (Array.isArray(p)) plugins.push(...(p as PluginOption[]))
    else plugins.push(p as PluginOption)
  }

  return {
    plugins,
    resolve: {
      alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
    },
    base: '/', // Deployment-Pfad ggf. anpassen
    server: {
      port: 5173,
      strictPort: true,
      proxy: {
        '/api': { target: 'http://localhost:8080', changeOrigin: true },
      },
    },
    build: {
      target: 'es2020',
      sourcemap: false,
      outDir: 'dist',
      emptyOutDir: true,
    },
  }
})
