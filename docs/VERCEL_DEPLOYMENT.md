# Vercel deployment

Team Analyzer is a full-stack application. Vercel serves the Vue frontend and runs the Spring Boot API in a container. The application database must be an external MySQL-compatible service because Vercel containers have no durable local storage.

The live demo at <https://team-analyzer-demo.vercel.app> runs this setup with `APP_DEMO_ENABLED=true`. The static mock-up in [`demo/`](../demo/) is not deployed.

## Vercel project settings

1. Connect the `fabsGitHub/team-analyzer-portfolio` repository in a new Vercel project.
2. Set the project root directory to the repository root.
3. Select the **Services** framework preset. The root `vercel.json` builds the Vue/Vite frontend and the Spring Boot container, and routes `/api/*` requests to the backend.
4. Set `PORT=8080` in the Vercel project environment for the environments you deploy. Spring Boot listens on this port so the container can run as a non-root user.
5. Add production secrets and service configuration in Vercel. Never commit these values, enter them in GitHub, or paste them into chat.

## Required environment variables

| Variable | Purpose |
| --- | --- |
| `SPRING_PROFILES_ACTIVE=prod` | Enables the production Spring profile. |
| `APP_FRONTEND_BASE_URL` | Public Vercel URL used in verification links. |
| `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD` | MySQL-compatible database connection. `MYSQL_PORT` defaults to `4000`, the TiDB Cloud port; set it to `3306` for a standard MySQL server. SSL identity verification is enabled by default. |
| `TIDB_HOST`, `TIDB_PORT`, `TIDB_DATABASE`, `TIDB_USER`, `TIDB_PASSWORD` | Alternative connection names accepted for TiDB Cloud. Standard `MYSQL_*` variables take precedence when both are set. |
| `MYSQL_SSL_MODE` | Optional MySQL Connector/J SSL mode. Defaults to `VERIFY_IDENTITY`. |
| `APP_JWT_SECRET_BASE64` | Fresh Base64-encoded JWT signing key with at least 32 random bytes. |
| `EMAIL_VERIFY_HMAC_SECRET` | Independent key for email verification tokens, at least 64 bytes, e.g. `openssl rand -hex 32` (64 characters). The API refuses to start with a shorter value. |
| `DOWNLOAD_TOKEN_HMAC_SECRET` | Independent key for signed download links, at least 32 bytes, e.g. `openssl rand -hex 32`. |
| `APP_MAIL_PROVIDER=brevo-api` | Uses Brevo's transactional email API; this is the default for production. Set to `smtp` only when using SMTP credentials instead. |
| `BREVO_API_KEY` | Brevo API key used by the transactional email API. Store it as a Vercel secret. |
| `SPRING_MAIL_FROM`, `SPRING_MAIL_FROM_NAME` | Verified sender address (required for both Brevo and SMTP; the API refuses to start without it) and display name. |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `SPRING_MAIL_PORT` | Optional SMTP configuration when `APP_MAIL_PROVIDER=smtp`. The SMTP password must be an SMTP key, not an API key. Port defaults to `587` with authentication and STARTTLS enabled. |
| `APP_BOOTSTRAP_ADMIN_EMAIL` | The exact email address that should receive the first administrator role, after verification. |
| `LOG_VERIFICATION_LINK=false` | Keep verification-token links out of runtime logs. |
| `APP_DEMO_ENABLED` | `true` enables "Try the live demo" (`POST /api/demo/session`): each call creates a private sample workspace that is deleted after 24 hours. Defaults to `false`. The limits for demo visitors have defaults and optional variables, see [Demo workspaces](#demo-workspaces). |
| `PORT=8080` | Vercel container port. |

Generate new, separate signing secrets for each deployment. Do not reuse any value from this repository's history. Use Vercel's environment-variable secret handling for passwords and signing keys.

Use a managed MySQL-compatible database with TLS enabled and a database/user created for this app. Flyway runs the migration in `backend/src/main/resources/db/migration` when the backend first connects. Do not point preview deployments at a database containing production or personal data.

## First administrator

No administrator account or default password is seeded. Before registering the account you control, set `APP_BOOTSTRAP_ADMIN_EMAIL` to that exact email address in the production environment. Register through the app and complete email verification. The backend assigns `ROLE_ADMIN` only to that address, and only when it is confirmed with the mailbox link together with the account's password (or through a completed password reset), so a registration of the address by someone else can never become administrator. Remove `APP_BOOTSTRAP_ADMIN_EMAIL` from the Vercel environment after the initial account is provisioned; the stored role remains in the database.

Email verification depends on a real transactional email provider. Do not set `LOG_VERIFICATION_LINK=true` on a public deployment.

### Brevo Free transactional email

In Brevo, create and verify a sender, then generate an API key from SMTP & API settings. Configure `APP_MAIL_PROVIDER=brevo-api`, `BREVO_API_KEY`, `SPRING_MAIL_FROM` (the verified sender), and optionally `SPRING_MAIL_FROM_NAME`. Keep the API key only in Vercel's environment variables. The app sends verification and password-reset messages through Brevo's transactional email API. To use SMTP instead, set `APP_MAIL_PROVIDER=smtp` and provide the SMTP host, username, SMTP key, and port. SMTP connection, read, and write timeouts and the HTTP timeouts for the Brevo API (5 s connect, 10 s read) are capped, so a stalled provider cannot leave signup or reset requests waiting indefinitely.

Brevo can block API calls from unknown IP addresses (**Security → Authorised IPs**). Vercel containers have no fixed outbound IP addresses, so turn that blocking off for the key the app uses; otherwise every email fails with "unrecognised IP address" in the API log.

For reliable delivery to Gmail, Yahoo, and Microsoft inboxes, authenticate the sender domain with DKIM and DMARC in Brevo. The `vercel.app` domain is managed by Vercel, so it cannot be authenticated by this project's owner. Without a domain you control, Brevo may rewrite the sender address and delivery cannot be guaranteed. Verify delivery to an inbox you control before inviting anyone to register.

## When the API does not start

The API stops during startup, instead of failing later, when a required setting is missing: the database connection, `BREVO_API_KEY` or `SPRING_MAIL_FROM` (with the default Brevo provider), or a signing secret that is too short. On Vercel this shows up as `500 FUNCTION_INVOCATION_FAILED` on every `/api` request while the page itself still loads. Without the `MYSQL_*`/`TIDB_*` variables the API tries `localhost:4000` and stops with "Communications link failure". Check the variable names in **Settings → Environment Variables** for the Production environment and redeploy after changing them, because running deployments keep their old values.

The database named in `MYSQL_DATABASE` must exist; Flyway creates the tables in it. On a new TiDB Cloud Starter cluster, run `CREATE DATABASE teambase;` once in the SQL editor.

## Cold starts

Vercel stops the API container after a few idle minutes. The next request waits while it starts again, typically 15–30 seconds. The frontend handles this: it pings `GET /api/ping` as soon as a page loads, and before any API request after a quiet minute it waits for that ping (up to 90 seconds) before the request's own timeout starts. While the wait is noticeable, the app shows "Starting the server…". The container's JVM flags in `backend/Dockerfile.vercel` favour fast startup over peak throughput.

## Demo workspaces

With `APP_DEMO_ENABLED=true`, every "Try the live demo" click creates a generated account (`demo+<id>@example.invalid`, random password, never shown), a team led by that account, a survey and 18 fictional responses, and signs the browser in for up to 24 hours. Expired workspaces are deleted when the next demo starts and by an hourly job inside the API (`APP_DEMO_CLEANUP_INTERVAL_MS`).

### Limits

Anyone can start a demo and use its token, so the API limits what demo visitors can create. When a limit is reached, the API answers with a problem detail whose `code` names the limit (and `limit` gives its value); the app shows a translated message. Answers with status 429 also carry a `Retry-After` header.

| Limit | Default | Variable | Answer |
| --- | --- | --- | --- |
| Demo starts per client address and hour, per API instance | 20 | `APP_DEMO_MAX_SESSIONS_PER_ADDRESS_PER_HOUR` | 429, `demo.limit.address` |
| Demo starts per hour, across all instances | 100 | `APP_DEMO_MAX_SESSIONS_PER_HOUR` | 429, `demo.limit.hourly` |
| Demo workspaces at the same time | 300 | `APP_DEMO_MAX_LIVE_WORKSPACES` | 429, `demo.limit.capacity` |
| Surveys per workspace, the seeded one included | 10 | `APP_DEMO_MAX_SURVEYS_PER_WORKSPACE` | 403, `demo.limit.surveys` |
| Participation tokens per demo survey, renewed ones included | 20 | `APP_DEMO_MAX_TOKENS_PER_SURVEY` | 403, `demo.limit.tokens` |

- **Per address:** the client address comes from `x-vercel-forwarded-for` (set by Vercel), then `X-Real-IP` (set by the nginx container), then the connection's address. The count is kept in memory, so it is per instance and resets when the container restarts. It tracks at most `APP_DEMO_TRACKED_ADDRESSES` addresses (default 10,000); when that many are active, the address seen least recently is forgotten. New visitors are never turned away because many other addresses were seen.
- **Per hour and at the same time:** both are counted in the database, so they hold across instances and restarts. Every demo account in the database counts as a workspace, including expired ones that cleanup has not removed yet: if cleanup falls behind, new demos stop instead of the tables growing. Starts are not serialised, so a burst of simultaneous starts can pass these two limits by the number of requests in flight.
- **Per workspace:** checked in the transaction that writes the survey or token, with the workspace's team row locked, so simultaneous requests cannot pass them. Each token submits at most one response, so the token limit also limits responses. With the defaults, a workspace holds at most about 1,600 rows (10 surveys with 50 questions, 200 tokens, 218 responses with 1,090 answers), so 300 workspaces stay below 500,000 rows.
- **For every account:** survey titles are limited to 300 characters and questions to 1,000 (the column lengths); longer input gets 400 instead of a database error.
- Refreshing a demo session replaces its refresh token row instead of keeping a revoked one, so refreshing in a loop does not add rows.
- Addresses of the form `demo+…@example.invalid` cannot be registered, so the live-workspace limit cannot be filled through registration.

### Switching the demo off

With `APP_DEMO_ENABLED=false`, `POST /api/demo/session` returns 404 and existing demo sessions end: access tokens of demo accounts no longer sign anyone in, and their refresh tokens are refused and revoked. The hourly cleanup keeps running, so the remaining workspaces are deleted 24 hours after they were created. Switching the demo back on does not revive sessions whose refresh token was refused in the meantime.

## Data and project scope

This is a portfolio and learning project, not a production-audited service. Do not collect confidential team feedback or real organizational survey data through a public portfolio deployment. Review privacy, retention, monitoring, backup, and account-recovery needs before using it with real teams.
