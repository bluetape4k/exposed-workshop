# 05 Exposed DML: 기본 연산 (01-dml)

Exposed 1.0.0 DSL로 DML 핵심 문법(조회/삽입/수정/삭제/집계/조인)을 학습하는 모듈입니다. 모든 예제는 테스트 코드로 제공되어 DB Dialect 차이를 함께 검증할 수 있습니다.

## 학습 목표

- SELECT/INSERT/UPDATE/DELETE/UPSERT 패턴을 익힌다.
- 조건식, 조인, 집계, 서브쿼리, 페이징 조합을 실습한다.
- DB별 지원 여부가 다른 기능(예: DISTINCT ON, RETURNING, MERGE)을 구분해 사용한다.

## 선수 지식

- [`../04-exposed-ddl/README.md`](../04-exposed-ddl/README.md)
- Exposed DSL 기본 문법

## 핵심 개념

- 조회: `where`, `andWhere`, `exists`, `orderBy`, `groupBy`, `having`
- 변경: `insertAndGetId`, `batchInsert`, `update`, `deleteWhere`, `upsert`
- 고급 쿼리: `union`, `intersect`, `except`, `mergeFrom`, `fetchBatchedResults`

## 예제 지도

소스 위치: `src/test/kotlin/exposed/examples/dml`

| 범주     | 파일                                                                                                                                                                                                                                 |
|--------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 기본 DML | `Ex01_Select.kt`, `Ex02_Insert.kt`, `Ex03_Update.kt`, `Ex04_Upsert.kt`, `Ex05_Delete.kt`                                                                                                                                           |
| 조회 고급  | `Ex06_Exists.kt`, `Ex07_DistinctOn.kt`, `Ex08_Count.kt`, `Ex09_GroupBy.kt`, `Ex10_OrderBy.kt`, `Ex11_Join.kt`                                                                                                                      |
| 변경 고급  | `Ex12_InsertInto_Select.kt`, `Ex13_Replace.kt`, `Ex14_MergeBase.kt`, `Ex14_MergeTable.kt`, `Ex14_MergeSelect.kt`, `Ex15_Returning.kt`                                                                                              |
| 성능/확장  | `Ex16_FetchBatchedResults.kt`, `Ex17_Union.kt`, `Ex20_AdjustQuery.kt`, `Ex21_Arithmetic.kt`, `Ex22_ColumnWithTransform.kt`, `Ex23_Conditions.kt`, `Ex30_Explain.kt`, `Ex40_LateralJoin.kt`, `Ex50_RecursiveCTE.kt`, `Ex99_Dual.kt` |

## 실행 방법

```bash
./gradlew :exposed-05-exposed-dml-01-dml:test
```

## 실습 체크리스트

- 동일 쿼리를 H2/PostgreSQL/MySQL에서 실행해 결과 차이를 기록한다.
- `JOIN + GROUP BY + HAVING` 조합 쿼리를 직접 변형해본다.
- `RETURNING`, `MERGE`, `DISTINCT ON` 같은 DB 의존 기능은 대체 전략을 함께 정리한다.

## DB별 주의사항

- `withDistinctOn`: PostgreSQL/H2 중심으로 사용
- `replace`: MySQL/MariaDB 전용
- `returning`: DB 지원 여부 확인 필요
- `merge`: DB별 문법/지원 범위 차이 존재

## 성능·안정성 체크포인트

- 대량 조회는 `fetchBatchedResults`와 페이징으로 메모리 사용량을 제한
- 조인/집계 쿼리는 실행계획(`EXPLAIN`)으로 인덱스 사용 여부 확인
- 동적 조건 조합 시 `adjustWhere`로 쿼리 의도를 명확히 유지

## 다음 모듈

- [`../02-types/README.md`](../02-types/README.md)
