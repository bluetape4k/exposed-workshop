# 01 Spring Boot: Spring WebFlux with Exposed

Spring WebFlux + Kotlin Coroutines 환경에서 Exposed를 논블로킹 API로 사용하는 모듈입니다. Reactor Context + coroutine-aware 트랜잭션을 통해 영화/배우 데이터를 처리합니다.

## 학습 목표

- `newSuspendedTransaction`을 이용한 suspend 트랜잭션을 이해한다.
- Reactor Context 기반 `TenantId`/`CoroutineContext` 전파를 숙지한다.
- Netty/Swagger 설정을 통해 Reactive API를 문서화한다.

## 선수 지식

- [`00-shared/exposed-shared-tests`](../../00-shared/exposed-shared-tests/README.md): 공통 테스트 베이스 클래스와 DB 설정 참고
- Kotlin Coroutines 기본 개념

## 핵심 개념

- Reactor + Coroutine 환경에서 Database 트랜잭션 관리 (`newSuspendedTransactionWithCurrentReactorTenant`)
- WebFlux Controller의 suspend 핸들러와 비동기 응답 흐름
- Swagger/OpenAPI + Netty `LoopResources`, Timeout 튜닝

## 주요 파일 구성

| 파일 | 설명 |
|------|------|
| `src/main/kotlin/.../controller/ActorController.kt` | 배우 REST API (suspend 핸들러) |
| `src/main/kotlin/.../controller/MovieController.kt` | 영화 REST API (suspend 핸들러) |
| `src/main/kotlin/.../domain/repository/ActorRepository.kt` | Exposed DSL 기반 배우 Repository |
| `src/test/kotlin/.../controller/ActorControllerTest.kt` | 배우 API 통합 테스트 |

## 실행 방법

```bash
# 애플리케이션 기동
./gradlew :spring-webflux-exposed:bootRun

# 테스트 실행
./gradlew :spring-webflux-exposed:test
```

## 실습 체크리스트

- `GET /actors` 응답이 suspend 경로에서 정상 반환되는지 확인
- Netty EventLoop/ConnectionProvider 설정에 따라 처리량 변화 관찰

## 성능·안정성 체크포인트

- Reactor Context 누수 없이 `TenantId`가 전달되는지 검증
- Netty 타임아웃/리소스 설정이 연결 안정성을 보장하는지 확인

## 핵심 시나리오 설명

### WebFlux Actor API 통합 테스트

`WebTestClient`와 `runSuspendIO`를 조합해 suspend 핸들러의 전체 비동기 흐름을 검증한다.
생성 후 삭제(POST → DELETE), 잘못된 파라미터 방어 처리 등의 시나리오가 포함된다.

관련 파일:
- 배우 컨트롤러 테스트: [`src/test/kotlin/exposed/workshop/springwebflux/controller/ActorControllerTest.kt`](src/test/kotlin/exposed/workshop/springwebflux/controller/ActorControllerTest.kt)

### newSuspendedTransaction + Reactor Context 전파

`newSuspendedTransactionWithCurrentReactorTenant`를 통해 Reactor Context에 담긴
`TenantId`가 Exposed 트랜잭션 내부까지 전파되는지 검증한다.
컨텍스트 누수 없이 올바르게 전달되는지 확인하는 것이 핵심 학습 포인트다.

### 잘못된 파라미터 방어 처리

birthday 등 날짜 파라미터에 유효하지 않은 값이 전달될 경우 예외를 던지지 않고
전체 목록을 반환하는 방어 로직을 `search actors ignores invalid birthday parameter` 테스트로 검증한다.

## 다음 챕터

- [02-alternatives-to-jpa](../02-alternatives-to-jpa/README.md)
