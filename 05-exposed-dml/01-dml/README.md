# 05 Exposed DML: 기본 연산 (01-dml)

Exposed DSL로 **조회(SELECT), 삽입(INSERT), 수정(UPDATE)
** 의 기본 패턴을 정리한 모듈입니다. 각 예제는 테스트 케이스로 구성되어 있어, 다양한 DB Dialect에서 동일한 동작을 검증할 수 있습니다.

## 학습 목표

- Exposed DSL의 기본 DML 문법 흐름을 익힌다.
- 조건식 조합, 서브쿼리, 페이징 등 실무에서 자주 쓰는 패턴을 익힌다.
- insert/update 시 ID 반환, conflict 처리, batch 처리 등 부가 기능을 이해한다.

## 예제 구성

모든 예제는 `src/test/kotlin/exposed/examples/dml` 아래에 있습니다.

### Ex01_Select.kt - SELECT 기본

기본 조건/조합, `inList`, 서브쿼리, `distinct`, `limit/offset`까지 SELECT의 전반을 다룹니다.

```kotlin
users.selectAll()
    .where { users.id eq "andrey" }
    .andWhere { users.name.isNotNull() }
```

### Ex02_Insert.kt - INSERT 기본

`insertAndGetId`, `insertIgnore`, `batchInsert`, 표현식 기반 insert, DAO insert 등.

```kotlin
val id = idTable.insertAndGetId { it[name] = "name-1" }
id.value shouldBeEqualTo 1
```

### Ex03_Update.kt - UPDATE 기본

기본 update, limit update, join update, 다중 join update.

```kotlin
users.update(where = { users.id eq "alex" }) {
    it[users.name] = "Alexey"
}
```

### Ex04_Upsert.kt - UPSERT

PK/복합키 기준 upsert와 batch upsert, update/insert 분기 로직.

```kotlin
AutoIncTable.upsert {
    it[id] = id1
    it[name] = "C"
}
```

### Ex05_Delete.kt - DELETE

`deleteWhere`, `deleteIgnoreWhere`, `deleteAll`, join 기반 delete, limit delete.

```kotlin
users.deleteWhere { users.name like "%thing" }
```

### Ex06_Exists.kt - EXISTS / NOT EXISTS

`exists`, `notExists` 조건식 사용과 DB별 boolean 처리.

```kotlin
users.selectAll().where {
    exists(userData.select(userData.userId).where { userData.userId eq users.id })
}
```

### Ex07_DistinctOn.kt - DISTINCT ON

Postgres/H2 전용 `withDistinctOn` 사용과 정렬 조합.

```kotlin
tester.selectAll()
    .withDistinctOn(tester.v1)
    .orderBy(tester.v1 to SortOrder.ASC)
```

### Ex08_Count.kt - COUNT / COUNT DISTINCT

`count`, `countDistinct`, groupBy/limit/offset과 함께 동작하는 count.

```kotlin
val cityCount = cities.id.countDistinct()
cities.select(cityCount).single()[cityCount]
```

### Ex09_GroupBy.kt - GROUP BY / HAVING

집계 함수, `groupBy`, `having`, `groupConcat`, `max` 등.

```kotlin
cities.innerJoin(users)
    .select(cities.name, users.id.count())
    .groupBy(cities.name)
```

### Ex10_OrderBy.kt - ORDER BY

NULL 정렬 차이, expression/subquery 기반 정렬.

```kotlin
users.selectAll()
    .orderBy(users.cityId, SortOrder.DESC)
    .orderBy(users.id)
```

### Ex11_Join.kt - JOIN

inner/cross join, 다중 join, many-to-many join, alias join.

```kotlin
cities.innerJoin(users).innerJoin(userData)
    .selectAll()
    .orderBy(users.id)
```

### Ex12_InsertInto_Select.kt - INSERT INTO SELECT

select 결과를 insert로 넣기, expression/limit/columns 지정.

```kotlin
cities.insert(users.select(slice).orderBy(users.id).limit(2))
```

### Ex13_Replace.kt - REPLACE INTO (MySQL/MariaDB)

`replace`, `batchReplace`, select 기반 replace.

```kotlin
NewAuth.replace {
    it[username] = "username"
    it[session] = "session".toByteArray()
}
```

### Ex14_MergeBase.kt - MERGE 테스트 기반

`MERGE INTO` 테스트용 공통 테이블/데이터 구성.

