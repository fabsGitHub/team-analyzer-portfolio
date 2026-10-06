# Team Analyzer

A full-stack survey application for collecting and reviewing structured team feedback. This portfolio snapshot contains the current application source with a fresh Git history; it excludes local environment files, private keys, and the original repository history.

## What it demonstrates

- Java 21 and Spring Boot REST services with Spring Security and JPA
- Vue 3 and TypeScript interface built with Vite
- Team and survey management with role-based access
- Participation through invitation-token links
- Survey response aggregation and JSON export
- MySQL development setup, Flyway migrations, and Docker Compose

## Local setup

No preloaded demo users or passwords are included. Register users through the application.

Requirements: Java 21, Node.js, pnpm, Docker and Docker Compose.

1. Create a local `.env` file and set unique values for `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`, `APP_JWT_SECRET_BASE64`, `EMAIL_VERIFY_HMAC_SECRET`, and `DOWNLOAD_TOKEN_HMAC_SECRET`.
2. Generate each application secret locally with `openssl rand -base64 32`; do not reuse values across variables or environments.
3. Start the services from the repository root:

   ```sh
   docker compose -f docker/docker-compose.yml up --build
   ```

4. Open the frontend at `http://localhost:5173`.

The application is a demo project. Use a managed secret store, TLS, production-grade access controls, and independently generated credentials before any deployment beyond a local development environment.

## Configuration

Secret values are read from environment variables. No credentials or `.env` files belong in the repository. See `backend/src/main/resources/application.yml` and `docker/docker-compose.yml` for the configuration keys.
