# 변경 이력

모든 주요 변경 사항은 이 파일에 기록됩니다. 형식은 [Keep a Changelog](https://keepachangelog.com/ko/1.0.0/)를 따르며, 이 프로젝트는 [Semantic Versioning](https://semver.org/lang/ko/)을 따릅니다.

---

## [미배포]

`1.1.2` 태그 이후 `develop` 브랜치에 반영된 예제·문서·검증 변경을
기록합니다. 현재 버전과 의존성 기준은 `gradle/libs.versions.toml`의
`bluetape4k-dependencies:2.0.0`, Kotlin `2.4.0`, Java toolchain `25`,
Exposed `1.4.0`입니다.

### 추가

- **Chapter 10 멀티테넌시**: schema-per-tenant, database-per-tenant, Spring
  Security tenant authorization, Ktor, tenant onboarding 예제를 추가했습니다.
- **Chapter 11 고성능**: Ktor cache/routing 변형과 JDBC + Lettuce 원격 캐시
  예제를 추가했습니다. H2 기본 검증과 Redis opt-in 경계를 문서화했습니다.
- **Chapter 12 운영 통합**: Spring Boot 4와 Ktor의 application architecture,
  HTTP outbox/idempotency, auth/session, realtime outbox, observability/readiness
  예제를 쌍으로 구성했습니다.
- **Chapter 13 ecosystem integrations**: BigQuery, Trino, CockroachDB,
  StarRocks, Ktor Exposed, Spring Modulith, DDD, DuckDB, Apache Druid,
  checkpointable JDBC batch, JaVers + Exposed 감사 이력 예제를 연결했습니다.

### 변경

- `settings.gradle.kts`가 `00`부터 `13`장까지 소스 트리의 모듈을 자동 발견하도록
  유지하고, Gradle version catalog와 중앙 BOM을 기준으로 의존성 좌표를 관리합니다.
- Chapter 10의 기존 MVC/Virtual Thread tenant consumer가 versionless
  `libs.bluetape4k.tenant` alias를 통해 공통 `bluetape4k-tenant` carrier를
  사용하도록 전환했습니다. 애플리케이션 경계는 header parsing·인가·schema/database
  routing을 유지하고, lexical binding과 cleanup은 공통 `ThreadLocalTenantContext` /
  `ScopedValueTenantContext`에 위임합니다.
- CI는 선택된 Examples 경로와 nightly 검증 경계를 사용하며, detekt·테스트
  결과와 시각 자료 검증을 아티팩트로 남깁니다.
- 모든 장의 README는 실행 모듈, 다이어그램 이미지, English/Korean locale 쌍을
  소스와 함께 설명하도록 정렬했습니다.

### 수정

- tenant datasource registry가 소유한 Hikari pool의 lifecycle을 정리해 종료 시
  pool 누수를 방지했습니다.
- JaVers 감사 예제에서 JDBC 고객 삭제 이후에도 삭제 시점의 감사 이력을 보존하고,
  Exposed 삭제 lifecycle 계약을 회귀 검증으로 고정했습니다.
- JDBC batch의 checkpoint 재시작 경계와 Lettuce cache provider의 sync/suspend
  계약을 각각 독립 예제로 명확히 했습니다.

### 테스트

- Druid query-only, checkpointable JDBC batch, Ktor observability, measured
  단위 컬럼, JaVers 감사 이력, JDBC Lettuce cache의 H2 중심 테스트를 추가했습니다.
- #255 대상 모듈에서 unbound context, 중첩 scope 복원, 예외 후 cleanup, 순차·병렬
  tenant 격리를 검증했습니다. 공개 `2.0.0`의 정상 Gradle 해석 기준
  `02` 44개와 `06` 32개 테스트가 통과했습니다.
- 안정 `2.0.0` catalog의 dependency resolution과 governance를 통과시켰습니다.
  `:11-checkpointable-batch:test`에서 H2 2.4.240의
  `BATCH_JOB_EXEC_STATUS_ACTIVE_KEY_CHK` cross-session 회귀가 변경 전 `develop`와
  동기화 branch에서 모두 재현되어 #260으로 등록했습니다. 전역 H2 2.4.240은
  유지하고 checkpointable batch test runtime만 `h2-v2-check-workaround = 2.3.232`로
  고정했으며, 회귀 1개·batch 모듈 9개·Ktor 6개 대상 테스트를 통과시켰습니다.
  최종 전체 `clean build`도 `BUILD SUCCESSFUL in 13m 1s`와 `1102 actionable tasks`
  (`1094 executed`, `6 from cache`, `2 up-to-date`)로 완료했습니다.
- 멀티테넌트 MVC/Virtual Thread/WebFlux와 cache/routing 예제에 순차·병렬 격리,
  실패 후 cleanup, fallback 동작을 확인하는 회귀 검증을 보강했습니다.
- 문서 검증은 `git diff --check`, localization 범위, README 링크와 다이어그램
  자산 검사를 기준으로 유지합니다.

### 문서

- 루트 및 장별 README의 모듈 목록을 현재 `settings.gradle.kts`와 일치시켰습니다.
- WIP 큐를 2026-09-02 GitHub 상태로 갱신하고, `#255` 구현이
  `2.0.0` catalog와 공개 tenant artifact 기준으로 완료된 상태를
  기록했습니다. `bluetape4k-tenant`와 dependency/exposed BOM의 공개 안정
  좌표를 확인했으며, upstream provider PR #1566은 merge되었습니다.
- `#259`를 통해 `bluetape4k-dependencies:2.0.0` 안정 릴리스 기준으로
  workshop catalog, governance guard, 현재 문서를 정렬하고, 후속 H2 회귀는
  `#260`에서 모듈 범위 workaround와 회귀 테스트로 추적합니다.

## [1.1.2] - 2026-03-21

### 변경

- 프로젝트 버전을 `1.1.1`에서 `1.1.2`로 올렸습니다.
- Bluetape4k 기준 버전을 `1.5.0-Beta2`로 갱신했습니다.

---

## [1.1.1] - 2026-03-14

### 추가

- **`11-high-performance/04-benchmark`**: `kotlinx-benchmark` 기반 마이크로벤치마크 모듈 추가
    - `RoutingKeyResolverBenchmark`: 라우팅 키 계산 오버헤드 측정
    - `ReadThroughCacheBenchmark`: cache hit / cache miss 비교
    - smoke 프로파일(`smokeBenchmark`) 및 Markdown 리포트 생성(`benchmarkMarkdown`) 지원
- `.editorconfig` 파일 추가

### 변경

- **Kotlin**: `2.3.20-RC3` → `2.3.20` (정식 릴리스)
- **Bluetape4k**: `1.4.0` → `1.5.0-Beta1`
    - `bluetape4k-jackson` → `bluetape4k-jackson2` 리네이밍 대응
    - `bluetape4k-crypto` Deprecated → `bluetape4k-tink` 대체 반영
- Kotlin 2.3 기준으로 빌드 설정 전면 전환 (`languageVersion = 2.3`, `apiVersion = 2.3`)
- Redisson Client 설정 개선 (netty 설정 변경, Virtual Threads 적용)

### 수정

- `RedissonClient` 설정에서 netty 관련 설정 오류 수정 (`5982dcd7`, `c9870321`, `b1908633`, `d4b7dd01`)

---

## [1.0.5] - 2026-03-12

### 추가

- **`bin/repo-status`**, **`bin/repo-diff`**, **`bin/repo-test-summary`**: 토큰 절약형 저장소 요약 헬퍼 스크립트 추가
- **Redisson 설정 최적화**: Virtual Threads 기반 Redisson Client 설정 적용 (`dd8f3042`)

### 변경

- **Bluetape4k**: `1.3.1` → `1.4.0`
- **Kotlin**: `2.2.21` → `2.3.20-RC3`
    - `vertx-sqlclient-example` compileTestKotlin hang 문제 우회 처리
- 빌드 속도 개선 (configuration cache 활용 강화)

### 수정

- `vertx-sqlclient-example` 컴파일 시 hang 걸리는 문제 해결

---

## [1.0.0] - 2025-12-01 (초기 릴리스)

### 추가

- **워크샵 전체 구조** 구성 (모듈 `00` ~ `11`)
- **`00-shared/exposed-shared-tests`**: 공통 테스트 인프라 (`AbstractExposedTest`, `TestDB`, `WithTables`)
- **`01-spring-boot`**: Spring MVC + Virtual Threads, Spring WebFlux + Coroutines 예제
- **`02-alternatives-to-jpa`**: R2DBC, Vert.x SQL Client, Hibernate Reactive 비교 예제
- **`03-exposed-basic`**: DSL / DAO 패턴 기초 예제
- **`04-exposed-ddl`**: 연결 관리, 스키마 정의
- **`05-exposed-dml`**: SELECT/INSERT/UPDATE/DELETE, 컬럼 타입, SQL 함수, 트랜잭션, Entity API
- **`06-advanced`**: Crypt, JavaTime, kotlinx-datetime, JSON, Money, 커스텀 컬럼, 커스텀 Entity, Jackson, Fastjson2, Jasypt, Jackson3, Tink
- **`07-jpa`**: JPA → Exposed 마이그레이션 (기본/고급)
- **`08-coroutines`**: Coroutines 기반 비동기 트랜잭션, Virtual Threads
- **`09-spring`**: Spring Boot AutoConfiguration, TransactionTemplate, @Transactional, Repository 패턴, Spring Cache, Suspended Cache
- **`10-multi-tenant`**: Schema-based 멀티테넌시 (MVC, Virtual Threads, WebFlux)
- **`11-high-performance`**: Read/Write Through/Behind 캐시 전략, RoutingDataSource

[미배포]: https://github.com/bluetape4k/exposed-workshop/compare/1.1.2...HEAD

[1.1.2]: https://github.com/bluetape4k/exposed-workshop/compare/1.1.1...1.1.2

[1.1.1]: https://github.com/bluetape4k/exposed-workshop/compare/1.0.5...1.1.1

[1.0.5]: https://github.com/bluetape4k/exposed-workshop/compare/1.0.0...1.0.5

[1.0.0]: https://github.com/bluetape4k/exposed-workshop/tree/1.0.0
