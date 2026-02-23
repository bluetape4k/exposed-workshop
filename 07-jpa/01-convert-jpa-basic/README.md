# 07 JPA Migration: 기본 전환 (01)

JPA 기본 CRUD/연관관계 코드를 Exposed로 전환하는 입문 모듈입니다. 기능 동등성을 유지하면서 의존성을 줄이는 전환 패턴을 다룹니다.

## 학습 목표

- JPA Entity 중심 코드를 Exposed DSL/DAO로 치환한다.
- 전환 전/후 결과 동등성 테스트를 작성한다.
- 점진적 전환 전략을 수립한다.

## 선수 지식

- JPA/Hibernate 기본
- [`../05-exposed-dml/README.md`](../05-exposed-dml/README.md)

## 핵심 개념

- CRUD 전환
- 단순 연관관계 전환
- 테스트 기반 회귀 방지

## 실행 방법

```bash
./gradlew :exposed-07-jpa-01-convert-jpa-basic:test
```

## 실습 체크리스트

- JPA 구현과 Exposed 구현의 결과를 같은 픽스처로 비교
- 예외 메시지/실패 코드가 기존 계약과 호환되는지 확인

## 성능·안정성 체크포인트

- 기본 조회에서 쿼리 수 회귀가 없는지 확인
- 트랜잭션 경계가 기존과 동일한지 검증

## 다음 모듈

- [`../02-convert-jpa-advanced/README.md`](../02-convert-jpa-advanced/README.md)
