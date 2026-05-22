# Issue 62 Observability Readiness

## Context

Issue #62 asked for paired Spring Boot 4 and Ktor examples that demonstrate
operational diagnostics for database-backed services.

## Decision

Add two focused chapter 12 modules instead of folding readiness into the
existing Ktor architecture baseline:

- `09-spring-observability-readiness`
- `10-ktor-observability-readiness`

Spring uses Actuator readiness groups and a custom database health contributor.
Ktor uses an explicit `/readyz` route with `CallId`, `CallLogging`, and
`StatusPages`.

## Outcome

Both modules persist slow-operation diagnostics through Exposed JDBC, sanitize
and propagate `X-Request-ID`, return structured errors, and test degraded
database readiness with in-process state.

README architecture sections use generated PNG diagrams under
`docs/images/readme-diagrams/` with SVG sources kept next to them.

The repository guidance now requires every example README to include an
Architecture Diagram PNG/SVG pair. Add class, sequence, ERD, or other UML-style
diagrams when the example has relationships or flows that are not obvious from
the architecture diagram alone.

## Verification

```bash
./gradlew :09-spring-observability-readiness:test :10-ktor-observability-readiness:test --stacktrace --continue
```

Result after the Claude review fixes: Spring 6 tests passing, Ktor 6 tests
passing with `--rerun-tasks`.

README diagram links were checked for missing local PNG targets and Mermaid
residue after the image links were added.

After rebasing on current chapter 12, the modules were renumbered to 09/10,
the chapter English/Korean index README files were updated, and the regenerated
PNG diagrams were visually opened to confirm the title and labels match 09/10.
The final chapter README scan reported `readmes=20`, `missingPng=0`,
`missingArchitecture=0`, `mermaidResidue=0`, and `missingFiles=0`.
`./gradlew projects --quiet` listed both new modules, and `git diff --check`
passed.

Claude 6-Tier review found one P1 and several P2/P3 issues before PR creation:
Spring method-level parallel tests shared `DiagnosticsState`, request IDs were
echoed without validation, Spring readiness allowed repository exceptions to
escape, and Ktor schema initialization could be marked complete before DDL
success. The rerun also flagged Spring framework 4xx errors and unexpected
exceptions in the catch-all handler; those now return structured 400 responses
or log server-side before returning a sanitized 500. These were fixed and
covered by the final targeted test run.

Final Claude 6-Tier rerun artifact:
`.omx/artifacts/claude-issue-62-code-review-6tier-stdin-6min-20260522184325.md`.
It reported `P0=0`, `P1=0`, `P2=0`, `P3=2`, verdict `PASS`.

## Future Agents

Spring Boot 4 health classes live under `org.springframework.boot.health.*`, not
the Spring Boot 3 `org.springframework.boot.actuate.health.*` package.

Do not echo caller-supplied correlation headers directly in examples. Cap and
sanitize them before adding them to response headers, logs, or persisted rows.
