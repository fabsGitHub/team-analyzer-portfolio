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
| `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD` | MySQL-compatible database connection. `MYSQL_PORT` defaults to `3306`; SSL identity verification is enabled by default. |
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
| `APP_DEMO_ENABLED` | `true` enables "Try the live demo" (`POST /api/demo/session`): each call creates a private sample workspace that is deleted after 24 hours. Defaults to `false`. |
| `PORT=8080` | Vercel container port. |

Generate new, separate signing secrets for each deployment. Do not reuse any value from this repository's history. Use Vercel's environment-variable secret handling for passwords and signing keys.

Use a managed MySQL-compatible database with TLS enabled and a database/user created for this app. Flyway runs the migration in `backend/src/main/resources/db/migration` when the backend first connects. Do not point preview deployments at a database containing production or personal data.

## First administrator

No administrator account or default password is seeded. Before registering the account you control, set `APP_BOOTSTRAP_ADMIN_EMAIL` to that exact email address in the production environment. Register through the app and complete email verification. The backend assigns `ROLE_ADMIN` only to that address, and only when it is confirmed with the mailbox link together with the account's password (or through a completed password reset), so a registration of the address by someone else can never become administrator. Remove `APP_BOOTSTRAP_ADMIN_EMAIL` from the Vercel environment after the initial account is provisioned; the stored role remains in the database.

Email verification depends on a real transactional email provider. Do not set `LOG_VERIFICATION_LINK=true` on a public deployment.

### Brevo Free transactional email

In Brevo, create and verify a sender, then generate an API key from SMTP & API settings. Configure `APP_MAIL_PROVIDER=brevo-api`, `BREVO_API_KEY`, `SPRING_MAIL_FROM` (the verified sender), and optionally `SPRING_MAIL_FROM_NAME`. Keep the API key only in Vercel's environment variables. The app sends verification and password-reset messages through Brevo's transactional email API. To use SMTP instead, set `APP_MAIL_PROVIDER=smtp` and provide the SMTP host, username, SMTP key, and port. SMTP connection, read, and write timeouts and the HTTP timeouts for the Brevo API (5 s connect, 10 s read) are capped, so a stalled provider cannot leave signup or reset requests waiting indefinitely.

For reliable delivery to Gmail, Yahoo, and Microsoft inboxes, authenticate the sender domain with DKIM and DMARC in Brevo. The `vercel.app` domain is managed by Vercel, so it cannot be authenticated by this project's owner. Without a domain you control, Brevo may rewrite the sender address and delivery cannot be guaranteed. Verify delivery to an inbox you control before inviting anyone to register.

## Demo workspaces

With `APP_DEMO_ENABLED=true`, every "Try the live demo" click creates a generated account (`demo+<id>@example.invalid`, random password, never shown), a team led by that account, a survey and 18 fictional responses, and signs the browser in for up to 24 hours. Expired workspaces are deleted when the next demo starts and by an hourly job inside the API (`APP_DEMO_CLEANUP_INTERVAL_MS`). Each API instance allows 20 demo starts per client IP address and hour; the limit is kept in memory, so it resets when the container restarts and is not shared between instances.

## Data and project scope

This is a portfolio and learning project, not a production-audited service. Do not collect confidential team feedback or real organizational survey data through a public portfolio deployment. Review privacy, retention, monitoring, backup, and account-recovery needs before using it with real teams.
