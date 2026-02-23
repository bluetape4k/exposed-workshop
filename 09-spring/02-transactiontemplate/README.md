# 09 Spring: TransactionTemplate (02)

`TransactionTemplate` 기반 프로그래밍 트랜잭션 제어 모듈입니다. 선언적 트랜잭션보다 세밀한 경계 제어가 필요한 경우를 다룹니다.

## 학습 목표

- `TransactionTemplate.execute` 사용법을 익힌다.
- 예외/롤백 제어 패턴을 이해한다.
- 복합 트랜잭션 시나리오 구현 기준을 정한다.

## 선수 지식

- [`../01-springboot-autoconfigure/README.md`](../01-springboot-autoconfigure/README.md)

## 핵심 개념

- 명시적 트랜잭션 경계
- 반환값 기반 트랜잭션 흐름 제어
- 예외와 롤백 규칙

## 실행 방법

```bash
./gradlew :exposed-09-spring-02-transactiontemplate:test
```

## 실습 체크리스트

- 성공/실패 경로에서 커밋/롤백을 각각 검증
- 중첩 호출에서 트랜잭션 경계가 의도대로 유지되는지 확인

## 성능·안정성 체크포인트

- 과도한 세분화로 트랜잭션 가독성이 떨어지지 않게 유지
- 재시도/보상 트랜잭션 전략을 분리 설계

## 다음 모듈

- [`09-spring/03-spring-transaction/README.md`](../03-spring-transaction/README.md)
