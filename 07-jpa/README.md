# 07 JPA Migration

기존 JPA 중심 코드베이스를 Exposed로 전환할 때 필요한 전략을 단계적으로 정리하는 챕터입니다.

## 챕터 목표

- JPA와 Exposed의 개념/동작 차이를 비교하고, 전환 리스크를 줄이는 패턴을 이해한다.
- 기본 CRUD부터 복잡한 관계/트랜잭션까지 점진적으로 전환할 수 있는 전략을 수립한다.
- 테스트 중심의 회귀 방지 절차를 정립한다.

## 선수 지식

- JPA/Hibernate 기본 사용 경험
- `05-exposed-dml` 내용 (DSL/DAO 트랜잭션 흐름)

## 포함 모듈

| 모듈                        | 설명                             |
|---------------------------|--------------------------------|
| `01-convert-jpa-basic`    | 기본 CRUD 전환 시나리오 및 Entity 매핑 비교 |
| `02-convert-jpa-advanced` | 복잡 쿼리/관계/트랜잭션, 트리거/배치 전환 전략    |

## 권장 학습 순서

1. `01-convert-jpa-basic`
2. `02-convert-jpa-advanced`

## 실행 방법

```bash
./gradlew :01-convert-jpa-basic:test
./gradlew :02-convert-jpa-advanced:test
```

## 테스트 포인트

- 전환 전/후 동일 입력에 대해 결과가 동등한지 검증한다.
- 트랜잭션/락 동작이 기존 정책과 일관된지 확인한다.

## 성능·안정성 체크포인트

- 지연 로딩 의존 코드 제거 여부를 점검한다.
- 쿼리 수/응답 시간 회귀를 계측으로 검토한다.

## 다음 챕터

- [08-coroutines](../08-coroutines/README.md): 코루틴/Virtual Thread 기반 Exposed 운영 패턴으로 확장합니다.
