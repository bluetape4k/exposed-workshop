# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Kotlin Exposed 프레임워크의 사용법을 단계별로 학습하는 Gradle 멀티모듈 워크샵 프로젝트입니다. Kotlin 2.3, Java 21, Spring Boot 3.x 기반으로 구성되어 있습니다.

## Build & Test Commands

```bash
# 전체 빌드 및 테스트
./gradlew clean build

# 전체 테스트
./gradlew test

# 특정 모듈 테스트 (모듈 경로 지정)
./gradlew :05-exposed-dml:01-dml:test

# 정적 분석 (detekt)
./gradlew detekt
```

모듈 경로는 `:<section>:<submodule>:test` 형식을 사용합니다. (예: `:09-spring:04-exposed-repository:test`)

공유 테스트 유틸리티 모듈:

```bash
./gradlew :exposed-shared-tests:test
```

## Module Structure

```
00-shared/exposed-shared-tests/   # 공통 테스트 유틸리티 (모든 모듈이 의존)
01-spring-boot/                   # Spring MVC + WebFlux 통합
02-alternatives-to-jpa/           # R2DBC, Vert.x SQL Client, Hibernate Reactive
03-exposed-basic/                 # DSL과 DAO 패턴 기초
04-exposed-ddl/                   # 연결 설정, 스키마 정의
05-exposed-dml/                   # SELECT/INSERT/UPDATE/DELETE, 트랜잭션, Entity API
06-advanced/                      # JSON, 암호화, 커스텀 타입, 금융 데이터
07-jpa/                           # JPA → Exposed 마이그레이션
08-coroutines/                    # Coroutines, Virtual Threads 통합
09-spring/                        # Spring 트랜잭션, 캐시, Repository 패턴
10-multi-tenant/                  # Schema-based 멀티테넌시
11-high-performance/              # 캐시 전략, RoutingDataSource
```

각 모듈은 `src/main/kotlin`과 `src/test/kotlin` 표준 레이아웃을 사용합니다.

## Key Architecture

### Dependency Management

모든 서브프로젝트는 루트 `build.gradle.kts`에서 공통 설정을 상속받습니다:

- BOM: `bluetape4k_bom`, `exposed_bom`, `kotlinx_coroutines_bom`, `spring_boot_dependencies`
- 버전 정보: `buildSrc/src/main/kotlin/Libs.kt`
- Java 21 toolchain, Kotlin 2.3 언어 수준
- 테스트 간 Mutex로 병렬 실행 방지 (`maxParallelUsages = 1`)

### Exposed 사용 패턴

- **DSL 방식**: `object`로 `Table`을 정의하고 `transaction { }` 블록 내에서 DSL 쿼리 작성
- **DAO 방식**: `Entity`와 `EntityClass`를 상속하여 ORM 스타일로 사용
- **코루틴**: `newSuspendedTransaction { }`, `suspendedTransactionAsync { }` 사용
- **Virtual Threads**: `Dispatchers.IO`와 함께 블로킹 코드 스타일 유지
- **Connection Pooling**: 테스트에서는 직접 연결, 프로덕션에서는 HikariCP 사용
- **패키지 import**: `org.jetbrains.exposed.v1.*` (v1 패키지 사용)

### Test Infrastructure (`00-shared/exposed-shared-tests`)

- **`TestDB`** enum: H2, PostgreSQL, MySQL V8, MariaDB, CockroachDB 등 지원. `USE_TESTCONTAINERS=true`가 기본값
- **`AbstractExposedTest`**: 모든 Exposed 테스트의 베이스 클래스. `enableDialects()` 메서드로 테스트 대상 DB 지정
- **`WithTables`** / **`WithTablesSuspending`**: 테스트 전후 테이블 생성/삭제 처리 헬퍼
- 기본 활성화 DB: `H2, POSTGRESQL, MYSQL_V8, MARIADB` (`USE_FAST_DB=true`이면 H2만)
- Faker 기반 테스트 데이터 생성

## Coding Conventions

- **언어**: Kotlin only (Java 사용 금지), Kotlin extension 함수 적극 활용
- **들여쓰기**: 4 spaces
- **패키지**: 소문자 dot-separated (예: `exposed.examples.suspendedcache`)
- **타입**: PascalCase, 함수/변수: camelCase, 상수: UPPER_SNAKE_CASE
- **KDoc**: 공개 API에 한국어 KDoc 주석 추가
- **로깅**: `io.bluetape4k.logging.KLogging` companion object 사용

## Commit Conventions

한국어 커밋 메시지, Conventional Commit 접두사 사용:

- `feat:`, `fix:`, `refactor:`, `build:`, `docs:`, `chore:`, `test:`, `perf:`

## Key Libraries

- `exposed-core`, `exposed-jdbc`, `exposed-dao`, `exposed-java-time`: Exposed 핵심 모듈
- `bluetape4k-exposed`: bluetape4k의 Exposed 확장 유틸리티
- `bluetape4k-junit5`, `bluetape4k-testcontainers`: 테스트 지원
- `kluent`: 코틀린 스타일 assertion 라이브러리
- `mockk`: Kotlin 목 라이브러리
- `datafaker`, `random-beans`: 테스트 데이터 생성
- `kotlinx-atomicfu`: 원자적 연산 지원 (컴파일러 플러그인 방식)

## Compiler Options

실험적 API는 별도 opt-in 없이 사용 가능하도록 전역 설정:

- `ExperimentalCoroutinesApi`, `FlowPreview`, `DelicateCoroutinesApi`
- `-Xcontext-parameters`: Context 파라미터 지원
- `--enable-preview`: Java preview 기능 활성화
