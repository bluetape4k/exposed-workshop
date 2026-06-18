# 03 Exposed Basic: DAO 예제

[English](./README.md) | 한국어

Exposed DAO(Entity) 패턴의 기본을 학습하는 모듈입니다. `IntIdTable`/`IntEntity` 모델링, `optionalReferencedOn`과 `optionalReferrersOn`을 사용하는 nullable 관계 매핑, eager loading, update/delete, 코루틴 트랜잭션을 다룹니다.

## 개요

Exposed DAO 패턴은 `IntIdTable`과 `IntEntity`/`IntEntityClass` 쌍으로 ORM 스타일의 데이터 접근을 제공합니다. `Schema.kt`는 `City`와 `User`를 `cities`, `users` 테이블에 매핑하고, `User.city`를 nullable 관계로 두며, 반대 방향인 `City.users` 컬렉션도 노출합니다. 테스트는 엔티티 조회, one-to-many와 many-to-one eager loading, 프로퍼티 기반 update, delete, `newSuspendedTransaction { }` 안에서의 동일 동작을 검증합니다.

## 학습 목표

- `IntIdTable`, `IntEntity`, `IntEntityClass` 기반 엔티티 모델링을 익힌다.
- `optionalReferencedOn`, `optionalReferrersOn`을 통해 nullable many-to-one, one-to-many 관계 매핑을 이해한다.
- `.with()` eager loading으로 관계 조회의 N+1 패턴을 방지한다.
- `newSuspendedTransaction` 기반 코루틴 트랜잭션에서 DAO를 사용한다.

## 선수 지식

- [`../exposed-sql-example/README.md`](../exposed-sql-example/README.md)

## ERD

![exposed dao example Entity Relationship diagram](../../docs/images/readme-diagrams/03-exposed-basic-exposed-dao-example-erd-01.png)

## 도메인 모델

![exposed dao example Class Structure 2 diagram](../../docs/images/readme-diagrams/03-exposed-basic-exposed-dao-example-class-02.png)

## 핵심 개념

### Entity / Table 선언

```kotlin
object CityTable: IntIdTable("cities") {
    val name = varchar("name", 50)
}

object UserTable: IntIdTable("users") {
    val name = varchar("name", 50)
    val age = integer("age")
    val cityId = optReference("city_id", CityTable)  // nullable FK
}

// City Entity — one-to-many: users
class City(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<City>(CityTable)

    var name: String by CityTable.name

    // one-to-many: 이 City에 속한 User 목록 (Lazy by default)
    val users: SizedIterable<User> by User optionalReferrersOn UserTable.cityId
}

// User Entity — many-to-one: city
class User(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<User>(UserTable)

    var name: String by UserTable.name
    var age: Int by UserTable.age

    // many-to-one: nullable FK
    var city: City? by City optionalReferencedOn UserTable.cityId
}
```

### CRUD

```kotlin
transaction {
    // INSERT
    val seoul = City.new { name = "Seoul" }
    val user = User.new {
        name = "debop"
        age = 56
        city = seoul
    }

    // SELECT by id
    val found = User.findById(user.id)

    // UPDATE — 트랜잭션 내에서 프로퍼티 변경 시 자동 반영
    found?.name = "debop (updated)"

    // DELETE
    found?.delete()
}
```

### Eager Loading으로 N+1 방지

```kotlin
// 문제 상황 (Lazy Loading — N+1 발생)
City.all().forEach { city ->
    city.users.forEach { user -> println(user.name) }  // N번 추가 쿼리
}

// 해결책 (Eager Loading — .with() 사용)
// City 1회 + User 1회 = 총 2회 쿼리
City.find { CityTable.name eq "Seoul" }
    .with(City::users)          // users를 미리 로딩
    .forEach { city ->
        city.users.forEach { println(it.name) }
    }
```

### 코루틴 트랜잭션 내 DAO 사용

```kotlin
// newSuspendedTransaction 내에서 Entity 접근
suspend fun withSuspendedCityUsers(testDB: TestDB, statement: suspend JdbcTransaction.() -> Unit) {
    withTablesSuspending(testDB, CityTable, UserTable) {
        populateSamples()
        flushCache()
        entityCache.clear()
        statement()
    }
}

// 사용 예시
withSuspendedCityUsers(testDB) {
    val users = User.find { UserTable.age greaterEq intLiteral(18) }
        .with(User::city)
        .toList()
}
```

## 예제 구성

| 파일                              | 설명                                 |
|---------------------------------|------------------------------------|
| `Schema.kt`                     | DAO 테이블/엔티티 정의, 샘플 행, 캐시 flush, 동기/suspending 헬퍼 |
| `ExposedDaoExample.kt`          | 동기 DAO 조회, eager loading, update, delete 테스트 |
| `ExposedDaoSuspendedExample.kt` | `withSuspendedCityUsers`로 같은 시나리오를 반복하는 코루틴 DAO 테스트 |

## 테스트 실행 방법

```bash
# 전체 테스트
./gradlew :exposed-dao-example:test

# H2만 대상으로 빠른 테스트
./gradlew :exposed-dao-example:test -PuseFastDB=true

# 특정 테스트 클래스만 실행
./gradlew :exposed-dao-example:test \
    --tests "exposed.dao.example.ExposedDaoExample"
```

## 복잡한 시나리오

### N+1 문제와 Eager Loading

DAO 패턴에서 연관 엔티티를 지연 로딩으로 반복 접근하면 N+1 쿼리 문제가 생길 수 있습니다. 테스트는 `City.users`나 `User.city`를 순회하기 전에 `.with()`로 관계를 미리 로딩합니다.

관련 테스트:

- `ExposedDaoExample` — `DAO Entity를 조건절로 검색하기 01` : one-to-many eager loading
- `ExposedDaoExample` — `DAO Entity를 조건절로 검색하기 02` : many-to-one eager loading

### 코루틴 트랜잭션 내 DAO 사용

관련 테스트: `ExposedDaoSuspendedExample`

## 실습 체크리스트

- DAO와 SQL DSL로 비교 가능한 유스케이스를 각각 구현해 코드 형태를 비교한다.
- 관계 조회 시 eager loading 유무에 따른 쿼리 수를 비교한다.
- 트랜잭션 경계 밖에서 Entity 지연 접근을 피한다.
- 관계 탐색이 깊어질수록 N+1 위험을 테스트로 고정한다.

## 다음 챕터

- [`../../04-exposed-ddl/README.md`](../../04-exposed-ddl/README.md)
