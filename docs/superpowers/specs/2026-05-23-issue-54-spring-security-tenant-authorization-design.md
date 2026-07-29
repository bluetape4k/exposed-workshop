# Issue 54 Spring Security tenant authorization 설계

## 배경

Issue #54는 10장 routing example이 남긴 production trust gap을 닫는다. 기존 Spring MVC 예제는
tenant propagation과 isolation을 보여 주지만, `X-Tenant-ID`가 여전히 final trust source다. 이
예제는 어떤 Exposed JDBC transaction도 tenant database를 선택하기 전에 caller를 먼저
authenticate하고, security material에서 caller tenant를 도출한 뒤 requested tenant를 authorize해야
한다.

이는 security-sensitive example module, public README material, CI wiring, review-gated
authorization behavior를 추가하므로 Type A Full Design change다.

## 외부 근거

- Spring Security 6.5 servlet resource server documentation은 OAuth2 Resource Server JWT와
  custom `jwtAuthenticationConverter`를 사용하는 Kotlin `SecurityFilterChain` configuration을
  보여 준다.
  Source: Context7 `/websites/spring_io_spring-security_reference_6_5`,
  servlet OAuth2 resource server JWT.
- Spring Security test support는 `spring-security-test`를 통해 JWT/OAuth2 helper를 포함한
  MockMvc request post processor를 제공한다.
  Source: Context7 `/websites/spring_io_spring-security_reference_6_5`,
  servlet test API docs.
- 기존 repository pattern:
  `12-production-integration/05-spring-auth-session` uses servlet
  `SecurityFilterChain`, stateless JSON security, and `spring-security-test`.
- 기존 tenant routing pattern:
  `10-multi-tenant/05-database-per-tenant-spring-web` owns one Hikari pool and
  Exposed `Database` per tenant, with no default datasource fallback.
- 이전 R2DBC sibling design:
  `exposed-r2dbc-workshop` issue #40 requires tenant claim failures to remain
  authorization failures and requires the routing context to be written only
  after tenant authorization succeeds.
- Module `05`는 tenant header를 `trim().lowercase()`로 normalize한다. 이 모듈도
  `X-Tenant-ID: ACME`가 `acme`로 mapping되도록 해당 policy를 유지한다.

## 범위

- `10-multi-tenant/06-spring-security-tenant-authorization-spring-web`를 추가한다.
- Spring MVC, Spring Security, Exposed JDBC, 두 H2 tenant database를 사용한다:
  `acme`와 `globex`.
- Workshop을 위해 self-contained authentication path 세 가지를 지원한다:
  - `tenant_id` claim이 있는 JWT bearer token.
  - Tenant에 mapping되는 API key header.
  - Tenant에 mapping되는 demo session header.
- Inventory API와 database-per-tenant routing shape를 module `05`와 비교 가능하게 유지한다.
- Committed architecture diagram PNG와 추가 sequence/request-flow PNG가 있는 English/Korean
  README를 추가한다.
- 모듈을 Chapter 10 docs, module list를 유지하는 root README docs, selected examples CI에
  연결한다.

범위 제외:

- Production identity provider 또는 authorization server setup.
- Dynamic tenant onboarding/provisioning. Issue #55가 이를 소유한다.
- Shared bluetape4k security abstraction.
- Testcontainers database matrix coverage. 이 모듈은 H2-only로 유지되며 CI decision을
  문서화한다.

## 설계

### Security boundary

예제는 다음 invariant를 증명해야 한다.

1. Authentication은 tenant identity를 수립한다.
2. `X-Tenant-ID`는 target tenant를 선택한다.
3. Tenant authorization은 authenticated tenant가 requested tenant와 같을 때만 성공한다.
4. `TenantContext`는 tenant authorization이 성공한 뒤에만 기록된다.
5. Repository는 `TenantTransaction`을 통해서만 Exposed `Database`를 선택한다.

어떤 request path도 raw tenant header만으로 `TenantContext`를 쓰면 안 된다. Header-only trust가
Exposed까지 도달할 수 있다면 모듈은 primary purpose에 실패한 것이다.

