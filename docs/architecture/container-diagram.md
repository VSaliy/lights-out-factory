# Container diagram

```mermaid
flowchart LR
  browser["Browser"]
  ui["Frontend container\nAngular build + Nginx"]
  api["Backend container\nJava 21 / Spring Boot 3.5"]
  db[("PostgreSQL 16")]
  collector["OTel Collector\noptional profile"]
  prometheus["Prometheus\noptional profile"]
  grafana["Grafana\noptional profile"]
  browser -->|"localhost:4200"| ui
  ui -->|"/api and /actuator proxy"| api
  api --> db
  api -. "OTLP traces" .-> collector
  prometheus -. "scrape /actuator/prometheus" .-> api
  grafana --> prometheus
```

`docker compose up --build` starts the three required containers. Monitoring services exist only under the `observability` profile. Services and published ports bind to loopback in the supplied Compose configuration.
