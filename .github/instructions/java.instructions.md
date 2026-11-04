---
applyTo: "backend/**/*.java"
---

# Java and Spring conventions

- Use Java 21 and Spring Boot 3.5; keep REST DTOs separate from JPA entities.
- Place workflow logic in capability packages, not controllers. Use explicit transaction boundaries and do not hold them across future long-running execution.
- Persist schema changes through Flyway. Keep `ddl-auto=validate`; use UUID identifiers and optimistic locking where state can race.
- Reject invalid transitions and return safe RFC 9457 Problem Details without stack traces.
- Keep provider calls behind `AgentExecutor`; any new real executor requires an isolation and permission design.
- Test workflow approval, duplicate actions, failure behavior, and independently evaluated gates.