```kotlin
Source.insert(key = "only-in-source-1", value = 1)
Dest.insert(key = "only-in-dest-1", value = 10)
```

### Ex14_MergeTable.kt - MERGE FROM (Table)

`whenNotMatchedInsert`, `whenMatchedUpdate`, `whenMatchedDelete`.

```kotlin
dest.mergeFrom(source, on = { Source.key eq Dest.key }) {
    whenMatchedUpdate { it[dest.value] = (source.value + dest.value) * 2 }
}
```

### Ex14_MergeSelect.kt - MERGE FROM (Select)

subquery를 소스로 사용하는 MERGE.

```kotlin
dest.mergeFrom(sourceQuery, on = { Dest.key eq sourceQuery[Source.key] }) {
    whenNotMatchedInsert { it[dest.key] = sourceQuery[Source.key] }
}
```

### Ex15_Returning.kt - RETURNING

`insertReturning`, `upsertReturning`, `updateReturning`, `deleteReturning` (Postgres/MariaDB).

```kotlin
val row = Items.insertReturning { it[name] = "A"; it[price] = 99.0 }.single()
```

### Ex16_FetchBatchedResults.kt - 배치 조회

`fetchBatchedResults`로 대량 결과를 배치 단위로 가져오기.

```kotlin
cities.selectAll()
    .where { cities.id less 51 }
    .fetchBatchedResults(batchSize = 25)
```

### Ex17_Union.kt - UNION / INTERSECT / EXCEPT

`union`, `unionAll`, `intersect`, `except`, `orderBy`, `limit/offset`.

```kotlin
andreyQuery.union(sergeyQuery).limit(1).offset(1)
```

### Ex20_AdjustQuery.kt - Query 조정

`adjustSelect`, `adjustColumnSet`, `adjustWhere`, `adjustHaving`으로 기존 쿼리 수정.

```kotlin
query.adjustSelect { select(users.name, cities.name) }
query.adjustWhere { (users.id eq "andrey") or (users.name eq "Sergey") }
```

### Ex21_Arithmetic.kt - 산술 연산

컬럼/리터럴 산술 연산, scale 있는 나눗셈.

```kotlin
val expr = ((userData.value - 5) * 2) / 2
userData.select(userData.value, expr)
```

### Ex22_ColumnWithTransform.kt - Column 변환

`ColumnTransformer`, `ColumnWithTransform`, 중첩 transform.

```kotlin
val v1 = integer("v1").transform(
    wrap = { Holder(it) },
    unwrap = { it.value }
)
```

### Ex23_Conditions.kt - 조건식 모음

`isDistinctFrom`, `between`, `Coalesce`, `Case`, `Op.TRUE/FALSE` 등.

```kotlin
table.selectAll().where { table.number1 isNotDistinctFrom table.number2 }
```

### Ex30_Explain.kt - EXPLAIN

`explain { ... }`로 실행 계획 출력, `ANALYZE` 옵션 차이.

```kotlin
explain { Countries.insert { it[code] = "ABC" } }.toList()
```

### Ex40_LateralJoin.kt - LATERAL JOIN

Postgres 전용 LATERAL JOIN과 alias 사용.

```kotlin
parent.joinQuery(joinType = JoinType.CROSS, lateral = true) {
    child.selectAll().where { child.value greater parent.value }.limit(1)
}
```

### Ex50_RecursiveCTE.kt - 재귀 CTE

Raw SQL로 recursive CTE 실행 (Postgres/MySQL/MariaDB).

```kotlin
exec(sql, explicitStatementType = StatementType.SELECT) { rs -> /* map */ }
```

### Ex99_Dual.kt - DUAL 테이블

`Table.Dual`로 단일 값/현재 날짜 조회.

```kotlin
val result = Table.Dual.select(intLiteral(1)).single()
```

### join_diagram.png - JOIN 다이어그램

`Ex11_Join.kt` 이해를 돕기 위한 조인 다이어그램입니다.

## 테스트 실행

```bash
./gradlew :exposed-05-exposed-dml-01-dml:test
```

모든 테스트는 `@ParameterizedTest`로 H2, MySQL, PostgreSQL 등 여러 DB에서 실행됩니다.

## Further Reading

- [7.1 DML 함수](https://debop.notion.site/1ad2744526b0800baf1ce81c31f3cbf9?v=1ad2744526b08007ab62000c0901bcfa)
