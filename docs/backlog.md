# Backlog

| Priority | Work item | Dependencies | Acceptance criteria / verification |
|---|---|---|---|
| P1 | Add authentication and authorization before any shared deployment | Identity and threat-model decision | Unauthenticated remote requests are rejected; tests prove least-privilege approval and execution; local demo remains explicitly separate. |
| P1 | Add isolated execution harness design and prototype | `AgentExecutor` boundary; security review | A disposable worktree/container has no host Docker socket, allow-listed tools, resource/time/network limits, bounded artifact collection, and cleanup tests. |
| P1 | Improve workflow transitions and failure coverage | Existing persisted workflow | Tests cover mock failure/timeout, duplicate approval/execute, concurrency conflict, and restart persistence; every terminal state has correlated evidence. |
| P2 | Add GitHub issue/PR metadata adapter | Official API/permission review; app auth | Import and link metadata only with least privilege; tokenless local demo still starts; no claim of direct Copilot invocation. |
| P2 | Add independent build/test gate adapter | Isolated harness; gate contract | Gate consumes collected artifacts from a real test run, persists reproducible evidence, and blocks completion on failure. |
| P2 | Add durable telemetry retention and per-persistence tracing | Current Micrometer/OTel instrumentation | Metrics survive restarts where required; traces include persistence spans without sensitive payload attributes; sampling and retention are documented and tested. |
| P3 | Add bounded background execution and recovery | Persistence and executor budgets | Runs survive process termination, cancellation and retry are bounded and audited, and metrics reflect persisted outcomes without fabricated token/cost data. |
| P2 | Reassess Angular CLI development-tool advisories | Compatible Angular 21 CLI release | Clear the moderate dev-only `npm audit` findings without an unsupported Angular major upgrade, or document vendor remediation and compensating controls. |
