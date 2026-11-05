# Testing

## Backend

```sh
cd backend
./mvnw test
```

The suite includes unit tests for workflow decisions and a PostgreSQL Testcontainers integration test for migrations, persistence, REST behavior, execution, and metrics. It also runs an ArchUnit rule for the API/persistence boundary. Docker must be available for the database integration path.

## Frontend

```sh
cd frontend
npm ci
npm run lint
npm test -- --watch=false
npm run build
```

`npm run lint` checks Prettier formatting. Vitest tests Angular behavior and HTTP error handling.

## Browser workflow

Start the full stack with `docker compose up --build -d --wait`. Install Chromium once with `npx playwright install chromium` from `frontend`, then run `npm run test:e2e`. The browser test creates a fresh work item, plans and approves it, runs the mock, and checks its simulated evidence and audit trail. Stop the stack with `docker compose down`.

The E2E and quality gate test simulated output only; neither validates source code produced by an external agent.
