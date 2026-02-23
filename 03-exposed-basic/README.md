# 03 Exposed Basic

Exposed를 처음 만나보는 학습 적응 장치로, DSL과 DAO를 나란히 비교하며 공통 조회/저장 흐름을 테스트 기반으로 익히는 챕터입니다.

## 챕터 목표

- DSL과 DAO의 역할 차이를 명확히 이해한 후, 공통 CRUD 시나리오를 재현한다.
- 테스트 코드로 조건/정렬/페이징 결과를 검증해 안정적인 쿼리 작성 경험을 확보한다.
- 이후 `04-exposed-ddl`, `05-exposed-dml`에서 재사용할 구조(스키마/유틸 클래스)를 정리한다.

## 선수 지식

- Kotlin 기본 문법과 함수형 관용구
- 관계형 데이터베이스 기본 개념(테이블, PK/FK)

## 포함 모듈

| 모듈                    | 설명                                                      |
|-----------------------|---------------------------------------------------------|
| `exposed-sql-example` | DSL 중심으로 SELECT/INSERT/UPDATE/DELETE 기본 흐름을 확인하는 테스트 예제 |
| `exposed-dao-example` | DAO(Entity) 모델링, 관계 매핑, 코루틴 트랜잭션 사례를 담은 예제              |

## 권장 학습 순서

1. `exposed-sql-example`
2. `exposed-dao-example`

## 실행 방법

```bash
./gradlew :exposed-03-exposed-basic-exposed-sql-example:test
./gradlew :exposed-03-exposed-basic-exposed-dao-example:test
```

## 테스트 포인트

- DSL/DAO 각각에서 동일 비즈니스 시나리오를 재현할 수 있는지 검증
- 조회 조건, 정렬, 페이징 결과가 기대값과 일치하는지 확인

## 성능·안정성 체크포인트

- N+1 가능성이 있는 조회 패턴을 조기에 식별
- 트랜잭션 경계 밖에서 Entity 지연 접근이 발생하지 않도록 테스트로 고정

## 다음 챕터

- [04-exposed-ddl](../04-exposed-ddl/README.md): DB 연결과 스키마 정의 실습으로 확장합니다.
