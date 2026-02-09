# 03 Exposed Basic: Exposed SQL DSL Example

This module (
`exposed-sql-example`) provides practical examples of using Exposed's SQL Domain Specific Language (DSL) for direct interaction with a relational database. It demonstrates how to perform fundamental CRUD (Create, Read, Update, Delete) operations, join tables, and utilize aggregate functions using Exposed's fluent API, without relying on the DAO (Data Access Object) pattern. The examples showcase both synchronous and asynchronous (coroutine-based) approaches to data access for a simple "City and User" domain.

## Key Features and Components:

### 1. Domain Schema (`Schema.kt`)

- **Table Definitions**: Defines `CityTable` and `UserTable` as
  `Table` objects, establishing a schema for cities and users. These definitions include explicit primary keys and a foreign key relationship.
- **Sample Data Insertion**:
  `insertSampleData` function demonstrates how to insert data directly using the SQL DSL, including examples of using SQL functions (
  `trim`, `substring`) within inserts.
- **Test Setup Helpers**: Includes `withCityUsers` and
  `withSuspendedCityUsers` for convenient setup of test environments, including schema creation and sample data population.

### 2. Synchronous SQL DSL Operations (`ExposedSQLExample.kt`)

- **Updating Data**: Examples of performing `UPDATE` statements with `WHERE` clauses to modify records.
- **Deleting Data**: Demonstrations of `DELETE` statements using `deleteWhere` with specific criteria.
- **Joining Tables**: Examples of `INNER JOIN` operations, showing how to connect `UserTable` and
  `CityTable` based on foreign key relationships, both implicitly and explicitly.
- **Querying and Filtering**: Performing `SELECT` queries with various `WHERE` conditions (`eq`, `like`, `isNull`,
  `or`) to filter and retrieve data.
- **Aggregate Functions and Grouping**: Usage of aggregate functions like `count()` in conjunction with
  `groupBy()` for analytical queries (e.g., counting users per city).

### 3. Asynchronous SQL DSL Operations (`ExposedSQLSuspendedExample.kt`)

- **Coroutine Integration
  **: Showcases how to perform the same SQL DSL operations (update, delete, join, query, aggregate) within a Kotlin Coroutines suspended context.
- **Reactive-style Data Access**: Utilizes `runSuspendIO` and
  `withSuspendedCityUsers` to illustrate non-blocking database interactions, making Exposed's SQL DSL suitable for reactive applications.

## Purpose:

This module serves as an excellent resource for understanding:

- The direct SQL DSL approach in Exposed for interacting with databases.
- How to perform common database operations (CRUD, joins, aggregation) using the fluent API.
- Integrating Exposed SQL DSL with Kotlin Coroutines for asynchronous programming.
- The differences between Exposed's DAO and SQL DSL patterns.

## Getting Started:

To explore these examples:

1. Review the source code in `src/test/kotlin/exposed/sql/example`.
2. Run the test cases using your IDE or Gradle to observe Exposed's SQL DSL functionality in action, both synchronously and asynchronously.

This module provides a clear and concise guide to mastering the direct SQL DSL capabilities of Exposed.

## City-User 스키마 한눈에 보기

![City-User Schema](CityUserSchema.png)

이 모듈의 모든 예제는 위 스키마를 기준으로 동작합니다. City는 `cities` 테이블, User는 `users` 테이블에 저장되며,
`users.city_id -> cities.id` 외래키 관계를 가집니다.

## 스키마와 Exposed 코드 (초보자용 요약)

### 1) DB 스키마 (DDL 요약)

```sql
CREATE TABLE cities (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL
);

CREATE TABLE users (
    id VARCHAR(10) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    city_id INT NULL,
    CONSTRAINT fk_users_city_id__id
        FOREIGN KEY (city_id) REFERENCES cities(id)
);
```

### 2) Exposed Table 코드 (이 모듈에서 실제 사용)

`src/test/kotlin/exposed/sql/example/Schema.kt` 에 정의되어 있습니다.

```kotlin
object CityTable: Table("cities") {
  val id = integer("id").autoIncrement()
  val name = varchar("name", length = 50)
  override val primaryKey = PrimaryKey(id, name = "PK_Cities_ID")
}

object UserTable: Table("users") {
  val id = varchar("id", length = 10)
  val name = varchar("name", length = 50)
  val cityId = optReference("city_id", CityTable.id)
  override val primaryKey = PrimaryKey(id, name = "PK_User_ID")
}
```

### 3) Exposed Entity 코드 (DAO 방식 참고용)

이 모듈은 SQL DSL만 사용하므로 Entity를 직접 사용하지 않습니다. 다만, 같은 스키마를 DAO 방식으로 매핑하면 아래와 같이 표현할 수 있습니다.

```kotlin
object CityTable: IntIdTable("cities") {
  val name = varchar("name", 50)
}

object UserTable: IdTable<String>("users") {
  val id = varchar("id", 10).entityId()
  val name = varchar("name", 50)
  val cityId = optReference("city_id", CityTable)
  override val primaryKey = PrimaryKey(id)
}

class City(id: EntityID<Int>): IntEntity(id) {
  companion object: IntEntityClass<City>(CityTable)

  var name by CityTable.name
  val users by User optionalReferrersOn UserTable.cityId
}

class User(id: EntityID<String>): Entity<String>(id) {
  companion object: EntityClass<String, User>(UserTable)

  var name by UserTable.name
  var city by City optionalReferencedOn UserTable.cityId
}
```

## DAO vs SQL DSL (간단 비교)

| 구분    | DAO                               | SQL DSL                     |
|-------|-----------------------------------|-----------------------------|
| 중심 개념 | `Entity` / `EntityClass`          | `Table` / `Query`           |
| 조회 방식 | `City.all()`, `User.find { ... }` | `CityTable.select { ... }`  |
| 관계 접근 | `city.users`, `user.city`         | `innerJoin`, `reference` 컬럼 |
| 변경 감지 | 엔티티 프로퍼티 변경 → flush               | 명시적 `update`/`insert`       |
| 사용 적합 | 객체 중심 도메인 모델                      | SQL 중심, 복잡한 쿼리              |
