# ADR 0002: Persist deterministic workflow state

- Status: Accepted
- Decision: Store work-item and plan states and make each transition conditional on the persisted current state. Reject duplicate/illegal requests with HTTP 409.
- Context: Approval must precede execution, and a failed run or quality gate must never appear successful.
- Consequences: State changes, run evidence, and audit facts are persisted in a transaction. The mock path is synchronous and short; no distributed exactly-once semantics are claimed.
