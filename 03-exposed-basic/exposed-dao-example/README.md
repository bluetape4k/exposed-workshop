# 03 Exposed Basic: DAO 예제

Exposed DAO(Entity) 패턴의 기본을 학습하는 모듈입니다. Entity/EntityClass 모델링, 관계 매핑, CRUD, 코루틴 연동을 다룹니다.

## 학습 목표

- Entity 중심 데이터 접근 패턴을 익힌다.
- 관계 매핑(`referencedOn`, `referrersOn`)을 이해한다.
- DAO 스타일의 동기/코루틴 트랜잭션 사용법을 익힌다.

## 선수 지식

- [`../exposed-sql-example/README.md`](../exposed-sql-example/README.md)

## 핵심 개념

- `IntEntity`, `IntEntityClass`
- 관계 조회와 eager loading
- `newSuspendedTransaction` 기반 DAO 접근

## 예제 구성

| 파일                              | 설명                    |
|---------------------------------|-----------------------|
| `Schema.kt`                     | Entity/테이블/테스트 데이터 정의 |
| `ExposedDaoExample.kt`          | 동기 DAO CRUD           |
| `ExposedDaoSuspendedExample.kt` | 코루틴 DAO 예제            |

## 실행 방법

```bash
./gradlew :exposed-dao-example:test
```

## 실습 체크리스트

- DAO와 DSL로 동일 유스케이스를 각각 구현해 비교한다.
- 관계 조회 시 eager loading 유무에 따른 쿼리 수를 비교한다.

## 성능·안정성 체크포인트

- 트랜잭션 경계 밖에서 Entity 지연 접근을 피함
- 관계 탐색이 깊어질수록 N+1 위험을 테스트로 고정

## 다음 챕터

- [`../README.md`](../README.md)
