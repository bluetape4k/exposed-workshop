# 03 Exposed Basic: Exposed DAO Example

This module (
`exposed-dao-example`) provides practical examples of using the Data Access Object (DAO) pattern in Exposed, Kotlin's powerful ORM framework. It demonstrates how to interact with a relational database using
`Entity` and
`EntityClass` objects, covering fundamental CRUD (Create, Read, Update, Delete) operations and relationship management for a simple "City and User" domain. The examples showcase both synchronous and asynchronous (coroutine-based) approaches to data access.

## Key Features and Components:

### 1. Domain Schema (`Schema.kt`)

- **Table Definitions**: Defines `CityTable` and `UserTable` using
  `IntIdTable`, establishing a schema for cities and users.
- **Entity Definitions**: `City` and `User` are defined as
  `IntEntity` classes, representing the DAO entities that map to the respective tables.
- **Relationships
  **: Demonstrates one-to-many relationship (users in a city) and many-to-one relationship (city for a user) using Exposed's
  `referrersOn` and `referencedOn` functionality.
- **Test Setup Helpers**: Includes `withCityUsers` and
  `withSuspendedCityUsers` for convenient setup of test environments, including schema creation and sample data population.

### 2. Synchronous DAO Operations (`ExposedDaoExample.kt`)

- **Querying Entities**: Examples of retrieving single or multiple `City` and `User` entities using `all()` and
  `find { ... }` methods.
- **Eager Loading**: Demonstrates how to eager load related entities using the
  `.with(Entity::property)` extension function to fetch relationships efficiently.
- **Filtering Data**: Applying `WHERE` clauses with various conditions to filter entities.
- **Creating Entities**: Examples of creating new `User` entities.
- **Updating Entities**: Modifying existing entity properties.
- **Deleting Entities**: Removing entities from the database.

### 3. Asynchronous DAO Operations (`ExposedDaoSuspendedExample.kt`)

- **Coroutine Integration
  **: Showcases how to perform the same DAO operations (querying, eager loading, filtering) within a Kotlin Coroutines suspended context.
- **Reactive-style Data Access**: Utilizes `runSuspendIO` and
  `withSuspendedCityUsers` to illustrate non-blocking database interactions, making Exposed suitable for reactive applications.

## Purpose:

This module serves as an excellent resource for understanding:

- The core concepts of Exposed's DAO pattern.
- How to model domain entities and their relationships.
- Performing standard database operations (CRUD) using entities.
- Integrating Exposed DAO with Kotlin Coroutines for asynchronous programming.

## Getting Started:

To explore these examples:

1. Review the source code in `src/test/kotlin/exposed/dao/example`.
2. Run the test cases using your IDE or Gradle to observe Exposed's DAO functionality in action, both synchronously and asynchronously.

This module provides a clear and concise guide to mastering the basics of Exposed DAO.

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
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    age INT NOT NULL,
    city_id INT NULL,
    CONSTRAINT fk_users_city_id__id
        FOREIGN KEY (city_id) REFERENCES cities(id)
);
```

### 2) Exposed Table 코드

`src/test/kotlin/exposed/dao/example/Schema.kt` 에 정의되어 있습니다.

```kotlin
object CityTable: IntIdTable("cities") {
  val name = varchar("name", 50)
}

object UserTable: IntIdTable("users") {
  val name = varchar("name", 50)
  val age = integer("age")
  val cityId = optReference("city_id", CityTable)
}
```

### 3) Exposed Entity 코드 (DAO 핵심)

```kotlin
class City(id: EntityID<Int>): IntEntity(id) {
  companion object: IntEntityClass<City>(CityTable)

  var name: String by CityTable.name
  val users: SizedIterable<User> by User optionalReferrersOn UserTable.cityId
}

class User(id: EntityID<Int>): IntEntity(id) {
  companion object: IntEntityClass<User>(UserTable)

  var name: String by UserTable.name
  var age: Int by UserTable.age
  var city: City? by City optionalReferencedOn UserTable.cityId
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
