# 09 Spring: Exposed Repository Coroutines (05)

코루틴 기반 Repository 패턴으로 Exposed를 비동기 처리하는 모듈입니다. 고동시성 환경에서 논블로킹 데이터 접근 구조를 학습합니다.

## 학습 목표

- `suspend` 저장소 메서드 설계를 익힌다.
- 코루틴 트랜잭션 컨텍스트 연동을 이해한다.
- 동기 저장소와의 역할 분리를 정리한다.

## 선수 지식

- [`../08-coroutines/01-coroutines-basic/README.md`](../08-coroutines/01-coroutines-basic/README.md)
- [`../04-exposed-repository/README.md`](../04-exposed-repository/README.md)

## 핵심 개념

- suspend repository
- 코루틴 컨텍스트 전파
- 비동기 트랜잭션

## 실행 방법

```bash
./gradlew :exposed-09-spring-05-exposed-repository-coroutines:test
```

## 실습 체크리스트

- 동기/코루틴 저장소 결과 동등성을 비교한다.
- 취소/타임아웃 상황에서 정리 동작을 검증한다.

## 성능·안정성 체크포인트

- 블로킹 JDBC 호출이 이벤트 루프를 점유하지 않도록 분리
- 코루틴 예외 전파 규칙을 서비스 계층에서 일관되게 처리

## 다음 모듈

- [`../06-spring-cache/README.md`](../06-spring-cache/README.md)
