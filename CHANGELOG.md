# Changelog

모든 주요 변경 사항은 이 파일에 기록됩니다. 형식은 [Keep a Changelog](https://keepachangelog.com/ko/1.0.0/)를 따르며, 이 프로젝트는 [Semantic Versioning](https://semver.org/lang/ko/)을 따릅니다.

---

## [Unreleased]

### Added

- `.omc/` 디렉터리 추가 (OMC 상태 관리용)
- `.claude/worktrees` gitignore 규칙 추가
- `Libs.bluetape4k_lingua`, `Libs.bluetape4k_mock_web_server`, `Libs.bluetape4k_mock_webflux_server` 모듈 참조 추가 (BOM 1.7.0 신규 모듈)
- GitHub Actions CI 워크플로 개선: DB 매트릭스 (`H2/PostgreSQL/MySQL 8/MariaDB`) 병렬 실행, Kover 커버리지 집계, detekt/테스트 아티팩트 업로드

### Changed

- **Bluetape4k**: `1.6.2` → `1.7.0`
- **Kotlin**: `2.3.20` → `2.3.21`
- **MariaDB JDBC 드라이버**: `mariadb-java-client` `3.5.7` → `3.5.8`, `r2dbc-mariadb` `1.3.0` → `1.4.0`
- CI 테스트 잡에서 Testcontainers 기반 DB는 `--max-workers=1` 적용 (Docker 리소스 경합 방지)

### Removed

- `Libs.bluetape4k_crypto`, `Libs.bluetape4k_exposed_jasypt` 참조 제거 (BOM 1.7.0에서 제외됨)
- `06-advanced/10-exposed-jasypt` 예제 모듈 전체 삭제 (대체 모듈은 `12-exposed-tink`)
- `06-advanced/06-custom-columns`의 encrypt 테스트 디렉터리 삭제 (`bluetape4k-crypto` 의존)
- `00-shared/exposed-shared-tests`, `02-alternatives-to-jpa/hibernate-reactive-example`에서 crypto/jasypt 참조 정리

### Fixed

- MariaDB에서 JSON/JSONB 컬럼 default 메타데이터 round-trip 불일치로 실패하던 테스트 8개 스킵 처리 (`04-exposed-json`, `08-exposed-jackson`, `09-exposed-fastjson2`, `11-exposed-jackson3`)

### Test

- **`10-multi-tenant/01-multitenant-spring-web`**: `ActorExposedRepository` / `MovieExposedRepository`에 `@Transactional` 추가, `ActorRepositoryTest` / `MovieRepositoryTest` 신규 추가 (테넌트별 스키마 격리 검증, 28개)
- **`10-multi-tenant/02-multitenant-spring-web-virtualthread`**: 동일한 리포지토리 테스트 추가 (가상 스레드 환경, 28개)
- **`10-multi-tenant/03-multitenant-spring-webflux`**: WebFlux 환경 도메인 리포지토리 테스트 추가 (12개)
- **`11-high-performance/01-cache-strategies`**: 캐시 무효화(단일/복수) 및 단일 이벤트 write-behind 테스트 추가 (31 → 35개)
- **`11-high-performance/02-cache-strategies-coroutines`**: 코루틴 스타일 동일 테스트 추가 (31 → 35개)
- **`11-high-performance/03-routing-datasource`**: `InMemoryDataSourceRegistry` 중복키/없는키 조회, `ContextAwareRoutingKeyResolver` null supplier·read-only 키 테스트 추가 (21 → 25개)

---

## [1.1.1] - 2026-03-14

### Added

- **`11-high-performance/04-benchmark`**: `kotlinx-benchmark` 기반 마이크로벤치마크 모듈 추가
    - `RoutingKeyResolverBenchmark`: 라우팅 키 계산 오버헤드 측정
    - `ReadThroughCacheBenchmark`: cache hit / cache miss 비교
    - smoke 프로파일(`smokeBenchmark`) 및 Markdown 리포트 생성(`benchmarkMarkdown`) 지원
- `.editorconfig` 파일 추가

### Changed

- **Kotlin**: `2.3.20-RC3` → `2.3.20` (정식 릴리스)
- **Bluetape4k**: `1.4.0` → `1.5.0-Beta1`
    - `bluetape4k-jackson` → `bluetape4k-jackson2` 리네이밍 대응
    - `bluetape4k-crypto` Deprecated → `bluetape4k-tink` 대체 반영
- Kotlin 2.3 기준으로 빌드 설정 전면 전환 (`languageVersion = 2.3`, `apiVersion = 2.3`)
- Redisson Client 설정 개선 (netty 설정 변경, Virtual Threads 적용)

### Fixed

- `RedissonClient` 설정에서 netty 관련 설정 오류 수정 (`5982dcd7`, `c9870321`, `b1908633`, `d4b7dd01`)

---

## [1.0.5] - 2026-03-12

### Added

- **`bin/repo-status`**, **`bin/repo-diff`**, **`bin/repo-test-summary`**: 토큰 절약형 저장소 요약 헬퍼 스크립트 추가
- **Redisson 설정 최적화**: Virtual Threads 기반 Redisson Client 설정 적용 (`dd8f3042`)

### Changed

- **Bluetape4k**: `1.3.1` → `1.4.0`
- **Kotlin**: `2.2.21` → `2.3.20-RC3`
    - `vertx-sqlclient-example` compileTestKotlin hang 문제 우회 처리
- 빌드 속도 개선 (configuration cache 활용 강화)

### Fixed

- `vertx-sqlclient-example` 컴파일 시 hang 걸리는 문제 해결

---

## [1.0.0] - 2025-12-01 (초기 릴리스)

### Added

- **워크샵 전체 구조** 구성 (모듈 `00` ~ `11`)
- **`00-shared/exposed-shared-tests`**: 공통 테스트 인프라 (`AbstractExposedTest`, `TestDB`, `WithTables`)
- **`01-spring-boot`**: Spring MVC + Virtual Threads, Spring WebFlux + Coroutines 예제
- **`02-alternatives-to-jpa`**: R2DBC, Vert.x SQL Client, Hibernate Reactive 비교 예제
- **`03-exposed-basic`**: DSL / DAO 패턴 기초 예제
- **`04-exposed-ddl`**: 연결 관리, 스키마 정의
- **`05-exposed-dml`**: SELECT/INSERT/UPDATE/DELETE, 컬럼 타입, SQL 함수, 트랜잭션, Entity API
- **`06-advanced`
  **: Crypt, JavaTime, kotlinx-datetime, JSON, Money, 커스텀 컬럼, 커스텀 Entity, Jackson, Fastjson2, Jasypt, Jackson3, Tink
- **`07-jpa`**: JPA → Exposed 마이그레이션 (기본/고급)
- **`08-coroutines`**: Coroutines 기반 비동기 트랜잭션, Virtual Threads
- **`09-spring`
  **: Spring Boot AutoConfiguration, TransactionTemplate, @Transactional, Repository 패턴, Spring Cache, Suspended Cache
- **`10-multi-tenant`**: Schema-based 멀티테넌시 (MVC, Virtual Threads, WebFlux)
- **`11-high-performance`**: Read/Write Through/Behind 캐시 전략, RoutingDataSource

[Unreleased]: https://github.com/bluetape4k/exposed-workshop/compare/v1.1.1...HEAD

[1.1.1]: https://github.com/bluetape4k/exposed-workshop/compare/v1.0.5...v1.1.1

[1.0.5]: https://github.com/bluetape4k/exposed-workshop/compare/v1.0.0...v1.0.5

[1.0.0]: https://github.com/bluetape4k/exposed-workshop/releases/tag/v1.0.0
