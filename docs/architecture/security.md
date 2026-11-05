# Security posture and limitations

## Implemented safeguards

- The demonstration binds published service ports to loopback by default.
- Database credentials are configurable through environment variables; the checked-in password is a local-demo default, not a production secret.
- The mock executor accepts a bounded scenario enum and performs no network, shell, or repository operations.
- Work-item and executor output are treated as task data; audit records store event metadata rather than payloads.
- Illegal workflow transitions are rejected; plan approval is required before execution.
- Actuator exposes health only by default. The optional profile enables Prometheus metrics solely for local monitoring.

## Not implemented

There is no authentication or authorization, multi-tenant isolation, secret management, real agent sandbox, or remote deployment. Do not expose this demo on a shared network or use it for sensitive work. Before adding a real executor, define an isolated harness with least-privilege tools, resource/time/network limits, bounded artifact collection, and cleanup. Human review remains mandatory for code integration and delivery.
