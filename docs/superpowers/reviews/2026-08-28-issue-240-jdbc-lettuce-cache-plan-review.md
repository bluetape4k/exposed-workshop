# 이슈 #240 JDBC Lettuce 캐시 구현 계획 리뷰

## 리뷰 상태

- 대상 계획: `docs/superpowers/plans/2026-08-28-issue-240-jdbc-lettuce-cache-plan.md`
- 대상 명세: `docs/superpowers/specs/2026-08-28-issue-240-jdbc-lettuce-cache-design.md`
- 리뷰 방식: 여섯 관점을 세 reviewer lane으로 병렬 검토
- 종합 판정: **P0 0건, 보정 전 P1 8건, 보정 후 잔여 P1 0건**
- 계획 승인: 대기

## 관점별 결과

| 관점 | 실행 결과 | 보정 전 지적 | 계획에 반영한 결정 |
| --- | --- | --- | --- |
| Performance | 완료 | `getAll`/`putAll` 비용 관찰 방법과 LongIdTable 신규 insert 계약이 모호함 | MGET 1회·miss별 load/SET·chunk/pipeline counter, seed된 기존 row UPDATE oracle, 성능 수치/SLO 제외 |
| Stability/Ops | 완료 | Redis stop/restart가 무제한 대기할 수 있고, 직렬성·두 번째 chunk 실패·cancellation·close oracle이 부족함 | `RedisURI` timeout, JUnit `@Timeout`, readiness deadline, `junit-platform.properties` same-thread, 결정적 second-chunk failure, `Job.cancelAndJoin`, lazy close 제한을 명시 |
| Security | 완료 | cache poisoning trust boundary와 wildcard 입력의 실행 검증이 부족함 | raw `StringCodec` malformed/변조 payload 테스트, cache를 authorization/business truth로 사용하지 않는 경고, ACL/TLS/secret manager, `product-*` allow-list 거부 규칙 |
| Operator/Ops | 완료 | 기본 CI가 Redis 통합을 제외하고 client 소유권이 이중 종료될 여지가 있음 | merge-ready에는 fresh Redis opt-in pass 필수, manual/Redis-enabled nightly 경계, `ShutdownQueue` 단일 client owner와 repository→client 종료 순서 |
| Developer/API | 완료 | H2-only와 Redis 테스트 경계, public package/constructor, serialVersionUID/KDoc, sync/suspend parity가 불명확함 | `exposed.examples.cache.lettuce` 고정 package, 파일·생성자·codec 기본값·KDoc·`serialVersionUID`, 전체 연산 parity 표 |
| User/Caller | 완료 | README의 실행 경로·source API·제약을 계획에서 정확히 추적하기 어려움 | EN/KO module/chapter README, 실제 import/constructor 예제, H2 기본/Redis opt-in 명령, R2DBC 별도 저장소와 near-cache 제한을 고정 |

## 통합 보정 내역

### 테스트 경계와 비용

- `ProductLettuceCacheH2Test`는 `cache`/`get`/`put`을 호출하지 않는 H2-only
  mapping·seed·count 테스트로 분리한다. Redis 동작은 두 개의 `@Tag("redis")`
  integration test에서만 실행한다.
- `getAll`은 한 번의 MGET, miss마다 DB load와 cache SET이라는 실제 provider
  비용을 단언한다. `putAll`은 두 번째 chunk writer/pipeline failure를 결정적으로
  주입해 첫 chunk commit, partial Redis state, 원래 예외를 단언한다.
- `LongIdTable` auto-increment의 신규 insert skip을 명시하고, write-through는
  seed된 기존 ID UPDATE만 성공 oracle로 사용한다.

### API·Kotlin 계약

- production package는 `exposed.examples.cache.lettuce`로 고정한다.
- `ProductLettuceCacheModels.kt`, `ProductJdbcLettuceRepository.kt`,
  `ProductSuspendedJdbcLettuceRepository.kt`, `ProductLettuceCacheConfig.kt`의
  책임과 실제 경로를 계획에 기록했다.
