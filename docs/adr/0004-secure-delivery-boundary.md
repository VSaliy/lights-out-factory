# ADR 0004: Keep delivery actions human-controlled

- Status: Accepted
- Decision: Bind development ports to loopback, use no fake authentication, accept no host commands, and require explicit human actions for approval and completion. Do not implement merge or deploy actions.
- Context: The local demo has no production identity, sandbox, or policy service and must not imply those controls exist.
- Consequences: Do not expose this service to shared networks. Before deployment, add real authentication/authorization, isolation and credential controls, restricted network and tool access, and human approval for protected or irreversible actions.