### Authentication source

Test와 README snippet이 self-contained 상태로 남도록 모듈은 fixed demo credential을 지원한다.

| Source | Request shape | Tenant source |
|---|---|---|
| JWT | `Authorization: Bearer demo-acme-token` | `tenant_id=acme` |
| API key | `X-API-Key: demo-acme-key` | configured API key map |
| Demo session | `X-Demo-Session: acme-session` | configured demo session map |

Demo JWT decoder는 fixed token string만 허용한다. Predictable claim을 가진 Spring Security
`Jwt` instance를 반환하며 signing이나 issuer discovery를 구현하지 않는다. 이렇게 해서 예제는
identity-provider infrastructure가 아니라 tenant authorization에 집중한다.

Bearer token이 syntactically valid하지만 tenant claim이 없어도 JWT authentication은 성공해야
한다. Tenant claim 문제는 authentication failure가 아니라 authorization failure이므로
`401 Unauthorized`가 아니라 `403 Forbidden`을 반환해야 한다.

Invalid bearer token, invalid API key, invalid demo session은 authentication failure로 남으며
`401 Unauthorized`를 반환한다.

### Credential precedence

하나의 request에 여러 credential source가 있으면 authentication 전에 거부한다. Request는 다음 중
정확히 하나만 보낼 수 있다.

- `Authorization: Bearer ...`
- `X-API-Key`
- `X-Demo-Session`

Header presence는 header value가 최소 하나 존재하고 trim 후 non-blank value가 최소 하나 있음을
뜻한다. Empty API-key 또는 demo-session header는 credential source로 세지 않으며 authenticate하지
않는다.

`Authorization` header는 non-blank header value 중 하나가 case-insensitive scheme matching으로
`Bearer` scheme을 사용할 때만 bearer credential source로 센다. 이 모듈은 Basic, Digest, form
login authentication을 지원하지 않으므로 non-bearer authorization scheme과 scheme 없는
authorization value는 무시한다. 여러 `Authorization` header value는 함께 scan한다. Bearer value가
있고 다른 supported credential source도 있으면 request는 conflicting 상태다.

Credential source가 둘 이상 있으면 request는 `CONFLICTING_CREDENTIALS`와 함께
`400 Bad Request`를 반환한다. Bearer token `acme`와 API key `globex` 조합 같은 cross-tenant
escalation attempt를 숨길 수 있으므로 이 모듈은 "first credential wins" behavior를 의도적으로
피한다.

Public health check는 credential conflict check와 tenant authorization을 bypass한다. Proxy 또는
caller가 unrelated credential header를 보내도 `/actuator/health`는 health response를 반환한다.

### Authorization filter

Custom filter는 `SecurityConfiguration` 안에서 instantiate되는 plain class다. `@Component`
class가 아니며 `Filter` bean으로 노출하지 않는다. 이는 Spring Boot가 이들을
`SecurityFilterChain` 밖의 independent servlet filter로 등록하는 것을 막는다.

`CredentialConflictFilter`는 authentication filter 앞의 `SecurityFilterChain` 안에 등록된다.
이 filter는 credential-source presence만 확인하고 mixed credential source에는
`400 CONFLICTING_CREDENTIALS`를 쓴다.

`TenantAuthorizationFilter`는 `SecurityFilterChain` 안에 등록된다. Spring Security
authentication과 request authentication authorization이 `SecurityContextHolder`를 populate/validate할
기회를 가진 뒤, MVC handler invocation 전에 실행된다. 이 filter는 다음을 수행한다.

- public actuator health request를 skip한다.
- JWT/API-key/demo-session authentication에서 authenticated tenant를 resolve한다.
- Missing, malformed, unknown, mismatched authenticated tenant identity에는 `403`을 반환한다.
- Authenticated tenant가 valid한 뒤에만 정확히 하나의 `X-Tenant-ID` header를 검증한다.
- Blank, comma-containing, too-long, unknown tenant selector를 거부한다.
- Authorized tenant에 대해서만 `TenantContext`를 설정하고 `finally`에서 clear한다.

