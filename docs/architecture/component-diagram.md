# Component diagram

The Spring application is a modular monolith; packages group behavior by capability. Controllers own HTTP mapping and validation, while workflow decisions remain in application services.

```mermaid
flowchart TB
  api["api\nREST DTOs, validation, errors"]
  work["workitem\nWorkItem + repository"]
  plan["planning\nExecutionPlan + AgentTask"]
  workflow["workflow\ntransitions and orchestration"]
  agents["agents\nAgentExecutor registry + mock"]
  verify["verification\nmock-provenance gate"]
  execute["execution\nExecutionRun"]
  audit["audit\ncorrelated audit events"]
  observe["observability\nmetrics, observations"]
  db[("JPA repositories / PostgreSQL")]
  api --> workflow
  workflow --> work
  workflow --> plan
  workflow --> agents
  agents --> execute
  workflow --> verify
  workflow --> audit
  workflow --> observe
  work --> db
  plan --> db
  execute --> db
  verify --> db
  audit --> db
```

The provider-neutral `AgentExecutor` is the only supported extension seam for execution. There is currently one registered implementation, `mock`. Architecture tests prevent API types from depending directly on persistence repositories.
