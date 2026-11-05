# Local setup

## Docker Compose

Install Docker Engine/Desktop with Compose v2, then run:

```sh
docker compose up --build
```

The UI is at `http://localhost:4200`, API at `http://localhost:8080`, and PostgreSQL at `localhost:5432`. The database volume survives container restarts; `docker compose down -v` removes it. Copy `.env.example` to `.env` to override local ports or the demo database password. Do not put real credentials in that file.

## Run services separately

Use Java 21 and PostgreSQL with the local `lightsout` database/user. In `backend`, run `./mvnw spring-boot:run`; set `DATABASE_URL`, `DATABASE_USER`, and `SPRING_DATASOURCE_PASSWORD` if needed. Flyway applies the schema.

Use Node.js 22 in `frontend`: run `npm ci` and `npm start`. The development-server proxy forwards `/api` and `/actuator` to the backend on port 8080. Windows users can run these commands from WSL2 Ubuntu with Docker Desktop WSL integration. IntelliJ IDEA can import `backend/pom.xml` using a Java 21 SDK and the checked-in Maven Wrapper.

## Optional observability

Follow the README's optional profile command to start the OTel Collector, Prometheus, and Grafana. They are unnecessary for normal application use. The collector debug exporter emits spans to collector logs; Prometheus exposes the local metrics UI on port 9090 and Grafana on port 3000.
