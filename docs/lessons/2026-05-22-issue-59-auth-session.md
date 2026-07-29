# Issue 59 Auth session 예제

## 배경

Issue #59는 12장에 paired Spring Boot 4/Ktor authentication/session 예제를 요구했다.
사용자는 모든 예제 README에 Architecture Diagram PNG도 포함해야 한다고 요구했다.

## 결정

두 예제는 작고 비교 가능하게 유지한다.

- Spring Boot는 `SecurityFilterChain`, repository-backed `UserDetailsService`, BCrypt,
  MVC controller endpoint를 사용한다.
- Ktor는 `Authentication`, `Sessions`, service-level role check, repository-owned
  `Dispatchers.IO` Exposed transaction을 사용한다.
- 두 모듈은 같은 `alice`, `admin` account를 seed하고 Exposed를 통해 session metadata를
  H2에 저장한다. Session row는 token hash와 expiry를 저장하며 raw token은 생성 시점에만
  반환한다.

## 결과

`12-production-integration` 아래 `05-spring-auth-session`과 `06-ktor-auth-session`을
추가하고, English/Korean README file과 `docs/images/readme-diagrams` 아래 PNG architecture
diagram을 함께 두었다. 완료된 예제 01-08을 link하고 PNG Architecture Diagram 요구사항을
chapter level에서 명시하도록 12장 English/Korean index README를 갱신했다.

## 검증

- `./gradlew :05-spring-auth-session:test :06-ktor-auth-session:test --stacktrace --continue`
  passed in 20s after the final stateless-session and cookie-hardening pass:
  Spring 5 tests, Ktor 8 tests.
- README PNG check for the two new modules: `missing=0`,
  `missingArchitecture=0`, `mermaidResidue=0`.
- IntelliJ diagnostics: build error 0건. 변경된 Ktor repository file은 열려 있지 않아
  stale file analysis 상태였지만 `problemCount 0`을 보고했다.
- Claude 6-Tier stdin review rerun은 token-storage P1을 찾았고, session token hashing,
  one-hour expiry 추가, list response에서 raw token 숨김으로 수정했다.
- 이후 Claude 6-Tier stdin review는 Spring Security가 여전히 parallel `HttpSession`을
  만들 수 있음을 찾았다. 최종 코드는 `SessionCreationPolicy.STATELESS`를 설정하고 Ktor
  cookie를 one-hour session row TTL 및 `SameSite=Lax`와 맞춘다.
- Chapter 12 diagram-image correction 위로 rebase한 뒤 chapter README scan은
  `readmes=16`, `missingPng=0`, `missingArchitecture=0`, `mermaidResidue=0`,
  `missingFiles=0`을 보고했다.

## 향후 참고

향후 예제 모듈에서는 최종 README 검증 전에 Architecture Diagram PNG를 먼저 만든다. 추가
UML diagram은 architecture diagram이 명확히 설명하지 못하는 flow나 contract가 있을 때만
사용한다.
