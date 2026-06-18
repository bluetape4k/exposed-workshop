# 03 Exposed Basic: DAO Example

English | [한국어](./README.ko.md)

A module for learning the fundamentals of the Exposed DAO (Entity) pattern. It covers `IntIdTable`/`IntEntity` modelling, nullable relationship mapping with `optionalReferencedOn` and `optionalReferrersOn`, eager loading, update/delete, and coroutine transactions.

## Overview

The Exposed DAO pattern provides ORM-style data access via an `IntIdTable` and `IntEntity`/`IntEntityClass` pair. `Schema.kt` maps `City` and `User` to `cities` and `users`, keeps `User.city` nullable, and exposes the inverse `City.users` collection. The tests then verify entity lookup, eager loading for one-to-many and many-to-one relationships, property-based updates, deletes, and the same behavior inside `newSuspendedTransaction { }`.

## Learning Goals

- Learn entity modelling based on `IntIdTable`, `IntEntity`, and `IntEntityClass`.
- Understand nullable many-to-one and one-to-many mapping through `optionalReferencedOn` and `optionalReferrersOn`.
- Prevent relationship-query N+1 patterns with `.with()` eager loading.
- Use DAO inside coroutine transactions based on `newSuspendedTransaction`.

## Prerequisites

- [`../exposed-sql-example/README.md`](../exposed-sql-example/README.md)

## ERD

![exposed dao example Entity Relationship diagram](../../docs/images/readme-diagrams/03-exposed-basic-exposed-dao-example-erd-01.png)

## Domain Model

![Domain Model diagram](../../docs/images/readme-diagrams/03-exposed-basic-exposed-dao-example-class-02.png)

## Core Concepts

### Entity / Table Declaration

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

    // one-to-many: list of Users belonging to this City (Lazy by default)
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

    // UPDATE — property changes within a transaction are reflected automatically
    found?.name = "debop (updated)"

    // DELETE
    found?.delete()
}
```

### Preventing N+1 with Eager Loading

```kotlin
// Problem (Lazy Loading — N+1 occurs)
City.all().forEach { city ->
    city.users.forEach { user -> println(user.name) }  // N additional queries
}

// Solution (Eager Loading — use .with())
// City 1 query + User 1 query = 2 queries total
City.find { CityTable.name eq "Seoul" }
    .with(City::users)          // pre-load users
    .forEach { city ->
        city.users.forEach { println(it.name) }
    }
```

### Using DAO within a Coroutine Transaction

```kotlin
// Entity access inside newSuspendedTransaction
suspend fun withSuspendedCityUsers(testDB: TestDB, statement: suspend JdbcTransaction.() -> Unit) {
    withTablesSuspending(testDB, CityTable, UserTable) {
        populateSamples()
        flushCache()
        entityCache.clear()
        statement()
    }
}

// Usage example
withSuspendedCityUsers(testDB) {
    val users = User.find { UserTable.age greaterEq intLiteral(18) }
        .with(User::city)
        .toList()
}
```

## Example Files

| File                              | Description                                           |
|---------------------------------|-------------------------------------------------------|
| `Schema.kt`                     | DAO table/entity definitions, sample rows, cache flush, and sync/suspending helpers |
| `ExposedDaoExample.kt`          | Synchronous DAO lookup, eager loading, update, and delete tests |
| `ExposedDaoSuspendedExample.kt` | Coroutine DAO tests that repeat the same scenarios through `withSuspendedCityUsers` |

## Running Tests

```bash
# Full tests
./gradlew :exposed-dao-example:test

# Fast tests targeting H2 only
./gradlew :exposed-dao-example:test -PuseFastDB=true

# Run a specific test class
./gradlew :exposed-dao-example:test \
    --tests "exposed.dao.example.ExposedDaoExample"
```

## Advanced Scenarios

### N+1 Problem and Eager Loading

Repeatedly accessing related entities lazily in the DAO pattern can create N+1 query issues. The tests use `.with()` to preload the relation before iterating over `City.users` or `User.city`.

Related tests:

- `ExposedDaoExample` — `DAO Entity를 조건절로 검색하기 01` : one-to-many eager loading
- `ExposedDaoExample` — `DAO Entity를 조건절로 검색하기 02` : many-to-one eager loading

### Using DAO within a Coroutine Transaction

Related test: `ExposedDaoSuspendedExample`

## Practice Checklist

- Implement comparable use cases in both DAO and SQL DSL and compare the resulting code shape.
- Compare the number of queries with and without eager loading during relationship queries.
- Avoid lazy entity access outside the transaction boundary.
- As relationship traversal deepens, pin N+1 risks with tests.

## Next Chapter

- [`../../04-exposed-ddl/README.md`](../../04-exposed-ddl/README.md)
