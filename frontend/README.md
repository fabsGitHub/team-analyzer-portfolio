# Team Analyzer – frontend

Vue 3 + TypeScript single-page app built with Vite. It talks to the Spring Boot API under `/api`.

## Development

```bash
pnpm install
pnpm exec cypress install   # once: pnpm does not run Cypress's download script
pnpm dev                    # http://localhost:5173, proxies /api to http://localhost:8080
```

Start the backend first (see the repository README), or run the whole stack with Docker Compose.

## Scripts

| Script | Purpose |
| --- | --- |
| `pnpm dev` | Vite dev server with hot reload and Vue DevTools |
| `pnpm build` | Type-check and build the production bundle into `dist/` |
| `pnpm type-check` | Run `vue-tsc` only |
| `pnpm lint` | ESLint for Vue templates and JavaScript |
| `pnpm test:component` | Cypress component tests (headless) |
| `pnpm format` | Prettier |

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `VITE_API_BASE` | `/api` | Base URL of the REST API. Keep the default when the API is served from the same origin (dev proxy, nginx). |

## Structure

- `src/api/` – Axios client with access-token refresh, request cancellation and retries, plus typed endpoint wrappers
- `src/store/` – small reactive stores for auth, surveys and teams
- `src/composables/` – view logic shared between components
- `src/views/`, `src/components/` – pages and UI building blocks
- `src/i18n.ts` – German and English messages
- `cypress/component/` – component tests with mocked API modules
