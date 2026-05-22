# Issue 59 Auth Session Examples

## Context

Issue #59 asked for paired Spring Boot 4 and Ktor authentication/session examples
in chapter 12. The user also required every example README to include an
Architecture Diagram PNG.

## Decision

Keep both examples small and comparable:

- Spring Boot uses `SecurityFilterChain`, repository-backed `UserDetailsService`,
  BCrypt, and MVC controller endpoints.
- Ktor uses `Authentication`, `Sessions`, service-level role checks, and
  repository-owned `Dispatchers.IO` Exposed transactions.
- Both modules seed the same `alice` and `admin` accounts and persist session
  metadata in H2 through Exposed. Session rows store token hashes and expiry;
  raw tokens are returned only at creation time.

## Outcome

Added `05-spring-auth-session` and `06-ktor-auth-session` under
`12-production-integration`, with English/Korean README files and PNG
architecture diagrams under `docs/images/readme-diagrams`. Updated the chapter
12 English/Korean index README files so completed examples 01-08 are linked and
the PNG Architecture Diagram requirement is explicit at the chapter level.

## Verification

- `./gradlew :05-spring-auth-session:test :06-ktor-auth-session:test --stacktrace --continue`
  passed in 20s after the final stateless-session and cookie-hardening pass:
  Spring 5 tests, Ktor 8 tests.
- README PNG check for the two new modules: `missing=0`,
  `missingArchitecture=0`, `mermaidResidue=0`.
- IntelliJ diagnostics: build errors 0; changed Ktor repository file reports
  problemCount 0 with stale file analysis because the file is not open.
- Claude 6-Tier stdin review rerun found token-storage P1s, which were fixed
  by hashing session tokens, adding one-hour expiry, and hiding raw tokens from
  list responses.
- A later Claude 6-Tier stdin review found that Spring Security could still
  create a parallel `HttpSession`; the final code sets `SessionCreationPolicy.STATELESS`
  and aligns the Ktor cookie with the one-hour session row TTL plus `SameSite=Lax`.
- After rebasing on the chapter 12 diagram-image correction, chapter README scan
  reported `readmes=16`, `missingPng=0`, `missingArchitecture=0`,
  `mermaidResidue=0`, and `missingFiles=0`.

## Future Notes

For future example modules, create the Architecture Diagram PNG before final
README verification. Use additional UML diagrams only when the example has a
flow or contract that the architecture diagram does not explain clearly.
