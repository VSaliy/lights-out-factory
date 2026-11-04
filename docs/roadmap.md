# Roadmap

This repository implements a local subset of P0–P3. Everything else below is planned, not present as a working integration.

- **P0 Bootstrap (partial):** Spring/Angular builds, Compose, PostgreSQL, Flyway, Actuator health, CI, and local setup docs.
- **P1 Work management (partial):** work-item persistence, API, validation, and UI. Project metadata and external references remain future work.
- **P2 Orchestration (partial):** one-task deterministic plan, explicit approval, state checks, and mock execution. No DAG, background queue, retry, or cancellation.
- **P3 Verification (partial):** mock-output provenance gate, correlated audit history, and persisted statuses. No external build/test runner or broad metrics.
- **P4 External integration (future):** GitHub Issues/PR metadata via documented APIs and least-privilege app credentials. No generic Copilot invocation endpoint is assumed.
- **P5 Real provider execution (future):** documented agent adapter, isolated ephemeral worktree/container, explicit tool allow-list, credentials isolation, network limits, budgets/timeouts, cancellation, artifact collection, prompt-injection defenses, and cleanup. Check each provider's current official APIs and permissions first.
- **P6 Multi-agent coordination (future):** DAG dependencies, bounded concurrency, retries, quotas/budgets, and human escalation.
- **P7 Controlled autonomy (future):** independent validation, risk policy, monitored deployment workflows, and evaluations/feedback. Protected merges and production deployments remain human-approved.

Runtime roles such as analyst, planner, developer, test engineer, security reviewer, and DevOps engineer are conceptual only; they are not autonomous agents in this MVP.
