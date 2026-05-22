# Issue 54 Spring Security Tenant Authorization

## Context

Chapter 10 needed a Spring Security multi-tenant example where the selected
tenant is authorized from authenticated identity, not trusted from
`X-Tenant-ID` alone.

## Decision

Add a dedicated Spring Web example module that accepts three demo credential
sources: JWT bearer token, API key, and demo session header. The module rejects
mixed credential sources, resolves the authenticated tenant before selector
validation, binds `TenantContext` only after tenant match, and clears the
context in `finally`.

Architecture diagrams remain PNG-first in README files, with SVG sources stored
under `docs/images/readme-diagrams/`.

## Outcome

The new module includes English/Korean README files, architecture and request
flow PNG diagrams, selected examples CI wiring, and 30 tests covering auth,
tenant mismatch, malformed claims/selectors, cross-tenant isolation, context
cleanup, rollback, registry lifecycle, and source-text architecture guards.

## Verification

- `./gradlew :06-spring-security-tenant-authorization-spring-web:build --stacktrace --continue --console=plain`
- `actionlint .github/workflows/examples.yml`
- `git diff --check`
- README scan confirmed no Mermaid in the new module README files.
- Diagram assets were rendered as 1280x760 RGB PNGs and visually inspected.
- Claude advisor/code-review artifacts reached `P0=0, P1=0`, final artifact:
  `.omx/artifacts/claude-issue-54-code-review-final-stdin-6min-20260523013657.md`.

## Future Guidance

For security examples, review health endpoint bypass behavior against both
custom filters and `SecurityFilterChain` authorization rules. When demo auth
fixtures are intentionally insecure, make the production boundary explicit in
KDoc and README text.
