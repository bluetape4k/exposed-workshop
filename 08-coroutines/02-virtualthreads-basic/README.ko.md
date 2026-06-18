# 08 Coroutines: Virtual Threads 기본 (02-virtualthreads-basic)

[English](./README.md) | 한국어

이 모듈은 Exposed 트랜잭션 예제를 Java 21 Virtual Thread 위에서 실행합니다. `Ex01_VirtualThreads.kt`는 블로킹 코드 스타일을 유지하되, 실행 경로를 `newVirtualThreadJdbcTransaction`과 `virtualThreadJdbcTransactionAsync`로 옮겨 fan-out, 일반 transaction 혼용, 예외 발생 후 정리 동작을 소스에서 확인할 수 있게 합니다.

## 학습 목표

- Java 21 조건에서 실행되는 테스트에서 `newVirtualThreadJdbcTransaction`을 사용한다.
- `virtualThreadJdbcTransactionAsync`, `VirtualFuture.awaitAll()`로 async fan-out을 실행한다.
- Virtual Thread 트랜잭션을 일반 `transaction { }` 및 앞 모듈의 코루틴 예제와 비교한다.

## 선수 지식

- Java 21+
- [`../01-coroutines-basic/README.md`](../01-coroutines-basic/README.md)

## 핵심 개념

### newVirtualThreadJdbcTransaction — 기본 사용

```kotlin
// Virtual Thread 위에서 트랜잭션 실행 (블로킹 스타일 유지)
newVirtualThreadJdbcTransaction {
    VTester.insert { }
    commit()
}

// 기존 트랜잭션에서 Virtual Thread 트랜잭션 중첩
fun JdbcTransaction.getTesterById(id: Int): ResultRow? =
    newVirtualThreadJdbcTransaction {
        VTester.selectAll()
            .where { VTester.id eq id }
            .singleOrNull()
    }
```

### virtualThreadJdbcTransactionAsync — 병렬 실행

```kotlin
// 여러 트랜잭션을 Virtual Thread로 병렬 실행
val futures: List<VirtualFuture<EntityID<Int>>> = (1..10).map {
    virtualThreadJdbcTransactionAsync {
        VTester.insertAndGetId { }
    }
}
val ids = futures.awaitAll()
```

## Virtual Thread 트랜잭션 흐름

![Virtual Thread diagram](../../docs/images/readme-diagrams/08-coroutines-02-virtualthreads-basic-sequence-01.png)

## 코루틴 vs Virtual Threads 실무 선택 가이드

| 상황                           | 권장 방식             |
|------------------------------|-------------------|
| 신규 비동기 코드베이스                 | Kotlin Coroutines |
| 기존 동기 블로킹 코드에 동시성 추가         | Virtual Threads   |
| Spring WebFlux / Reactive 연동 | Kotlin Coroutines |
| Spring MVC (서블릿 기반) + 높은 동시성 | Virtual Threads   |
| 취소(cancellation) 세밀한 제어      | Kotlin Coroutines |
| Java 17 이하 환경                | Kotlin Coroutines |
| Java 21+ 환경, 코드 변경 최소화       | Virtual Threads   |

## Virtual Thread 처리 모델 다이어그램

![Virtual Thread processing model diagram](../../docs/images/readme-diagrams/08-coroutines-02-virtualthreads-basic-architecture-02.png)

## Virtual Thread vs Platform Thread 비교 다이어그램

![Virtual Thread vs Platform Thread diagram](../../docs/images/readme-diagrams/08-coroutines-02-virtualthreads-basic-architecture-03.png)

## 테이블 ERD (virtualthreads_table)

![ERD (virtualthreads_table) diagram](../../docs/images/readme-diagrams/08-coroutines-02-virtualthreads-basic-erd-04.png)

## 예제 구성

소스 위치: `src/test/kotlin/exposed/examples/virtualthreads`

| 파일                       | 주요 테스트 시나리오 |
|--------------------------|---|
| `Ex01_VirtualThreads.kt` | Java 21 실행 조건, 존재하지 않는 ID 조회, 순차 Virtual Thread 트랜잭션, async fan-out, 일반 `transaction { }` 혼용, 중복 엔티티 ID 예외 래핑, 커넥션 정리 |

### 주요 테스트 시나리오

| 시나리오 | 사용 API |
|---|---|
| 기본 Virtual Thread 트랜잭션 | `newVirtualThreadJdbcTransaction` |
| `JdbcTransaction` receiver에서 중첩 조회 | `newVirtualThreadJdbcTransaction` |
| async fan-out insert/select 작업 | `virtualThreadJdbcTransactionAsync`, `VirtualFuture.awaitAll()` |
| 중복 엔티티 ID 예외 래핑 | `assertFailsWith<ExecutionException>`, 원인 `ExposedSQLException` |
| 일반 `transaction { }` 혼용 비교 | `transaction { }` vs `newVirtualThreadJdbcTransaction` |
| Java 21 전용 실행 조건 | `@EnabledForJreRange(min = JRE.JAVA_21)` |

## 실행 방법

```bash
./gradlew :02-virtualthreads-basic:test
```

> Java 21 이상 환경에서만 실행됩니다. `@EnabledForJreRange(min = JRE.JAVA_21)`로 보호되어 있습니다.

```bash
# Java 버전 확인
java -version

# 특정 Java 버전으로 실행
mise use java@21
./gradlew :02-virtualthreads-basic:test
```

## 실습 체크리스트

- 동시 작업 수를 늘려 처리량/지연시간 변화를 측정
- 예외 발생 시 롤백/정리 동작 검증
- 코루틴 버전과 Virtual Thread 버전의 같은 시나리오를 비교

## 성능·안정성 체크포인트

- Virtual Thread 증가와 DB 커넥션 수를 함께 조정
- 장시간 I/O 또는 외부 호출로 인한 병목을 분리
- `pinning` 현상 주의: `synchronized` 블록 내 블로킹 호출은 Virtual Thread를 플랫폼 스레드에 고정시킴

## 다음 챕터

- [`../../09-spring/README.md`](../../09-spring/README.md)
