# 02 Alternatives to JPA

Spring 기반 애플리케이션에서 JPA 대안 기술들을 비교/실습하며, 다양한 ORM/Reactive 클라이언트를 검증하는 챕터입니다.

## 챕터 목표

- Exposed 외에 Hibernate Reactive, R2DBC, Vert.x SQL Client 같은 대체 스택을 소개한다.
- 각 스택의 선언적 패턴, 트랜잭션, ID 전략 차이를 실습으로 비교한다.
- 레거시 JPA 프로젝트에서 Exposed로 전환할 때 고려해야 할 기준을 정리한다.

## 선수 지식

- Spring Boot 및 DI/트랜잭션 기본 개념
- Exposed/DML 흐름 (`03-exposed-basic`, `05-exposed-dml`) 소개

## 포함 모듈

| 모듈                           | 설명                                             |
|------------------------------|------------------------------------------------|
| `hibernate-reactive-example` | Hibernate Reactive + Mutiny + PostgreSQL 기반 예제 |
| `r2dbc-example`              | Spring Data R2DBC를 사용한 비동기 데이터 접근 예제           |
| `vertx-sqlclient-example`    | Vert.x SQL Client 기반 이벤트 드리븐 예제                |

## 권장 학습 순서

1. `hibernate-reactive-example`
2. `r2dbc-example`
3. `vertx-sqlclient-example`

## 실행 방법

```bash
./gradlew :hibernate-reactive-example:bootRun
./gradlew :r2dbc-example:bootRun
./gradlew :vertx-sqlclient-example:bootRun
```

## 테스트 포인트

- 각 클라이언트에서 동일 도메인 결과가 일관된지 확인한다.
- Reactive/Async 경로에서 예외/타임아웃/롤백 동작을 점검한다.

## 성능·안정성 체크포인트

- Thread/Connection 모델 차이를 계량적으로 측정한다.
- DB pool 설정이 각 클라이언트 특성에 맞게 조정됐는지 검증한다.

## 다음 챕터

- [03-exposed-basic](../03-exposed-basic/README.md): Exposed DSL/DAO 학습으로 이어집니다.
