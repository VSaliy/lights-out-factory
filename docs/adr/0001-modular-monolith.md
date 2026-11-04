# ADR 0001: Start as a modular monolith

- Status: Accepted
- Decision: Keep the MVP in one Spring Boot process with packages organized by workflow capability and PostgreSQL as the single durable store.
- Context: The required first slice is a short human-approved workflow, not a distributed scheduler. Separate services would add deployment and consistency costs before scale or isolation needs exist.
- Consequences: Transactions and local tests are simple. Package boundaries and replaceable execution interfaces should be preserved as future adapters are added. This does not claim tenant isolation or horizontal task scheduling.
