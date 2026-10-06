import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { i18n } from './i18n'
import './styles.css'

const app = createApp(App).use(i18n).use(router)

// zeigt Fehler statt White-Screen
app.config.errorHandler = (err, info) => {
  // Minimal-UI: Log + sichtbare Meldung
  console.error('[Vue Error]', err, info)
  const el = document.getElementById('global-error')
  if (!el) {
    const box = document.createElement('div')
    box.id = 'global-error'
    box.style.cssText =
      'position:fixed;inset:auto 1rem 1rem auto;background:#fee;border:1px solid #fca5a5;color:#991b1b;padding:.5rem .75rem;border-radius:8px;z-index:9999;font:14px/1.4 system-ui;max-width:60ch'
    box.textContent = 'Es ist ein Fehler aufgetreten. Bitte Seite neu laden.'
    document.body.appendChild(box)
  }
}

app.mount('#app')
