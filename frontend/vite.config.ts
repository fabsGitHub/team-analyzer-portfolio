import { defineConfig, type PluginOption } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ command }) => {
  const plugins: PluginOption[] = [vue()]

  // Vue DevTools are only useful (and only loaded) in the dev server.
  if (command === 'serve') {
    plugins.push(vueDevTools() as PluginOption)
  }

  return {
    plugins,
    resolve: {
      alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
    },
    define: {
      // Compile i18n messages without `new Function`, so the app runs under a strict CSP.
      __INTLIFY_JIT_COMPILATION__: true,
    },
    server: {
      port: 5173,
      strictPort: true,
      proxy: {
        // Keep the browser's Host header so the API treats proxied calls as same-origin.
        '/api': { target: 'http://localhost:8080', changeOrigin: false },
      },
    },
    build: {
      target: 'es2020',
      sourcemap: false,
    },
  }
})
