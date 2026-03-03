# 01 Spring Boot: Spring WebFlux with Exposed

Spring WebFlux + Kotlin Coroutines 환경에서 Exposed를 논블로킹 API로 사용하는 모듈입니다. Reactor Context + coroutine-aware 트랜잭션을 통해 영화/배우 데이터를 처리합니다.

## 학습 목표

- `newSuspendedTransaction`을 이용한 suspend 트랜잭션을 이해한다.
- Reactor Context 기반 `TenantId`/`CoroutineContext` 전파를 숙지한다.
- Netty/Swagger 설정을 통해 Reactive API를 문서화한다.

## 선수 지식

- `/Users/debop/work/bluetape4k/exposed-workshop/00-shared/exposed-shared-tests/README.md`
- Kotlin Coroutines 기본 개념

## 핵심 개념

- Reactor + Coroutine 환경에서 Database 트랜잭션 관리 (`newSuspendedTransactionWithCurrentReactorTenant`)
- WebFlux Controller의 suspend 핸들러와 비동기 응답 흐름
- Swagger/OpenAPI + Netty `LoopResources`, Timeout 튜닝

## 실행 방법

```bash
./gradlew :spring-webflux-exposed:bootRun
```

## 실습 체크리스트

- `GET /actors` 응답이 suspend 경로에서 정상 반환되는지 확인
- Netty EventLoop/ConnectionProvider 설정에 따라 처리량 변화 관찰

## 성능·안정성 체크포인트

- Reactor Context 누수 없이 `TenantId`가 전달되는지 검증
- Netty 타임아웃/리소스 설정이 연결 안정성을 보장하는지 확인

## 다음 챕터

- [02-alternatives-to-jpa](../02-alternatives-to-jpa/README.md)
