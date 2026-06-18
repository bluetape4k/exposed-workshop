# 03 Exposed Basic: SQL DSL Example

English | [한국어](./README.ko.md)

A module for learning Exposed SQL DSL through tests. It covers table definition, seeded sample data, update/delete operations, joins, aggregation, and the same DSL scenarios inside coroutine transactions.

## Overview

Exposed DSL expresses SQL queries as Kotlin type-safe function chains. `Schema.kt` defines plain `Table` objects for `cities` and `users`, seeds three cities and five users, then each test composes `update`, `deleteWhere`, `innerJoin`, `leftJoin`, `groupBy`, and `select` calls inside a shared transaction helper. `ExposedSQLSuspendedExample` repeats the same cases through the suspending helper backed by `newSuspendedTransaction { }`.

## Learning Goals

- Write type-safe update, delete, join, and aggregation queries with Exposed SQL DSL.
- Understand how nullable foreign keys affect join results.
- Compare the synchronous helper with the suspending helper that runs the same DSL bodies in a coroutine transaction.

## Prerequisites

- [`../README.md`](../README.md)

## ERD

![exposed sql example Entity Relationship diagram](../../docs/images/readme-diagrams/03-exposed-basic-exposed-sql-example-erd-01.png)

## DSL Query Flow

![DSL Query Flow diagram](../../docs/images/readme-diagrams/03-exposed-basic-exposed-sql-example-sequence-02.png)

## Domain Model

![Domain Model diagram](../../docs/images/readme-diagrams/03-exposed-basic-exposed-sql-example-class-03.png)

### Table Definition

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

## Core Concepts

### INSERT

```kotlin
// Basic INSERT
val seoulId = CityTable.insert {
    it[name] = "Seoul"
} get CityTable.id

// Expression-based INSERT — SUBSTRING(TRIM('   Daegu   '), 1, 2)
CityTable.insert {
    it.update(name, stringLiteral("   Daegu   ").trim().substring(1, 2))
}
```

### SELECT + WHERE

```kotlin
// Simple conditional query
CityTable.selectAll()
    .where { CityTable.id eq seoulId }
    .single()[CityTable.name]

// andWhere / orWhere chaining
UserTable.innerJoin(CityTable)
    .select(UserTable.name, CityTable.name)
    .where { (UserTable.id eq "debop") or (UserTable.name eq "Jane.Doe") }
    .andWhere { UserTable.id eq "jane" }
```

### JOIN + GROUP BY + Aggregation

```kotlin
// Aggregate user count per city
val userCountsByCity = CityTable.innerJoin(UserTable)
    .select(CityTable.name, UserTable.id.count())
    .groupBy(CityTable.name)
    .associate { it[CityTable.name] to it[UserTable.id.count()] }
```

### UPDATE / DELETE

```kotlin
// UPDATE
UserTable.update({ UserTable.id eq "debop" }) {
    it[name] = "Debop.Bae (Updated)"
}

// DELETE
UserTable.deleteWhere { UserTable.cityId.isNull() }
```

### Coroutine-Based Queries

```kotlin
// Use the same DSL inside newSuspendedTransaction
suspend fun withSuspendedCityUsers(testDB: TestDB, statement: suspend JdbcTransaction.() -> Unit) {
    withTablesSuspending(testDB, CityTable, UserTable) {
        insertSampleData()
        commit()
        statement()
    }
}
```

## Example Files

| File                              | Description                                         |
|---------------------------------|-----------------------------------------------------|
| `Schema.kt`                     | `CityTable`, `UserTable`, sample rows, and sync/suspending transaction helpers |
| `ExposedSQLExample.kt`          | Synchronous DSL update, delete, join, and aggregation tests |
| `ExposedSQLSuspendedExample.kt` | Coroutine DSL tests that repeat the same scenarios through `withSuspendedCityUsers` |

## Running Tests

```bash
# Full tests
./gradlew :exposed-sql-example:test

# Fast tests targeting H2 only
./gradlew :exposed-sql-example:test -PuseFastDB=true

# Run a specific test class
./gradlew :exposed-sql-example:test \
    --tests "exposed.sql.example.ExposedSQLExample"
```

## Advanced Scenarios

### Join + Aggregation Query

```kotlin
// City 1 query + User 1 JOIN query, then aggregate
val userCountsByCity = CityTable.innerJoin(UserTable)
    .select(CityTable.name, UserTable.id.count())
    .groupBy(CityTable.name)
    .associate { it[CityTable.name] to it[UserTable.id.count()] }
```

Related tests:

- `ExposedSQLExample` — `use functions and group by`
- `ExposedSQLSuspendedExample` — `use functions and group by`

### andWhere / orWhere Chaining

```kotlin
UserTable
    .innerJoin(CityTable)
    .select(UserTable.name, CityTable.name)
    .where { (UserTable.id eq "debop") or (UserTable.name eq "Jane.Doe") }
    .andWhere { UserTable.id eq "jane" }
```

Related test: `ExposedSQLExample` — `manual inner join`

## Practice Checklist

- Run the same DSL scenarios on both synchronous and coroutine paths and compare results.
- Extend the join + aggregation query on your own.
- For complex DSL chains, separate intermediate expressions to maintain readability.

## Next Module

- [`../exposed-dao-example/README.md`](../exposed-dao-example/README.md)
