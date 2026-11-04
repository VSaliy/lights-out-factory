# Backlog

| Priority | Work item | Dependencies | Acceptance criteria / verification |
|---|---|---|---|
| P1 | Add authentication and authorization before any shared deployment | Identity and threat-model decision | Unauthenticated remote requests are rejected; tests prove least-privilege approval and execution; local demo remains explicitly separate. |
| P1 | Add isolated execution harness design and prototype | `AgentExecutor` boundary; security review | A disposable worktree/container has no host Docker socket, allow-listed tools, resource/time/network limits, bounded artifact collection, and cleanup tests. |
| P1 | Improve workflow transitions and failure coverage | Existing persisted workflow | Tests cover mock failure/timeout, duplicate approval/execute, concurrency conflict, and restart persistence; every terminal state has correlated evidence. |
| P2 | Add GitHub issue/PR metadata adapter | Official API/permission review; app auth | Import and link metadata only with least privilege; tokenless local demo still starts; no claim of direct Copilot invocation. |
| P2 | Add independent build/test gate adapter | Isolated harness; gate contract | Gate consumes collected artifacts from a real test run, persists reproducible evidence, and blocks completion on failure. |
| P3 | Add operational metrics and bounded background execution | Persistence and executor budgets | Metrics reflect real persisted outcomes; no fabricated token/cost data; crashes and cancellation behavior are documented and tested. |
| P2 | Reassess Angular CLI development-tool advisories | Compatible Angular 21 CLI release | Clear the moderate dev-only `npm audit` findings without an unsupported Angular major upgrade, or document vendor remediation and compensating controls. |
