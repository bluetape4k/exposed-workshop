# Issue #142 Design - Explicit Ktor Exposed Integration

## Context

Issue #142 asks for a workshop example that introduces the explicit
`bluetape4k-exposed-ktor` integration added for the Exposed 1.11 line. Existing
Ktor examples in chapter 12 intentionally own their transaction boundaries
inside application repositories. This example must instead show the dedicated
helper API so learners can compare the two styles without rewriting the older
modules.

## Target Module

- Directory: `13-ecosystem-integrations/05-ktor-exposed-integration`
- Gradle task: `:05-ktor-exposed-integration:build`
- Default runtime: local in-memory H2 JDBC plus H2 R2DBC readiness probe
- Public title: `Explicit Ktor Exposed Integration`

## Learning Contract

The module demonstrates:

- caller-owned JDBC and R2DBC resources passed to
  `installBluetape4kExposedKtor`;
- helper-provided `/healthz/exposed` and `/readyz/exposed` routes;
- `ApplicationCall.exposedJdbcTransaction` as the blocking JDBC boundary;
- composed Ktor `StatusPages` with `bluetape4kErrorResponses()` and
  `bluetape4kExposedErrors()`;
- sanitized database error responses that do not leak SQL, JDBC URLs, or
  credentials.

## Non-Goals

- Do not replace chapter 10, 11, or 12 Ktor examples.
- Do not introduce real external services, cloud credentials, or network
  prerequisites.
- Do not build a full production service template. Keep the example small and
  focused on the integration helper surface.

## Acceptance Evidence

- Ktor `testApplication` verifies note CRUD through JDBC helper transactions.
- Ktor `testApplication` verifies readiness success for JDBC and R2DBC probes.
- Ktor `testApplication` verifies readiness failure returns `503 Service
  Unavailable` when the caller-owned JDBC resource is unavailable.
- Ktor `testApplication` verifies exposed database errors are structured and
  sanitized.
- `README.md` and `README.ko.md` compare the helper-based approach with older
  hand-owned Ktor examples.
- README diagram assets exist as editable SVG plus rendered PNG under
  `docs/images/readme-diagrams/`.
- Examples workflow includes `:05-ktor-exposed-integration:build`.

