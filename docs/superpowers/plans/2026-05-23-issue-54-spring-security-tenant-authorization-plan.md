# Issue 54 Spring Security tenant authorization 계획

## 범위

Tenant database를 선택하기 전에 tenant를 authorize하는 Spring MVC + Spring Security + Exposed JDBC
workshop module `10-multi-tenant/06-spring-security-tenant-authorization-spring-web`를 추가한다.

승인된 spec draft:

- `docs/superpowers/specs/2026-05-23-issue-54-spring-security-tenant-authorization-design.md`

## 구현 작업

### 1. Module scaffold

Complexity: M

- `10-multi-tenant/05-database-per-tenant-spring-web`의 structure를 복사한다.
- Package를 `exposed.multitenant.database`에서
  `exposed.multitenant.security`.
- Spring Boot entrypoint를 `TenantSecurityApplication.kt`로 rename한다.
- `springBoot.mainClass`, README title, application metadata를 갱신한다.
- `settings.gradle.kts`가 `includeModules("10-multi-tenant", false, false)`를 통해 module을
  auto-register하는지 확인하고, discovery가 실패할 때만 갱신한다.
- Inventory domain, service, repository, seed data를 module `05`와 비교 가능하게 유지한다.

### 2. Spring Security dependency 추가

Complexity: S

- Module dependency를 추가한다:
  - `org.springframework.boot:spring-boot-starter-security`
  - `org.springframework.boot:spring-boot-starter-oauth2-resource-server`
  - `org.springframework.security:spring-security-test`
- Module `05`의 H2-only tenant database dependency를 유지한다.
- 기존 repo style이 요구하지 않는 한 새 version catalog entry를 피한다. Current example은 direct
  Spring Boot starter coordinate를 사용한다.
- Konsist나 ArchUnit을 추가하지 않는다. Example이 dependency-light 상태를 유지하도록
  `java.nio.file.Files.walk`와 `readText()`를 사용하는 focused JUnit source-text architecture test를
  사용한다.
- Exposed import는 `org.jetbrains.exposed.v1.*` 아래로 유지한다.

### 3. Authentication 및 tenant authorization 구현

Complexity: L

- `config/SecurityConfiguration.kt`를 추가한다.
  - 이 stateless JSON workshop API에서는 CSRF를 비활성화한다.
  - Stateless session management를 사용한다.
  - `/actuator/health`를 permit한다.
  - Inventory endpoint에는 authentication을 요구한다.
  - Tenant claim이 없어도 JWT authentication을 보존하는 converter와 함께 OAuth2 Resource Server
    JWT를 활성화한다.
  - Custom security filter는 이 configuration 안에서 직접 instantiate한다. `@Component`를 붙이거나
    `Filter` bean으로 노출하지 않는다.
  - Credential source를 둘 이상 포함한 request를 거부하도록 authentication filter 앞에
    `CredentialConflictFilter`를 등록한다.
  - Bearer-token authentication 앞의 `SecurityFilterChain` 안에 API-key filter와 demo-session
    filter를 등록한다.
  - `.anyRequest().authenticated()`가 이미 실행되고 MVC handler가 authorized `TenantContext`를 받을
    수 있도록 `AuthorizationFilter` 뒤의 `SecurityFilterChain` 안에 `TenantAuthorizationFilter`를
    등록한다.
  - Custom security filter가 bean으로 노출된다면 independent servlet filter auto-registration을
    비활성화한다.
- `security/AuthenticatedTenant.kt`를 추가한다.
  - 모든 `data class`는 `java.io.Serializable`을 구현하고 `serialVersionUID`를 정의해야 한다.
  - Public API/KDoc text는 English여야 한다.
- `security/DemoJwtDecoder.kt`를 추가한다.
  - Fixed accepted token string:
    - `demo-acme-token` -> `tenant_id=acme`
    - `demo-globex-token` -> `tenant_id=globex`
    - `demo-no-tenant-token` -> no `tenant_id`
    - `demo-unknown-tenant-token` -> `tenant_id=initech`
    - `demo-acme-upper-token` -> `tenant_id=ACME`
    - `demo-malformed-tenant-token` -> `tenant_id=acme,globex`
    - `demo-non-string-tenant-token` -> numeric `tenant_id`
  - 이 decoder가 production token validation이 아니라 fixed-data workshop code라는 한국어 KDoc
    warning을 포함한다.
- `security/CredentialConflictFilter.kt`를 추가한다.
  - Bearer authorization, API key, demo session의 credential source presence만 count한다.
  - Trim 후 non-blank value가 최소 하나 있을 때만 header가 present라고 취급한다.
  - `Authorization`은 case-insensitive scheme matching으로 scheme이 `Bearer`일 때만 count한다.
  - 모든 `Authorization` header value를 scan한다. Non-blank bearer value가 있으면 bearer
    credential이 present다.
  - Example은 Basic, Digest, form login을 지원하지 않으므로 non-bearer authorization scheme은
    무시한다.
  - `/actuator/health`는 skip한다.
  - Source가 둘 이상 present이면 `400 CONFLICTING_CREDENTIALS`를 반환한다.
  - Raw secret-bearing header value를 log하면 안 된다.
