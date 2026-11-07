# Team Analyzer

[![CI](https://github.com/fabsGitHub/team-analyzer-portfolio/actions/workflows/ci.yml/badge.svg)](https://github.com/fabsGitHub/team-analyzer-portfolio/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

A full-stack team-survey application: team leads create short surveys, every team member answers once with a personal one-time token, and the team's leads review aggregated results. Built with Java 21 / Spring Boot and Vue 3 / TypeScript.

**Live demo:** <https://team-analyzer-demo.vercel.app> – the real application on Vercel. "Try the live demo" signs you in to a private sample workspace (a fictional team, an editable survey and 18 sample responses) that is deleted after 24 hours. No registration needed.

> This is a portfolio and learning project. It is not presented as a production-audited service.

## Features

- Registration with email confirmation (link plus password), JWT access tokens and rotating, HTTP-only refresh cookies
- Password reset with hashed, single-use, time-limited tokens; a reset revokes all refresh sessions
- Roles: administrators manage teams, leaders create surveys and see their team's results, members answer from "My tokens"
- One-time participation tokens per member; answers are stored anonymously (no link to the token or person, only the submission date)
- Results with averages and answer distribution, CSV export, and JSON export via short-lived signed links
- German and English UI
- Optional demo mode: one-click private sample workspaces that expire after 24 hours

## Technology

| Area | Technology |
| --- | --- |
| Backend | Java 21, Spring Boot 4.1, Spring Security 7, Spring Data JPA / Hibernate 7, Flyway, Nimbus JOSE + JWT |
| Database | MySQL 8.4 |
| Frontend | Vue 3, TypeScript, Vite, Vue Router, vue-i18n, Axios |
| Testing | JUnit 5, Mockito, Testcontainers (MySQL), Cypress component tests |
| Delivery | Docker Compose, nginx, GitHub Actions, Vercel (frontend plus API container), TiDB Cloud |

## Repository layout

| Path | Contents |
| --- | --- |
| [`backend/`](backend/) | REST API: security, domain services, repositories, Flyway migrations |
| [`frontend/`](frontend/) | Vue single-page app (see its [README](frontend/README.md)) |
| [`docker/`](docker/) | Dockerfiles, Compose files and the nginx configuration |
| [`demo/`](demo/) | Earlier static mock-up, kept as a design reference (not deployed) |
| [`docs/`](docs/) | Deployment guide for running the full stack on Vercel |

## Environments

| Environment | What runs | Where it is configured |
| --- | --- | --- |
| Vercel demo | Vue frontend plus the Spring Boot API as a container, TiDB Cloud as the database, Brevo for mail, demo workspaces enabled | Root [`vercel.json`](vercel.json), [`backend/Dockerfile.vercel`](backend/Dockerfile.vercel), [deployment guide](docs/VERCEL_DEPLOYMENT.md) |
| Local stack | MySQL, Spring Boot API and nginx-served frontend | [`docker/docker-compose.yml`](docker/docker-compose.yml), `.env.production` |
| Local tooling (optional) | phpMyAdmin and SonarQube | [`docker/docker-compose.tools.yml`](docker/docker-compose.tools.yml) |
| CI | Backend tests (with a MySQL container), frontend lint/tests/build, Docker image builds | [`.github/workflows/ci.yml`](.github/workflows/ci.yml) |
| Production profile | Spring profile `prod`: TLS to the database, mail via the Brevo API or SMTP, secure cookies | [`application.yml`](backend/src/main/resources/application.yml) |

## Run locally with Docker

Requirements: Docker with Compose v2.

1. Copy `.env.production.example` to `.env.production` (it is ignored by Git).
2. Fill in every value with a fresh random secret, for example:

   ```bash
   openssl rand -base64 32   # APP_JWT_SECRET_BASE64
   openssl rand -hex 32      # each password and both HMAC secrets
   ```

3. Start the stack from the repository root:

   ```bash
   docker compose --env-file .env.production -f docker/docker-compose.yml up --build
   ```

| Service | Address |
| --- | --- |
| App (frontend + `/api` proxy) | <http://localhost:5173> |
| Backend API | <http://localhost:8080> (health: `/actuator/health`) |
| MySQL | `localhost:3306` |

All ports are bound to `127.0.0.1`. Services start in order (database → API → frontend) once each health check passes. To add phpMyAdmin (<http://localhost:8082>) and SonarQube (<http://localhost:9000>), set `SONAR_DB_PASSWORD` and add the tools file:

```bash
docker compose --env-file .env.production -f docker/docker-compose.yml -f docker/docker-compose.tools.yml up --build
```

Stop the stack and keep the data:

```bash
docker compose --env-file .env.production -f docker/docker-compose.yml down
```

The Compose project is named `team-analyzer`, so its database volume is `team-analyzer_dbdata`. Changing `.env.production` does not re-initialise an existing MySQL volume; keep the original credentials or remove the disposable volume with `down -v`.

### First accounts

No accounts are seeded. Register in the app, or set `APP_DEMO_ENABLED=true` and use "Try the live demo" on the start page for a throwaway sample workspace. The local stack has no mail server, so for a disposable local demo set `LOG_VERIFICATION_LINK=true` in `.env.production`; verification and password-reset links are then printed to the backend log (`docker compose ... logs backend`). Keep this disabled anywhere else: the links are one-time credentials.

No account is an administrator by default. To make your own account the first administrator, set `APP_BOOTSTRAP_ADMIN_EMAIL` in `.env.production` to your address, restart the stack, then register and confirm that address: the role is granted on confirmation. Confirming an address needs both the link from the mailbox and the password chosen at registration, so nobody who registers your address (before or after you) can have their password activated or become administrator through your click. If a confirmation fails because someone else registered the address in the meantime, register again or reset the password; a completed password reset also confirms the address.

Alternatively, grant the role directly in the local database:

```bash
docker compose --env-file .env.production -f docker/docker-compose.yml exec db \
  sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" teambase -e "INSERT INTO user_roles (user_id, role) SELECT id, '\''ADMIN'\'' FROM users WHERE email = '\''you@example.com'\''"'
```

Sign in again afterwards so the new role is part of your access token. Administrators create teams and assign leaders; leaders receive the `LEADER` role automatically.

## Develop without Docker

- **Backend:** start MySQL (for example only the `db` service from the Compose file), export the variables from `.env.production`, then run `./mvnw spring-boot:run` in `backend/`. The `mysql` profile is active by default.
- **Frontend:** `pnpm install && pnpm dev` in `frontend/`. The dev server proxies `/api` to `http://localhost:8080`.

## Configuration

The backend reads its configuration from environment variables. Secrets have no defaults, so the API refuses to start without them.

| Variable | Required | Purpose |
| --- | --- | --- |
| `MYSQL_USER`, `MYSQL_PASSWORD` | yes | Database credentials |
| `DB_HOST`, `DB_PORT`, `MYSQL_DATABASE` | no | Database location for the `mysql` profile (defaults: `localhost`, `3306`, `teambase`) |
| `APP_JWT_SECRET_BASE64` | yes | Base64 key for signing access tokens, at least 32 random bytes |
| `EMAIL_VERIFY_HMAC_SECRET` | yes | Key for email-verification tokens, at least 64 bytes |
| `DOWNLOAD_TOKEN_HMAC_SECRET` | yes | Key for signed result-download links, at least 32 bytes |
| `APP_FRONTEND_BASE_URL` | no | Public URL used in emailed links (default `http://localhost:5173`) |
| `APP_CORS_ALLOWED_ORIGINS` | no | Comma-separated origins allowed to call the API cross-origin (default: `APP_FRONTEND_BASE_URL`); not needed when the frontend and API share an origin |
| `APP_BOOTSTRAP_ADMIN_EMAIL` | no | This address becomes administrator once it is registered and confirmed |
| `APP_DEMO_ENABLED` | no | Enables `POST /api/demo/session` ("Try the live demo"): each call creates a private sample workspace that is deleted after 24 hours (default `false`) |
| `APP_DEMO_MAX_*` | no | Limits for demo visitors, with defaults; see [Demo workspaces](docs/VERCEL_DEPLOYMENT.md#limits) |
| `LOG_VERIFICATION_LINK` | no | Print one-time links to the log when mail is disabled (local demos only) |
| `PORT` | no | HTTP port of the API (default `8080`) |

Use a different random value for every secret, and never reuse values from the repository history.

**Production profile (`SPRING_PROFILES_ACTIVE=prod`)** additionally needs `MYSQL_HOST`, `MYSQL_USER` and `MYSQL_PASSWORD` (or the `TIDB_*` equivalents) and a mail setup with a verified sender address in `SPRING_MAIL_FROM`: by default the Brevo transactional API (`BREVO_API_KEY`), or SMTP with `APP_MAIL_PROVIDER=smtp` and `SPRING_MAIL_HOST`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`. Without the database credentials, the Brevo key or the sender address the API refuses to start; wrong SMTP credentials only show up as failed sends in the log. The [Vercel deployment guide](docs/VERCEL_DEPLOYMENT.md) lists every variable. Keep these values in the deployment platform's secret store.

## Automated checks

GitHub Actions runs on every pull request and on pushes to `main`:

- **Backend:** `./mvnw verify`: unit tests, MockMvc tests and integration tests against a real MySQL container (Testcontainers), including the full register → verify → login → refresh → password-reset flow. Needs a Docker runtime locally.
- **Frontend:** `pnpm lint`, Cypress component tests and `pnpm build` (includes `vue-tsc` type checking).
- **Docker:** validates both Compose files and builds both images.

Run them locally:

```bash
cd backend && ./mvnw --batch-mode verify
```

```bash
cd frontend && pnpm install --frozen-lockfile && pnpm exec cypress install && pnpm lint && pnpm test:component && pnpm build
```

## Security notes

- Passwords are hashed with BCrypt. Refresh, participation and password-reset tokens are stored only as SHA-256 hashes.
- Survey responses keep no reference to the participation token or member, and the application tables store only the submission date, so responses cannot be matched with token redemption times. Limits: in very small teams, a date with a single response can still narrow down who answered, and database transaction logs and backups (for example the MySQL binary log, point-in-time recovery or change-data-capture) record which redemption and which response were written together until they expire. The local Compose database runs without a binary log; check the retention of managed databases.
- Download links are HMAC-signed, scoped to a single survey and valid for five minutes.
- The nginx container sends a strict Content Security Policy and related security headers.
- The Compose stack is meant for local development. A real deployment also needs TLS, mail delivery, secret management, backups, monitoring and an operational review.
- Demo workspaces belong to generated accounts (`demo+<id>@example.invalid`) with a random password that is never shown; the browser gets an access token and a 24-hour refresh cookie. Their teams, surveys and accounts are deleted after 24 hours, when a new demo starts and by an hourly job.
- Anyone can start a demo, so demo visitors are limited (see the [deployment guide](docs/VERCEL_DEPLOYMENT.md#limits)): demo starts per IP address and API instance, demo starts per hour and demo workspaces at the same time (both counted in the database, across instances), and surveys and participation tokens per workspace. A demo session that is refreshed replaces its refresh token row. Demo addresses cannot be registered. Survey titles and questions are limited to their column lengths for every account.
- With demo mode off, existing demo sessions end: their access and refresh tokens are refused, and the hourly cleanup still deletes their workspaces after 24 hours.

## Roadmap

- User lookup by email in the team administration (currently user IDs are entered directly)
- Rate limiting for authentication endpoints
- A per-IP demo limit shared across instances (the per-IP count is kept in memory per backend instance; the hourly and workspace limits are already counted in the database)

## License

MIT, see [LICENSE](LICENSE). The license covers the original project code; third-party dependencies keep their own licenses.
