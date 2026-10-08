# Team Analyzer

A full-stack team-survey application built to demonstrate a structured Spring Boot and Vue architecture, role-aware workflows, token-based participation, and a reproducible local environment.

## Project overview

Team Analyzer lets team leaders create surveys, invite participants through shareable token links, and review or export the resulting data. Participants can answer public surveys without creating an account. Authentication is available for management areas, with role-based access for administrators, leaders, and members.

This is a portfolio and learning project. It is not presented as a production-audited service.

## Interactive demo

The Vercel demo runs the Vue application from `frontend/` and routes `/api` to the Spring Boot service in `backend/`. Visitors can start a private demo workspace with a seeded team, an editable survey, and 18 fictional responses. Each workspace is isolated and expires after 24 hours. The older static mock-up is retained under `demo/` as design reference only.
## What it demonstrates

- A Java 21 / Spring Boot API with Spring Security, JPA, and Flyway migrations.
- A Vue 3 and TypeScript single-page frontend.
- Registration, email verification, JWT access tokens, and refresh-cookie flows.
- Role-aware survey management and public participation links.
- JSON result exports protected by signed download links.
- Local service orchestration with Docker Compose.

## Technology

| Area | Technology |
| --- | --- |
| Backend | Java 21, Spring Boot 3, Spring Security 6, Hibernate/JPA |
| Data | MySQL, H2 for development, Flyway |
| Frontend | Vue 3, TypeScript, Vite |
| Local environment | Docker Compose, Nginx, phpMyAdmin, SonarQube |

## Architecture at a glance

- **backend/** contains the REST API, security and domain services, and database migrations.
- **frontend/** contains the Vue application, API client, route guards, and views.
- **docker/** contains the Compose definition, backend and frontend images, and Nginx configuration.

## Run locally

The Compose stack is intended for local development. It binds its host ports to 127.0.0.1.

1. Copy .env.production.example to .env.production.
2. Set distinct values for every empty password and signing-secret entry. Keep .env.production local; it is ignored by Git.
3. Generate fresh values rather than reusing any value previously published in this repository. For example:
   - openssl rand -base64 32 for APP_JWT_SECRET_BASE64.
   - openssl rand -hex 32 for each HMAC secret and database password.
4. Start from the repository root:

```bash
docker compose --env-file .env.production -f docker/docker-compose.yml up --build
```

The local services are:

| Service | Address |
| --- | --- |
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080 |
| phpMyAdmin | http://localhost:8082 |
| SonarQube | http://localhost:9000 |

MySQL is available to the host at localhost:3306 for local tooling. The database, application, SonarQube, and phpMyAdmin ports are not published on external interfaces by this Compose file.

Stop the stack while preserving its database volumes:

```bash
docker compose --env-file .env.production -f docker/docker-compose.yml down
```

Changing the environment file does not reinitialize an existing MySQL or SonarQube database volume. Keep the original database credentials for existing volumes, or intentionally reset disposable local data after making any backup you need.

## Registration and local verification

There are no pre-created demo accounts or seeded administrator credentials. Create an account through the registration flow. The project currently has no mail server configured in its local stack, so newly registered accounts need email verification before login.

For a disposable local demo only, set LOG_VERIFICATION_LINK=true in your local .env.production file to print verification links to the backend log. Those links contain one-time tokens and the logs may include personal data. Keep this option disabled outside a local environment, and do not share or retain logs produced while it is enabled. The default is disabled.

Administrator and leader roles are not granted automatically. Configure roles only in a local database that you control.

## Automated checks

GitHub Actions runs the backend Maven verification suite and the frontend lint, Cypress component tests, and production build on pushes to main and pull requests.

To run the frontend checks locally:

```bash
cd frontend
pnpm install --frozen-lockfile
pnpm lint
pnpm test:component
pnpm build
```

The backend integration tests use Testcontainers and require a working Docker runtime:

```bash
cd backend
./mvnw --batch-mode verify
```

These checks provide a repeatable baseline; they do not replace a security review or demonstrate production operation.

## Configuration

backend/src/main/resources/application.yml defines the Spring profiles and service defaults. The local Compose stack reads database credentials and application secrets from .env.production. The tracked example contains empty secret fields intentionally; fill them with fresh values before starting the services.

The `prod` Spring profile enables email delivery and requires `SPRING_MAIL_HOST`, `SPRING_MAIL_USERNAME`, and `SPRING_MAIL_PASSWORD`. `SPRING_MAIL_PORT` defaults to 587. Keep these values in the deployment environment's secret store; do not add them to the repository. Local development and automated tests do not require a real SMTP account.

The application uses separate secrets for JWT signing, email-verification tokens, and signed download links. Use a different random value for each purpose. Do not reuse values from commit history.

## Security and project scope

- The Compose file is for local development and publishes ports only on loopback.
- The local stack is not a production deployment template. Production operation needs environment-specific TLS, mail delivery, secret management, access controls, backups, monitoring, and operational review.
- Do not use real or reused credentials in this demo.
- Old database volumes may still contain demo accounts created by earlier versions. Removing the seed migration does not delete data already present in a volume; inspect and clean any disposable local database separately.
- Verification links are not logged by default. Enabling the local-only option exposes one-time tokens to anyone with access to the container logs.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE). The license applies to the original project code; third-party dependencies and other third-party materials retain their own licenses and terms.

## Further development

Useful next steps for this portfolio project are broader automated coverage around authentication and authorization, an explicit production deployment guide, and operational monitoring.


## Vercel full-stack deployment

The root `vercel.json` composes the Vite frontend and the backend container. Configure the Vercel project with the repository `fabsGitHub/team-analyzer-portfolio`, root directory `.`, and these server-only environment variables:

- `SPRING_PROFILES_ACTIVE=prod`
- `MYSQL_HOST`, `MYSQL_PORT=4000`, `MYSQL_DATABASE`, `MYSQL_USER`, and `MYSQL_PASSWORD` for TiDB Cloud SQL
- `APP_JWT_SECRET_BASE64`, `EMAIL_VERIFY_HMAC_SECRET`, and `DOWNLOAD_TOKEN_HMAC_SECRET`
- `APP_FRONTEND_BASE_URL` set to the demo's canonical HTTPS origin
- `APP_DEMO_ENABLED=true`
- `APP_MAIL_PROVIDER=brevo-api`, `BREVO_API_KEY`, and `SPRING_MAIL_FROM` for a verified Brevo sender

Keep credentials in Vercel encrypted environment variables. The TiDB Cloud management API key is not a MySQL/JDBC password. The old `demo/` directory is not the Vercel root.
