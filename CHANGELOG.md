# Changelog

모든 주요 변경 사항은 이 파일에 기록됩니다. 형식은 [Keep a Changelog](https://keepachangelog.com/ko/1.0.0/)를 따르며, 이 프로젝트는 [Semantic Versioning](https://semver.org/lang/ko/)을 따릅니다.

---

## [Unreleased]

### Added

- `.omc/` 디렉터리 추가 (OMC 상태 관리용)
- `.claude/worktrees` gitignore 규칙 추가

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