- sync/suspend constructor의 `client`, `config`, `valueCodec` 순서와
  `ExposedLettuceCodecs.jackson3(ProductRecord::class.java)` 기본값을 일치시킨다.
  `ProductRecord`에는 `@JvmField serialVersionUID`와 한국어 KDoc을 둔다.
- `get`, `getAll`, `put`, `putAll`, `findAll`, `invalidate`, `invalidateAll`,
  `invalidateByPattern`, `clear`를 sync/suspend parity 표로 추적한다.

### 장애·보안·수명주기

- 테스트 client에는 짧은 connect/command timeout, 각 테스트에는 JUnit timeout,
  Redis restart에는 readiness polling/deadline을 적용해 hang을 금지한다.
- `junit-platform.properties`에서 shared Redis stop/restart 테스트를 same-thread로
  직렬화하고, cleanup은 literal prefix `SCAN MATCH` + `UNLINK`만 사용한다.
- fixture는 `ShutdownQueue`에 client를 한 번만 등록하는 단일 owner를 사용하며,
  테스트에서 명시적 `client.shutdown()`을 중복 호출하지 않는다. repository를 먼저
  닫고 JVM 종료 callback이 client를 닫는다.
- malformed JSON은 raw `StringCodec` connection으로 주입해 decode 실패와 DB
  fallback을 검증한다. 유효하지만 변조된 payload는 DB 재검증 없이 반환될 수 있는
  provider trust boundary로 기록하고 authorization/business truth 사용을 금지한다.
- `invalidateByPattern`은 `product-*` allow-list만 허용하고 외부 wildcard/
  사용자 입력을 거부한다.

### CI·완료 게이트

- `:08-cache-strategies-lettuce:build`와 `detekt`를 필수 검증으로 추가했다.
- 기본 `test`/`build`/CI/nightly는 Redis tag를 제외하고, manual 또는
  Redis-enabled nightly의 `-PincludeRedisIntegration=true`가 통합 회귀를 실행한다.
- fresh Redis opt-in pass가 없거나 Docker가 없으면 결과를 `PENDING`으로 보고하고
  merge-ready/PR 보고를 보류한다.

## Writer 게이트 read-back

- **SPW-01 audience/evidence**: 구현자·리뷰어·chapter 11 독자를 대상으로
  provider source, issue #240, 기존 Gradle/CI convention을 근거로 계획했다.
- **SPW-02 structure**: 재확인 → 골격 → RED → GREEN → 문서/diagram → CI →
  검증/롤백 순서를 유지한다.
- **SPW-03 Korean register**: 설명·결정·위험은 한국어로, package·API·command·
  file path는 원문으로 보존했다.
- **SPW-04 technical traceability**: 각 테스트와 acceptance는 provider 실제
  hook, H2/Redis fixture, catalog alias, selector/path filter에 연결했다.
- **SPW-05 final read-back**: 보정 후 H2/Redis 경계, timeout, API parity, trust
  boundary, single owner, build/detekt, opt-in merge gate를 계획 전체에서 재확인했다.

## 잔여 위험

- provider의 remote-only cache map은 Redis 명령 실패에만 loader fallback을 제공하며,
  `client.connect` 초기화 실패에는 DB-only 전환을 제공하지 않는다.
- near-cache explicit codec/local invalidation 보완은 provider 후속 범위다.
- SCAN/UNLINK는 원자적 purge가 아니며, raw cache payload는 신뢰된 Redis 인프라
  전제의 read optimization일 뿐이다.
- Docker가 준비되지 않으면 Redis opt-in 검증은 수행할 수 없고, 이 상태에서 계획
  승인 이후에도 merge-ready로 전환하지 않는다.

## 결론

보정 후 P0/P1 차단 사항은 없다. 계획은 사용자 승인 후에만 TDD RED/골격 작업을
시작하며, 승인 전에는 production code를 변경하지 않는다.
