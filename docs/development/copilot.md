# Copilot and agent integration

This repository currently has no Copilot invocation, coding-agent integration, or custom provider implementation. The only executor is a deterministic `mock` that does not edit repositories or run commands.

Future agent providers must implement the existing `AgentExecutor` boundary and be designed for isolated execution, least privilege, bounded resources, safe handling of untrusted issue text, and reviewable artifacts. See ADR [0003](../adr/0003-provider-neutral-execution.md). Do not add a direct endpoint that claims to invoke Copilot; use supported provider mechanisms only after their security and permission model is specified.
