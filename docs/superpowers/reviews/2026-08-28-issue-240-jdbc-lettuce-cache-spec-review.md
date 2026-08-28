# 이슈 #240 JDBC Lettuce 캐시 설계 명세 리뷰

## 리뷰 상태

- 대상 명세: `docs/superpowers/specs/2026-08-28-issue-240-jdbc-lettuce-cache-design.md`
- 대상 이슈: [#240](https://github.com/bluetape4k/exposed-workshop/issues/240)
- 설계 방향 승인: 완료
- 리뷰 방식: 여섯 관점 독립 검토. 네이티브 API·사용자 리뷰는 bounded
  liveness 절차 후 main-session fallback으로 완료
- 종합 판정: **P0 0건, 명세 보정 전 P1 12건, 보정 후 잔여 P1 0건**
- 다음 게이트: 명세 read-back 승인 후 구현 계획 승인

## 관점별 결과

| 관점 | 실행 결과 | 주요 지적 | 명세에 반영한 결정 |
| --- | --- | --- | --- |
| Performance | 완료 | `getAll`은 MGET 뒤 miss별 DB loader/SET이고, `putAll` DB writer는 chunk별 행 단위 mapping일 수 있음. 처리량·SLO를 암시하면 안 됨 | provider batch 비용, query/command bounded smoke, 운영 SLO 제외를 명시 |
| Security | 완료 | suspend near-cache가 repository codec을 전달받지 않아 기본 codec을 쓸 수 있음. 기본 `nearCacheName` 충돌과 local invalidation 공백이 있음. 고정 prefix·payload trust boundary가 필요 | `nearCacheEnabled=false` remote-only를 DoD로 고정하고 near-cache는 후속 이슈로 분리. 테스트 Redis·고유 namespace·ACL/TLS/secret manager 경계를 명시 |
| Stability/Ops | replacement 완료 | provider 초기화 실패에는 DB fallback이 없음. batch 후속 chunk/pipeline 실패는 앞선 commit을 되돌리지 않음. close-before-use·retry·opt-in·near-cache 오류 경계가 불명확 | 초기화 실패 전파, partial success/stale cache, close lifecycle, retry 경계, 정확한 opt-in 규칙을 명시 |
| Operator/Ops | 완료 | Redis 중단 후 재연결 시나리오, CI/nightly 범위, 테스트 namespace·직렬성, client shutdown 소유권, SCAN 비원자성·rollback 정리가 부족 | same-client auto-reconnect bounded smoke, `@Tag("redis")` + `-PincludeRedisIntegration=true`, 고유 suffix·`ShutdownQueue`, SCAN/UNLINK·FLUSH 금지를 명시 |
| Developer/API | native replacement bounded 후 main fallback | sync/suspend hook과 `findAll` 기본 인자, `getAll` map 의미, pattern suffix 계약, 명시 codec 예제가 한눈에 고정되어야 함 | `ProductJdbcLettuceRepository`/`ProductSuspendedJdbcLettuceRepository`와 공통 호출 예제·map/pattern/batch 계약을 명시 |
| User/Caller | native lane interrupted 후 main fallback | 학습 순서, 실행 명령, source-equivalent README·diagram, R2DBC 제외와 high-performance 표현의 한계가 독자에게 명확해야 함 | 기본 H2 경로·Redis opt-in 명령·문서/diagram pair·R2DBC 별도 저장소 경계를 명시 |

## 통합 보정 내역

### 캐시·데이터 일관성

- `READ_WRITE_THROUGH`와 명시적 `ExposedLettuceCodecs.jackson3(...)`를 sync/suspend
  양쪽에서 동일하게 사용한다.
- Redis 명령 실패 후에만 DB fallback을 허용한다. provider map 생성 또는
  `client.connect(...)` 초기화 실패를 DB-only fallback으로 포장하지 않는다.
- `putAll`은 chunk별 독립 DB transaction과 Redis pipeline의 조합이다. 후속 실패가
  앞선 commit을 되돌리지 않는 partial success, stale cache, 재조회·invalidation·TTL
  복구 경계를 README와 테스트에 기록한다.
- `getAll`의 MGET + miss별 fallback 비용을 숨기지 않고 작은 고정 입력의 query/
  command counter만 검증한다. 성능 수치나 SLO는 산출하지 않는다.

### 장애·수명주기

- Lettuce client의 현재 기본 auto-reconnect와 5초 connect timeout을 사용하며,
  별도 command retry loop는 추가하지 않는다. read transport 오류는 즉시 fallback,
  write SET/pipeline 오류는 전파한다.
- Redis 중단 → DB fallback → Redis 재시작 → 동일 client의 재연결·cache fill을
  bounded smoke로 검증한다.
- repository를 먼저 닫고 fixture가 `ShutdownQueue` 또는 명시적 shutdown으로
  client를 닫는다. 공유 `DEFAULT_CLIENT_RESOURCES`는 테스트가 닫지 않는다.
  close-before-use와 repeated close를 검증하되 provider의 lazy cache 초기화
  동작을 제한 사항으로 남긴다.
- fallback/writer 로그는 payload·key 없이 `operation`, `entryCount`, `cacheType`,
  `errorType`만 보조 관찰 필드로 사용한다. metrics/health-check을 약속하지 않는다.

### 테스트·CI·문서

- Redis 테스트는 `@Tag("redis")`로 표시하고 기본 `test`/`build`/CI/nightly에서는
  제외한다. `-PincludeRedisIntegration=true`가 유일한 opt-in 명령이며 Docker가
  없을 때 skip하지 않고 실패시켜 검증 공백을 숨기지 않는다.
- 고정 문서 prefix와 테스트별 고유 suffix를 분리하고, `maxParallelUsages = 1`,
  `SCAN MATCH <literal-prefix>:*` + `UNLINK` 정리, `FLUSHDB`/`FLUSHALL` 금지를
  고정한다.
- README EN/KO는 같은 목차·명령·제약을 유지하고, architecture/sequence SVG와
  PNG를 source-equivalent pair로 제공한다. raw Mermaid는 사용하지 않는다.
- R2DBC는 `exposed-r2dbc-workshop`에서 별도 구현하며 이 모듈에 추가하지 않는다.

## Writer 게이트 read-back

- **SPW-01 audience/evidence**: 기존 chapter 11 독자와 provider source/이슈/CI를
  근거로 고정했으며, 구현·Redis 실행 전 명세임을 문서 상태에 표시했다.
- **SPW-02 structure**: 목표, 근거, 구조, API, 흐름, 장애, 테스트, CI, 문서/diagram,
  DoD, rollback 순서를 유지한다.
- **SPW-03 Korean register**: 기술 식별자·명령·API 이름만 원문을 보존하고 설명은
  한국어로 작성했다. `audit-korean-terms.mjs` 결과는 findings 0이다.
- **SPW-04 traceability**: 각 수용 기준은 provider source, 현재 workflow/selector,
  또는 테스트 관찰값으로 연결되며, near-cache·metrics·2PC·운영 secret은 범위 밖으로
  분리했다.
- **SPW-05 final read-back**: placeholder scan과 `git diff --check`를 통과했고,
  보정된 opt-in·reconnect·shutdown·partial batch·namespace 경계를 명세 전체에서
  다시 확인했다.

## 잔여 위험과 구현 계획 입력

- near-cache explicit codec/local invalidation 보완은 provider 변경 없이는 안전하게
  구현할 수 없으므로 이번 module의 DoD에서 제외한다.
- SCAN/UNLINK는 원자적 purge가 아니며, 운영 관찰은 로그 보조 수준이다.
- Docker가 없는 현재 환경에서는 기본 H2/static 검증만 완료할 수 있고, 최종 구현
  보고는 Redis opt-in 결과가 확보될 때까지 `PENDING`으로 유지한다.
- 구현 계획은 새 abstraction을 만들지 않고 기존 provider API, shared fixture,
  catalog alias, examples selector/path filter를 작은 순서형 작업으로 연결한다.

## 결론

P0와 보정 후 P1 차단 사항은 없다. 위 잔여 위험은 명세에 명시적으로 제한되었고,
구현 계획과 테스트 계약으로 추적 가능하다. 이 리뷰 문서와 보정된 설계를 읽은 뒤
계획 승인 게이트로 진행한다.