- `security/DemoApiKeyAuthenticationFilter.kt`를 추가한다.
  - `demo-acme-key` -> `acme`
  - `demo-globex-key` -> `globex`
  - Invalid provided API key는 authentication에 실패한다.
  - Spring Security exception translation에 참여해야 하며 security chain 밖에서 servlet response에
    직접 write하면 안 된다.
  - Plain class이며 `@Component`나 `Filter` bean이 아니다.
- `security/DemoSessionAuthenticationFilter.kt`를 추가한다.
  - `acme-session` -> `acme`
  - `globex-session` -> `globex`
  - Invalid provided session header는 authentication에 실패한다.
  - Spring Security exception translation에 참여해야 하며 persistent server session을 만들면 안 된다.
  - Plain class이며 `@Component`나 `Filter` bean이 아니다.
- `security/TenantAuthenticationResolver.kt`를 추가한다.
  - JWT, API-key, demo-session authentication에서 tenant identity를 읽는다.
  - Missing, malformed, unknown, resolved state를 구분한다.
  - `TenantId.fromHeaderOrNull`과 맞게 tenant claim value를 trim/lowercase로 normalize한다.
- `security/TenantAuthorizationFilter.kt`를 추가한다.
  - `/actuator/health`는 skip한다.
  - `X-Tenant-ID`를 validate하기 전에 authenticated tenant를 resolve한다. Missing, malformed,
    unknown authenticated tenant는 403을 반환한다.
  - 정확히 하나의 `X-Tenant-ID` header를 validate한다.
  - Requested tenant와 authenticated tenant를 비교한다.
  - Authorization이 성공한 뒤에만 `TenantContext`를 설정한다.
  - `finally`에서 `TenantContext`를 clear한다.
  - Plain class이며 `@Component`나 `Filter` bean이 아니다.
  - Raw `Authorization`, `X-API-Key`, `X-Demo-Session` value를 log하면 안 된다.
  - 명시적 servlet security anchor를 사용한다:
    - `addFilterBefore(credentialConflictFilter, BearerTokenAuthenticationFilter::class.java)`
    - `addFilterBefore(apiKeyAuthenticationFilter, BearerTokenAuthenticationFilter::class.java)`
    - `addFilterBefore(demoSessionAuthenticationFilter, BearerTokenAuthenticationFilter::class.java)`
    - `addFilterAfter(tenantAuthorizationFilter, AuthorizationFilter::class.java)`

### 4. Database-per-tenant routing 보존

Complexity: M

- `TenantDatabaseRegistry`, `TenantTransaction`, `TenantContext`, `TenantId` behavior를 module
  `05`와 정렬된 상태로 유지한다.
- Raw header에서 `TenantContext`를 쓰는 request-path filter는 제거한다.
- Registry validation과 datasource lifecycle test를 유지한다.
- Repository는 datasource-agnostic하게 유지한다. Repository는 `TenantTransaction`만 호출한다.

### 5. Test 추가

Complexity: L

- Security request test:
  - Valid JWT claim은 matching tenant seed data를 읽는다.
  - JWT/header mismatch는 403을 반환한다.
  - Tenant claim 없는 JWT는 403을 반환한다.
  - Unknown tenant claim이 있는 JWT는 403을 반환한다.
  - Malformed tenant claim이 있는 JWT는 403을 반환한다.
  - Non-string tenant claim이 있는 JWT는 403을 반환한다.
  - Missing authentication은 401을 반환한다.
  - API key는 matching tenant seed data를 읽는다.
  - Invalid API key는 401을 반환한다.
  - Demo session은 matching tenant seed data를 읽는다.
  - Demo session/header mismatch는 403을 반환한다.
- Tenant selector test:
  - Missing header는 400을 반환한다.
  - Blank, duplicate, comma-containing, too-long header는 400을 반환한다.
  - Unknown header는 authenticated caller에 대해 404를 반환한다.
  - Uppercase tenant selector와 uppercase tenant claim은 같은 known tenant로 normalize된다.
  - Tenant selector 및 claim 주변 leading/trailing whitespace는 같은 known tenant로 normalize된다.
  - Tenant claim 없는 authenticated JWT와 missing `X-Tenant-ID` 조합은 403을 반환한다.
    Authenticated tenant validation이 selector validation보다 먼저 실행되기 때문이다.
- Credential conflict test:
  - Bearer `acme`와 API key `globex` 조합은 `400 CONFLICTING_CREDENTIALS`를 반환한다.
  - API key와 demo session 조합은 `400 CONFLICTING_CREDENTIALS`를 반환한다.
  - `Authorization: Basic ...`과 API key 조합은 credential conflict가 아니며 API key로
    authenticate된다.
  - Multi-credential `/actuator/health`도 health response를 반환한다.
  - Conflict response는 `TenantContext`를 설정하지 않는다.
