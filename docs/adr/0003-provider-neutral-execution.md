# ADR 0003: Keep execution provider-neutral

- Status: Accepted
- Decision: Define `AgentExecutor` and resolve a provider by type; register only `mock` in this MVP.
- Context: An AI model, a provider adapter, and an isolated agent harness are distinct responsibilities. The local workflow must work offline and require no paid token.
- Consequences: New providers must add their own bounded request/result contract, authentication and isolation design, and independent verification. This interface is not a fabricated GitHub Copilot or vendor API.
