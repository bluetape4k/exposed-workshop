# 08 Coroutines

[English](./README.md) | 한국어

이 챕터는 소스 예제가 실제로 다루는 두 가지 동시성 방식을 비교합니다. 하나는 Exposed의 코루틴 트랜잭션 API이고, 다른 하나는 bluetape4k의 Java Virtual Thread JDBC 헬퍼입니다. 테스트는 중첩 조회, 비동기 fan-out, insert/update 경합, 일반 `transaction { }`과의 혼용, 예외 발생 후 정리 동작처럼 트랜잭션 경계가 흔들리기 쉬운 지점을 중심으로 구성되어 있습니다.

## 챕터 목표

- `newSuspendedTransaction`, `withSuspendTransaction`, `suspendedTransactionAsync`로 이어지는 실제 접근 흐름을 이해한다.
- `newVirtualThreadJdbcTransaction`, `virtualThreadJdbcTransactionAsync` 기반 Virtual Thread 트랜잭션과 코루틴 트랜잭션을 비교한다.
- 병렬 실행, 재시도, 예외 전파 상황에서도 명확하게 유지되는 트랜잭션 경계를 설계한다.

## 선수 지식

- Kotlin Coroutines 기본 문법/Context 구조
- `05-exposed-dml/04-transactions`의 트랜잭션 패턴

## 코루틴 vs Virtual Thread 비교

| 항목               | Kotlin Coroutines                                      | Java Virtual Threads (Java 21+)                                        |
|------------------|--------------------------------------------------------|------------------------------------------------------------------------|
| API              | `newSuspendedTransaction`, `suspendedTransactionAsync` | `newVirtualThreadJdbcTransaction`, `virtualThreadJdbcTransactionAsync` |
| 코드 스타일           | `suspend` 함수, `await()`                                | 블로킹 스타일 유지 가능                                                          |
| 스레드 사용           | 적은 스레드 + Dispatcher 스케줄링                               | JVM이 자동으로 플랫폼 스레드에 마운트/언마운트                                            |
| 취소(Cancellation) | `Job.cancel()` + 구조적 동시성                               | `Future.cancel()` / `Thread.interrupt()`                               |
| DB 커넥션 관리        | Dispatcher.IO 풀 + 커넥션 풀 연동                             | Virtual Thread 수와 커넥션 풀 함께 조정 필요                                       |
| 기존 코드 마이그레이션     | `suspend` 키워드 추가 필요                                    | 블로킹 코드 그대로 사용 가능                                                       |
| 주 사용 사례          | 신규 비동기 코드베이스, Spring WebFlux 연동                        | 기존 동기 코드베이스의 동시성 확장                                                    |
| 최소 Java 버전       | 모든 버전                                                  | Java 21+                                                               |

## 동시성 모델 비교 다이어그램

### 코루틴 vs Virtual Thread 처리 흐름

![vs Virtual Thread diagram](../docs/images/readme-diagrams/08-coroutines-architecture-01.png)

### 스레드 모델 구조 비교

![08 coroutines Architecture 2 diagram](../docs/images/readme-diagrams/08-coroutines-architecture-02.png)

## 포함 모듈

| 모듈                        | 소스 기준 학습 범위 |
|---------------------------|---|
| `01-coroutines-basic`     | `Tester`, `TesterUnique`로 suspended transaction, 중첩 조회, 비동기 insert/update, 예외 정리를 검증 |
| `02-virtualthreads-basic` | Java 21에서만 실행되는 `VTester` 예제로 Virtual Thread 트랜잭션, async fan-out, 일반 transaction 혼용, SQL 예외 래핑을 검증 |

## 권장 학습 순서

1. `01-coroutines-basic`
2. `02-virtualthreads-basic`

## 실행 방법

```bash
# 서브모듈 단독 실행
./gradlew :01-coroutines-basic:test
./gradlew :02-virtualthreads-basic:test

# 전체 챕터 실행
./gradlew :01-coroutines-basic:test :02-virtualthreads-basic:test --no-parallel
```

## 트랜잭션 흐름 비교

### Coroutines Transaction Flow

![Coroutines Transaction Flow diagram](../docs/images/readme-diagrams/08-coroutines-architecture-03.png)

### Virtual Thread Transaction Flow

![Virtual Thread Transaction Flow diagram](../docs/images/readme-diagrams/08-coroutines-architecture-04.png)

## 테스트 포인트

- 취소(cancellation) 발생 시 자원 정리가 정상 동작하는지 확인한다.
- 병렬 처리 시 데이터 정합성이 유지되는지 검증한다.

## 성능·안정성 체크포인트

- 블로킹 호출이 Reactor/EventLoop를 점유하지 않도록 점검한다.
- 스레드/커넥션 풀 설정과 동시성 수준을 함께 튜닝한다.

## 복잡한 시나리오 가이드

### 코루틴 트랜잭션 패턴 (`01-coroutines-basic/`)

| 시나리오 | 구현 파일 |
|---|---|
| `newSuspendedTransaction`, `withSuspendTransaction` 기본 흐름 | [`Ex01_Coroutines.kt`](01-coroutines-basic/src/test/kotlin/exposed/examples/coroutines/Ex01_Coroutines.kt) |
| `suspendedTransactionAsync`, `awaitAll`, `maxAttempts`를 이용한 병렬 insert/update | [`Ex01_Coroutines.kt`](01-coroutines-basic/src/test/kotlin/exposed/examples/coroutines/Ex01_Coroutines.kt) |

### Virtual Thread 트랜잭션 패턴 (`02-virtualthreads-basic/`)

| 시나리오 | 구현 파일 |
|---|---|
| Java 21 조건에서 실행되는 `newVirtualThreadJdbcTransaction` 기본 흐름 | [`Ex01_VirtualThreads.kt`](02-virtualthreads-basic/src/test/kotlin/exposed/examples/virtualthreads/Ex01_VirtualThreads.kt) |
| `virtualThreadJdbcTransactionAsync`, `VirtualFuture.awaitAll()` 기반 async fan-out | [`Ex01_VirtualThreads.kt`](02-virtualthreads-basic/src/test/kotlin/exposed/examples/virtualthreads/Ex01_VirtualThreads.kt) |
| Virtual Thread 트랜잭션과 일반 `transaction { }` 혼용 | [`Ex01_VirtualThreads.kt`](02-virtualthreads-basic/src/test/kotlin/exposed/examples/virtualthreads/Ex01_VirtualThreads.kt) |
| 중복 엔티티 ID 예외 래핑과 커넥션 정리 검증 | [`Ex01_VirtualThreads.kt`](02-virtualthreads-basic/src/test/kotlin/exposed/examples/virtualthreads/Ex01_VirtualThreads.kt) |

## 다음 챕터

- [09-spring](../09-spring/README.md): Spring 통합 환경에서 Exposed 통합 패턴을 이어서 학습합니다.
