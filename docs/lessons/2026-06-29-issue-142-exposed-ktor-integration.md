# Issue #142 Lesson - Explicit Ktor Exposed Integration

## Context

Issue #142 needed a chapter 13 example for the new
`bluetape4k-exposed-ktor` helper surface without replacing the older chapter 12
Ktor service examples.

## Decision

Keep the module small and local-first:

- Use H2 JDBC for CRUD through `call.exposedJdbcTransaction`.
- Use H2 R2DBC only as a readiness probe backend.
- Compose `bluetape4kErrorResponses()` and `bluetape4kExposedErrors()` in one
  `StatusPages` block.
- Keep HikariCP, R2DBC pool, and dispatcher lifecycle caller-owned.

## Outcome

The module shows the helper boundary directly and keeps chapter 12 focused on
production-service structure. The README diagram needed an extra rendered-PNG
inspection pass: SVG audits passed before the resource-lane connector corridor
was visually clean.

## Future Guidance

For future Ktor/Exposed workshop examples, use the helper when the lesson is
readiness, sanitized errors, or dispatcher-aware transaction wiring. Keep
hand-owned transaction examples in chapter 12 when the lesson is layered
application architecture.
