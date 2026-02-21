# 03 Exposed 기본: Exposed DAO 예제

이 모듈(`exposed-dao-example`)은 Kotlin의 강력한 ORM 프레임워크인 Exposed에서 DAO(Data Access Object) 패턴을 사용하는 실용적인 예제를 제공합니다. `Entity`와
`EntityClass` 객체를 사용하여 관계형 데이터베이스와 상호작용하는 방법을 보여주며, 간단한 "City와 User" 도메인을 위한 기본적인 CRUD(Create, Read, Update, Delete) 연산과 관계 관리를 다룹니다. 동기식과 비동기식(코루틴 기반) 데이터 접근 방식을 모두 제공합니다.

## 주요 기능 및 구성 요소

### 1. 도메인 스키마 (`Schema.kt`)

- **테이블 정의**: `IntIdTable`을 사용하여 `CityTable`과 `UserTable`을 정의하고, 도시와 사용자를 위한 스키마를 구축합니다.
- **Entity 정의**: `City`와 `User`는 `IntEntity` 클래스로 정의되며, 각 테이블에 매핑되는 DAO 엔티티를 나타냅니다.
- **관계**: Exposed의 `referrersOn`과 `referencedOn` 기능을 사용하여 일대다 관계(도시의 사용자들)와 다대일 관계(사용자의 도시)를 보여줍니다.
- **테스트 설정 헬퍼**: 스키마 생성과 샘플 데이터 채우기를 포함한 테스트 환경 구성을 위한 `withCityUsers`와 `withSuspendedCityUsers`를 제공합니다.

### 2. 동기식 DAO 연산 (`ExposedDaoExample.kt`)

- **Entity 조회**: `all()`과 `find { ... }` 메서드를 사용하여 단일 또는 복수의 `City`와 `User` 엔티티를 조회하는 예제입니다.
- **Eager Loading**: `.with(Entity::property)` 확장 함수를 사용하여 관련 엔티티를 효율적으로 가져오는 방법을 보여줍니다.
- **데이터 필터링**: 다양한 조건의 `WHERE` 절을 적용하여 엔티티를 필터링합니다.
- **Entity 생성**: 새로운 `User` 엔티티를 생성하는 예제입니다.
- **Entity 수정**: 기존 엔티티 프로퍼티를 수정하는 예제입니다.
- **Entity 삭제**: 데이터베이스에서 엔티티를 삭제하는 예제입니다.

### 3. 비동기 DAO 연산 (`ExposedDaoSuspendedExample.kt`)

- **코루틴 통합**: Kotlin Coroutines의 suspend 컨텍스트 내에서 동일한 DAO 연산(조회, eager loading, 필터링)을 수행하는 방법을 보여줍니다.
- **반응형 데이터 접근**: `runSuspendIO`와
  `withSuspendedCityUsers`를 사용하여 논블로킹 데이터베이스 상호작용을 구현하며, Exposed가 반응형 애플리케이션에 적합함을 보여줍니다.

## 목적

이 모듈은 다음을 이해하는 데 도움이 됩니다:

- Exposed의 DAO 패턴 핵심 개념
- 도메인 엔티티와 그 관계를 모델링하는 방법
- 엔티티를 사용한 표준 데이터베이스 연산(CRUD) 수행
- 비동기 프로그래밍을 위한 Exposed DAO와 Kotlin Coroutines 통합

## 시작하기

이 예제들을 살펴보려면:

1. `src/test/kotlin/exposed/dao/example`의 소스 코드를 검토합니다.
2. IDE 또는 Gradle을 사용하여 테스트 케이스를 실행하고 Exposed의 DAO 기능을 동기식 및 비동기식으로 확인합니다.

이 모듈은 Exposed DAO의 기본을 마스터하기 위한 명확하고 간결한 가이드를 제공합니다.

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

## 테스트 실행

```bash
# 전체 테스트 실행
./gradlew :03-exposed-basic:exposed-dao-example:test
```

모든 테스트는 `@ParameterizedTest`를 사용하여 H2, MySQL, PostgreSQL 등 다양한 데이터베이스 환경에서 실행됩니다.

## 더 읽어보기

- [7.1 DML 함수](https://debop.notion.site/1ad2744526b0800baf1ce81c31f3cbf9?v=1ad2744526b08007ab62000c0901bcfa)
