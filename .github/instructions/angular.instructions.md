---
applyTo: "frontend/src/**/*.ts"
---

# Angular and TypeScript conventions

- Use Angular 21 standalone components, strict TypeScript, typed reactive forms, and injected typed `HttpClient` services.
- Keep UI states explicit: loading, empty, error, disabled/in-flight, simulated output, quality evidence, and audit history.
- Provide accessible labels, keyboard-accessible controls, and responsive contrast.
- Derive dashboard counts from API data; never invent operational statistics.
- Add component/service tests for behavior changes and run `npm test -- --watch=false` and `npm run build`.