Check order는 고정이다.

1. `CredentialConflictFilter`는 public health를 skip한 뒤 mixed supported credential source를
   거부한다.
2. Spring Security는 하나의 supported credential source를 authenticate한다.
3. Spring Security `AuthorizationFilter`는 inventory request에 authentication을 강제한다.
4. `TenantAuthorizationFilter`는 authenticated tenant를 resolve하고 missing, malformed, unknown
   tenant identity에는 `403`을 반환한다.
5. Valid authenticated tenant가 존재한 뒤에만 filter는 `X-Tenant-ID`를 검증하고 selector error에
   `400`/`404`를 반환한다.
6. Tenant mismatch는 `403`을 반환한다. Match는 `TenantContext`를 설정하고 계속 진행한다.

Spring Security는 authentication/authorization failure의 `401` 및 `403`을 소유한다. Tenant
selector validation은 request가 controller에 도달하기 전에 stable JSON error를 반환한다.

`DemoApiKeyAuthenticationFilter`와 `DemoSessionAuthenticationFilter`도 bearer-token authentication
앞의 Spring Security chain 안에 등록된다. 이 filter들은 `Authentication` object를 만들고 Spring
Security exception translation에 참여해야 하므로 plain `@Component` servlet filter가 되면 안 된다.

Security log는 raw `Authorization`, `X-API-Key`, `X-Demo-Session` 값을 기록하면 안 된다.
Credential-related failure를 log할 때는 secret-bearing header value가 아니라 credential source와
outcome만 기록한다.

### Tenant routing

Database-per-tenant infrastructure는 의도적으로 module `05`와 비슷하게 유지한다.

- `TenantDataSourceProperties`는 `app.tenants` 아래 모든 known tenant datasource를 설정한다.
- `TenantDatabaseRegistry`는 `TenantId`마다 하나의 Hikari pool과 하나의 Exposed `Database`를
  만들고 소유한다.
- `TenantTransaction.execute { ... }`는 기본적으로 `TenantContext.current()`를 읽고
  `transaction(registry.databaseFor(tenant))`를 실행한다.
- Known tenant가 없거나 unknown tenant가 설정되면 registry는 startup을 실패시킨다.
- Default datasource fallback은 없다.

### Domain

모듈은 module `05`의 inventory API shape를 재사용한다.

- `InventoryItems`
- `InventoryItemRecord`
- `CreateInventoryItemRequest`
- `InventoryRepository`
- `InventoryService`
- `InventoryController`
- `InventorySeeder`

Tenant seed data는 test와 README example이 isolation을 증명할 수 있도록 `acme`와 `globex`에서
눈에 띄게 달라야 한다.

### Error contract

| Case | Result |
|---|---|
| valid auth tenant equals `X-Tenant-ID` | `200 OK` |
| missing authentication | `401 Unauthorized` |
| invalid bearer token/API key/demo session | `401 Unauthorized` |
| multiple credential sources | `400 Bad Request`, `CONFLICTING_CREDENTIALS` |
| missing `X-Tenant-ID` | `400 Bad Request`, `MISSING_TENANT` |
| blank, duplicated, or malformed `X-Tenant-ID` | `400 Bad Request`, `MISSING_TENANT` |
| unknown `X-Tenant-ID` | `404 Not Found`, `UNKNOWN_TENANT` |
| authenticated tenant missing or unknown | `403 Forbidden` |
| authenticated tenant malformed | `403 Forbidden` |
| authenticated tenant differs from `X-Tenant-ID` | `403 Forbidden` |

400/404 tenant-selector behavior는 module `05`와 정렬된 상태를 유지한다. 새로운 403 case는 이
issue가 추가하는 security boundary다. Tenant selector와 tenant claim value는 matching 전에
trim/lowercase로 normalize되므로 `ACME`, ` acme `, `acme`는 같은 tenant로 취급된다. Tenant
selector length cap은 module `05`와 같은 64자다. Blank, comma-containing, too-long value는
malformed 상태로 남는다. Request tenant selector에서는 blank, duplicated, comma-containing,
too-long value가 의도적으로 module `05`의 `MISSING_TENANT` code를 재사용한다. Authenticated
tenant identity에서 malformed는 non-string claim, blank claim, comma-containing claim,
mid-value whitespace, 64자를 넘는 value를 뜻한다. Malformed authenticated identity는 caller가
authenticated 상태지만 valid tenant에 authorized되지 않았으므로 `403`을 반환한다.

