# CLAUDE.md

Kotlin Exposed 워크샵 — Kotlin 2.3 / Java 21 / Spring Boot 3.x / Gradle 멀티모듈.

## 빌드 & 테스트

```bash
./gradlew clean build                                        # 전체 빌드
./gradlew :05-exposed-dml:01-dml:test                        # 특정 모듈 테스트
./gradlew detekt                                             # 정적 분석
./bin/repo-status                                           # git status 요약
./bin/repo-diff                                             # diff 파일별 요약
./bin/repo-test-summary -- ./gradlew :MODULE:test           # 테스트 로그 요약
```

모듈 경로: `:<section>:<submodule>:test` (예: `:09-spring:04-exposed-repository:test`)

**Token 절약 흐름**: `repo-status` → `repo-diff` → 필요한 파일만 상세 확인 → `repo-test-summary`

## 모듈 구조

| 모듈                               | 내용                                |
|----------------------------------|-----------------------------------|
| `00-shared/exposed-shared-tests` | 공통 테스트 유틸 (모든 모듈 의존)              |
| `01-spring-boot`                 | Spring MVC + WebFlux              |
| `02-alternatives-to-jpa`         | R2DBC, Vert.x, Hibernate Reactive |
| `03-exposed-basic`               | DSL / DAO 패턴 기초                   |
| `04-exposed-ddl`                 | 연결 설정, 스키마 정의                     |
| `05-exposed-dml`                 | SELECT/INSERT/UPDATE/DELETE, 트랜잭션 |
| `06-advanced`                    | JSON, 암호화, 커스텀 타입, 금융             |
| `07-jpa`                         | JPA → Exposed 마이그레이션              |
| `08-coroutines`                  | Coroutines, Virtual Threads       |
| `09-spring`                      | Spring 트랜잭션, 캐시, Repository       |
| `10-multi-tenant`                | Schema-based 멀티테넌시                |
| `11-high-performance`            | 캐시 전략, RoutingDataSource, 벤치마크    |

## 아키텍처 핵심

**의존성**: BOM(`bluetape4k_bom`, `exposed_bom`, `kotlinx_coroutines_bom`, `spring_boot_dependencies`), 버전 →
`buildSrc/src/main/kotlin/Libs.kt`, 테스트 병렬 실행 금지(`maxParallelUsages = 1`)

**Exposed 패턴**:

- DSL: `object Table` + `transaction { }` 블록
- DAO: `Entity` / `EntityClass` 상속
- 코루틴: `newSuspendedTransaction { }`, `suspendedTransactionAsync { }`
- import: `org.jetbrains.exposed.v1.*`

**테스트 인프라** (`00-shared/exposed-shared-tests`):

- `TestDB` enum: H2, PostgreSQL, MySQL V8, MariaDB (기본 활성, `USE_FAST_DB=true` → H2만)
- `AbstractExposedTest` + `enableDialects()` 로 대상 DB 지정
- `WithTables` / `WithTablesSuspending`: 테이블 생명주기 관리

## 코딩 규칙

- Kotlin only (Java 금지), extension 함수 적극 활용
- 들여쓰기 4 spaces, 패키지 소문자 dot-separated
- 타입 PascalCase / 함수·변수 camelCase / 상수 UPPER_SNAKE_CASE
- 공개 API → 한국어 KDoc, 로깅 → `KLogging` companion object

## 커밋

한국어 메시지 + Conventional Commit: `feat:` `fix:` `refactor:` `build:` `docs:` `chore:` `test:` `perf:`

## 주요 라이브러리

| 라이브러리                                            | 용도                 |
|--------------------------------------------------|--------------------|
| `exposed-core/jdbc/dao/java-time`                | Exposed 핵심         |
| `bluetape4k-exposed`                             | Exposed 확장 유틸      |
| `bluetape4k-junit5`, `bluetape4k-testcontainers` | 테스트 지원             |
| `kluent`                                         | Kotlin assertion   |
| `mockk`                                          | Kotlin mock        |
| `datafaker`, `random-beans`                      | 테스트 데이터            |
| `kotlinx-atomicfu`                               | 원자적 연산 (컴파일러 플러그인) |

## 컴파일러 옵션

전역 opt-in: `ExperimentalCoroutinesApi`, `FlowPreview`, `DelicateCoroutinesApi`
`-Xcontext-parameters` (Context 파라미터), `--enable-preview` (Java preview)