- Routing 및 cleanup test:
  - Valid `acme`, `globex` request는 tenant-specific row를 반환한다.
  - Cross-tenant data는 보이지 않는다.
  - Parallel authorized request는 `TenantContext`를 leak할 수 없다.
  - Direct filter test는 downstream failure 이후 same servlet-thread cleanup을 증명한다.
- Architecture test:
  - Konsist나 ArchUnit dependency 없이 `java.nio.file.Files.walk`를 사용하는 JUnit source-text
    scan으로 구현한다.
  - `TenantAuthorizationFilter`만 `TenantContext.set`을 호출한다.
  - Custom security filter는 `@Component` annotation을 갖지 않는다.
  - 이 module의 production code는 module `05`에서 import하지 않는다.
  - Repository는 `TenantTransaction`을 통하지 않는 bare `transaction(`을 호출하지 않는다.

### 6. Documentation 및 diagram 갱신

Complexity: M

- Module `README.md`와 `README.ko.md`를 추가한다.
- 다음을 설명한다:
  - Tenant routing vs tenant authorization.
  - JWT/API-key/demo-session identity source.
  - Demo session header는 production session cookie가 아니다.
  - 이 example은 stateless JSON이므로 CSRF가 비활성화된다.
  - Request 및 error contract.
  - 이 strategy를 선택할 시점.
  - CI coverage와 Nightly가 unchanged인 이유.
  - `DemoJwtDecoder`, fixed API key map, demo session header에 대한 top-level
    "Not for production" caveat.
  - MVC `ThreadLocal` propagation은 coroutine, WebFlux, virtual-thread example에 그대로
    이전되지 않는다.
- `docs/images/readme-diagrams/` 아래 architecture PNG/SVG와 sequence PNG/SVG를 추가한다.
- Readable contrast를 위해 PNG를 visually inspect한다.
- Module `06`을 나열하도록 `10-multi-tenant/README.md`와 `README.ko.md`를 갱신한다.
- Root README file은 module table이 Chapter 10 example을 직접 enumerate할 때만 갱신한다.

### 7. Selected Examples CI 연결

Complexity: S

- Examples workflow의 `on.push.paths`와 `on.pull_request.paths` 모두에
  `10-multi-tenant/06-spring-security-tenant-authorization-spring-web/**`를 추가한다.
- Selected examples Gradle command에 `:06-spring-security-tenant-authorization-spring-web:build`를
  추가한다.
- `actionlint .github/workflows/examples.yml`을 실행한다.

### 8. 검증 및 review

Complexity: M

다음 순서로 실행한다:

1. `./gradlew projects --quiet | rg '06-spring-security-tenant-authorization-spring-web'`
2. `./gradlew :06-spring-security-tenant-authorization-spring-web:compileKotlin --warning-mode all --console=plain`
3. `repo-test-summary -- ./gradlew :06-spring-security-tenant-authorization-spring-web:test --stacktrace --continue`
4. `./gradlew :06-spring-security-tenant-authorization-spring-web:build --stacktrace --continue`
5. `actionlint .github/workflows/examples.yml`
6. `git diff --check`
7. Required architecture diagram PNG link와 Mermaid block 없음 상태에 대한 README diagram scan.
8. Image viewer tooling을 통한 visual PNG inspection.
9. 사용할 수 있으면 IDE diagnostics. 그렇지 않으면 compile/test fallback을 기록한다.
10. Step 6-R current-session 6-Tier review와 Claude Code CLI review. 결과는 `P0=0`, `P1=0`.

### 9. 게시

Complexity: M

- `docs/lessons/2026-05-23-issue-54-spring-security-tenant-authorization.md`를 추가한다.
- Lore protocol trailer를 포함해 commit한다.
- Branch를 push하고 `debop`에게 assign된 `develop` 대상 PR을 연다.
- 사용할 수 있으면 `examples`, `documentation`, security-related label을 추가한다.
- Step 7-R PR comment와 formal review entry를 추가한다.
- Required check가 `SUCCESS` 또는 `SKIPPED`가 될 때까지 CI를 watch한다.
- DoD report 이후 사용자가 merge를 요청하기 전에는 merge하지 않는다.

## 수용 기준 매핑

- Authenticated request에서 tenant 도출:
  Task 2와 3은 `TenantContext`가 설정되기 전에 JWT/API-key/demo-session authentication 및 tenant
  authorization을 구현한다.
- Isolation 및 error handling용 focused test:
  Task 5는 valid access, missing auth, invalid tenant claim, mismatch, cross-tenant denial을
  다룬다.
- README 전략 안내:
  Task 6은 English/Korean docs와 committed PNG diagram을 갱신한다.
- CI/nightly coverage 결정:
  Task 7은 Examples CI를 연결한다. Spec은 Nightly가 unchanged인 이유를 기록한다.

## 검토 메모

구현을 진행하기 전에 Step 2-R/3-R advisor review가 필요하다. Gate는 최신 normalized table이
`P0=0` 및 `P1=0`을 보여 줄 때만 통과한다.
