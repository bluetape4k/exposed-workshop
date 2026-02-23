# 09 Spring: Declarative Transaction (03)

`@Transactional` 중심 선언적 트랜잭션 통합 모듈입니다. Spring 트랜잭션 속성과 Exposed 트랜잭션 동작을 정렬하는 방법을 다룹니다.

## 학습 목표

- 선언적 트랜잭션 경계 설계를 익힌다.
- 전파/격리/읽기전용/타임아웃 설정을 이해한다.
- 예외 기반 롤백 규칙을 테스트로 검증한다.

## 선수 지식

- [`../02-transactiontemplate/README.md`](../02-transactiontemplate/README.md)

## 핵심 개념

- `@Transactional` 속성
- 트랜잭션 전파와 롤백
- 읽기 전용 최적화

## 실행 방법

```bash
./gradlew :exposed-09-spring-03-spring-transaction:test
```

## 실습 체크리스트

- 전파 속성 변경에 따른 동작 차이를 비교한다.
- checked/unchecked 예외별 롤백 규칙을 확인한다.

## 성능·안정성 체크포인트

- 읽기 쿼리 경로에 readOnly 적용
- 장시간 트랜잭션과 외부 I/O 혼합을 피함

## 다음 모듈

- [`../04-exposed-repository/README.md`](../04-exposed-repository/README.md)
