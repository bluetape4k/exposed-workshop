# 03 Exposed Basic: SQL DSL 예제

Exposed SQL DSL의 기본 사용법을 학습하는 모듈입니다. 테이블 정의, CRUD, 조인, 집계, 코루틴 기반 비동기 쿼리까지 실습합니다.

## 학습 목표

- Exposed DSL로 타입 안전한 쿼리를 작성한다.
- CRUD/조인/집계를 DSL 스타일로 구현한다.
- 동기/코루틴 접근 방식의 차이를 이해한다.

## 선수 지식

- [`../README.md`](../README.md)

## 핵심 개념

- `select`, `insert`, `update`, `deleteWhere`
- `innerJoin`, `groupBy`, 집계 함수
- `newSuspendedTransaction` 기반 비동기 접근

## 예제 구성

| 파일                              | 설명                |
|---------------------------------|-------------------|
| `Schema.kt`                     | 테이블/샘플 데이터/테스트 헬퍼 |
| `ExposedSQLExample.kt`          | 동기 DSL 예제         |
| `ExposedSQLSuspendedExample.kt` | 코루틴 DSL 예제        |

## 실행 방법

```bash
./gradlew :exposed-03-exposed-basic-exposed-sql-example:test
```

## 실습 체크리스트

- 동일 시나리오를 동기/코루틴 경로로 각각 실행해 결과를 비교한다.
- 조인 + 집계 쿼리를 직접 확장해본다.

## 성능·안정성 체크포인트

- 복잡한 DSL 체인은 중간 표현식을 분리해 가독성을 유지
- 코루틴 경로에서 블로킹 호출 혼용을 피함

## 다음 모듈

- [`../exposed-dao-example/README.md`](../exposed-dao-example/README.md)
