# Lights-Out Factory

Lights-Out Factory is a local, supervised workflow demo: create a work item, draft and approve a plan, run a deterministic mock executor, and inspect its independent quality-gate result and audit history.

> **Simulation only:** the `mock` provider does not invoke an AI service, edit another repository, run shell commands, or compile/test generated code. This repository is a local development demonstration, not a production deployment.

## Five-minute walkthrough

Requirements: Docker with Compose v2. Java 21 and Node.js 22 are needed only when running the backend or frontend outside containers.

1. Start PostgreSQL, the Spring API, and Angular-served UI:
   ```sh
   docker compose up --build
   ```
2. Open [http://localhost:4200](http://localhost:4200). The API health check is [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health).
3. Create a work item:
   - Title: `Add an application health endpoint`
   - Description: `Implement an endpoint that returns a documented health status.`
   - Acceptance criteria: `HTTP 200; defined response contract; automated test.`
4. Choose **Create plan**, then **Approve plan**. The execution endpoint rejects unapproved plans.
5. Choose **Execute mock**. The UI displays the `SIMULATED` provider label, persisted output, quality-gate result, and correlated audit events. A passed gate moves the work item to review; **Mark complete** is a separate human action.
6. Stop the stack with `Ctrl+C`, or run `docker compose down`. The named PostgreSQL volume persists; `docker compose down -v` deletes it.

Ports are bound to loopback by default. To override local settings, copy `.env.example` to `.env`; do not commit real credentials.

## API

The checked-in OpenAPI 3 contract is [`docs/openapi.yaml`](docs/openapi.yaml). Read it locally or load it into an OpenAPI viewer; no Swagger UI service is bundled.

```sh
curl -sS -X POST http://localhost:8080/api/v1/work-items \
  -H 'Content-Type: application/json' \
  -d '{"title":"Health endpoint","description":"Expose an application health status","acceptanceCriteria":"HTTP 200 and a documented contract","priority":"P2"}'
```

The response includes the work-item UUID. Use it with:

- `POST /api/v1/work-items/{id}/plans`
- `POST /api/v1/plans/{id}/approve` with `{"actor":"local-demo"}`
- `POST /api/v1/plans/{id}/execute` with `{"providerType":"mock","scenario":"SUCCESS"}`
- `GET /api/v1/work-items/{id}/audit-events`
- `GET /api/v1/executions/{id}`

The mock accepts `SUCCESS`, `FAILURE`, and `TIMEOUT` scenarios. These are deterministic simulated outcomes, not time-bounded execution or real-agent behavior. Illegal workflow transitions return RFC 9457 Problem Details with HTTP 409; request validation errors return HTTP 400. Each request receives an `X-Correlation-ID`, reused in execution and audit records.

## Development and verification

### Backend

```sh
cd backend
./mvnw test
./mvnw spring-boot:run
```

The local API expects PostgreSQL at `localhost:5432` with database/user `lightsout`. Set `DATABASE_URL`, `DATABASE_USER`, and `SPRING_DATASOURCE_PASSWORD` as needed, or use Compose. Flyway applies migrations; Hibernate validates the schema.

On Windows 11, use WSL2 Ubuntu for the shell commands above, with Docker Desktop's WSL integration enabled. In IntelliJ IDEA, open `backend/pom.xml` as a Maven project and select a Java 21 SDK; the checked-in wrapper avoids a global Maven install.

### Frontend

```sh
cd frontend
npm ci
npm test -- --watch=false
npm run build
npm start
```

The dev-server proxy sends `/api` and `/actuator` to `localhost:8080`; Compose serves the built SPA and reverse-proxies those paths through the UI origin.

## Boundaries and security

- The backend is a modular monolith organized into API, work-item, planning, agent, workflow, verification, execution, and audit capabilities.
- Only the `mock` provider is registered. `AgentExecutor` is the extension boundary; unsupported providers are rejected.
- Verification checks only that a successful mock result carries its expected simulated-output provenance. It is not evidence that external code builds or passes tests.
- PostgreSQL records workflow state, gate evidence, and sanitized audit facts. The synchronous mock is bounded by doing no network or host work; the current single-transaction flow does not claim distributed exactly-once execution.
- This local demo has **no authentication**. Do not expose it to shared networks. Before shared/production use, add authentication and authorization, isolated per-run harnesses, credential/network/resource controls, audit retention policy, and human approval for merges, deployments, destructive changes, or elevated access.
- Issue text and agent output are untrusted task data. No endpoint accepts shell commands. There is no auto-merge, deployment, or real GitHub/Copilot integration.
- At implementation time, `npm audit --omit=dev` reports no production dependency advisories. The full audit reports 16 moderate findings in Angular CLI development-tool dependencies; its automated fix requires Angular CLI 22, outside the required Angular 21 line. This is tracked in the backlog rather than applying a breaking major upgrade.

## Architecture and planned work

The implemented boundary and state flow are in [`docs/architecture/overview.md`](docs/architecture/overview.md). Decision records, roadmap, and a short prioritized backlog are in [`docs/adr/`](docs/adr/), [`docs/roadmap.md`](docs/roadmap.md), and [`docs/backlog.md`](docs/backlog.md). The API's OpenAPI document describes the implemented endpoints; planned integrations are explicitly future work.

## CI

GitHub Actions runs Maven tests (including the PostgreSQL Testcontainers path when Docker is available), Angular tests, and an Angular production build. See [`CONTRIBUTING.md`](CONTRIBUTING.md) for local prerequisites and contribution guidance.
