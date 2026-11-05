# Contributing

## Local prerequisites

- Java 21 (Maven Wrapper is checked in under `backend/`).
- Node.js 22 and npm for the Angular application.
- Docker Compose v2 for the PostgreSQL-backed integration test and full local stack.

## Change workflow

1. Keep changes focused and preserve the modular monolith boundaries.
2. Run `cd backend && ./mvnw test`.
3. Run `cd frontend && npm ci && npm run lint && npm test -- --watch=false && npm run build`.
4. For a full local smoke test, run `docker compose up --build` and follow the README walkthrough.
5. To verify the browser workflow, install Chromium with `npx playwright install chromium` from `frontend`, start Compose, and run `npm run test:e2e`.
6. Clearly label simulated work and include the verification evidence actually produced.

Do not commit credentials, real `.env` files, generated build output, or host-execution endpoints. Do not add a real provider, remote deployment, or automatic merge without a separate security/permissions design and human approval.