### README diagram

두 README file은 `docs/images/readme-diagrams/` 아래 같은 committed PNG file을 link한다.

- `10-multi-tenant-06-spring-security-tenant-authorization-spring-web-architecture-01.png`
- `10-multi-tenant-06-spring-security-tenant-authorization-spring-web-sequence-02.png`

SVG source는 PNG 옆에 commit한다. Diagram text는 English다. README file은 Mermaid block을
포함하면 안 된다.

## 검증

Local verification:

- `./gradlew :06-spring-security-tenant-authorization-spring-web:test --stacktrace --continue`
- `./gradlew :06-spring-security-tenant-authorization-spring-web:build --stacktrace --continue`
- `./gradlew projects --quiet | rg '06-spring-security-tenant-authorization-spring-web'`
- `actionlint .github/workflows/examples.yml`
- Architecture Diagram PNG link, existing PNG file, Mermaid block 없음 상태를 확인하는 README
  diagram scan.
- Generated PNG의 readable contrast에 대한 visual inspection.

Required test coverage:

- JWT tenant claim은 matching tenant에 접근할 수 있다.
- JWT/header mismatch는 403을 반환한다.
- `tenant_id` 없는 JWT는 403을 반환한다.
- Missing authentication은 401을 반환한다.
- API key는 matching tenant에 접근할 수 있다.
- Invalid API key는 401을 반환한다.
- Demo session은 matching tenant에 접근할 수 있다.
- Demo session/header mismatch는 403을 반환한다.
- Missing, malformed, unknown tenant selector는 authenticated caller에 대해 module의 stable
  400/404 error contract를 유지한다.
- Parallel authorized `acme`/`globex` request는 `TenantContext`를 leak할 수 없다.
- Failing downstream request는 servlet thread에서 `TenantContext`를 clear한다.
- Architecture scan은 HTTP request path에서 `TenantContext`를 쓰는 주체가
  `TenantAuthorizationFilter`뿐임을 증명한다.
- Multi-credential request는 `CONFLICTING_CREDENTIALS`로 거부된다.
- Uppercase tenant selector와 tenant claim variant는 명시적 trim/lowercase normalization
  policy를 따른다.

Review gate:

- 이 spec/plan에 대해 stdin과 timeout >= 5 minutes를 사용하는 Claude Code CLI로 Step 2-R/3-R
  advisor review를 수행한다.
- 구현 후 6-Tier frame과 stdin 및 timeout >= 5 minutes를 사용하는 Claude Code CLI로 Step 6-R
  code review를 수행한다.

## Advisor gate

| Artifact | Result | Notes |
|---|---|---|
| `.omx/artifacts/claude-issue-54-spec-plan-advisor-stdin-6min-20260523005725.md` | FAIL, P0=0/P1=3 | Accepted edits for credential conflict behavior, source-text architecture tests, and custom filter registration. |
| `.omx/artifacts/claude-issue-54-spec-plan-advisor-rerun-stdin-6min-20260523010057.md` | FAIL, P0=0/P1=3 | Accepted edits for bearer detection, authorization/selector check order, and malformed authenticated tenant semantics. |
| `.omx/artifacts/claude-issue-54-spec-plan-advisor-rerun2-stdin-6min-20260523010327.md` | PASS, P0=0/P1=0 | Applied P2 clarifications for concrete filter anchors, blank credential presence, multiple authorization headers, selector length, and whitespace normalization tests. |

## CI 결정

이 모듈은 H2-only이므로 selected Examples CI에 속하며 Nightly Testcontainers shard가 필요하지
않다. Selected Examples workflow는 module path filter와
`:06-spring-security-tenant-authorization-spring-web:build` task를 포함해야 한다.
