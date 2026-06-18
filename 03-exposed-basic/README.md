# 03 Exposed Basic

English | [한국어](./README.ko.md)

An introductory chapter for first-time Exposed learners. SQL DSL and DAO are compared over the same `City`/`User` domain, with every concept anchored in executable tests.

## Overview

Exposed provides two data access patterns. The **DSL (SQL DSL)** pattern expresses SQL as Kotlin type-safe query chains over `Table` objects, while the **DAO** pattern maps `IntIdTable` rows to `IntEntity` objects. This chapter uses the same `City`/`User` shape in both modules so the schema, transaction boundary, relationship handling, and result type differences are easy to compare.

## Learning Goals

- Understand when to use SQL DSL query chains and when to use DAO entity objects.
- Verify update, delete, join, group-by, relationship loading, and coroutine transaction behavior through tests.
- Recognize the schema/helper/test structure that later chapters extend for DDL and DML practice.

## Included Modules

| Module                  | Description                                                           |
|-----------------------|-----------------------------------------------------------------------|
| `exposed-sql-example` | Test-based example for SQL DSL updates, deletes, joins, aggregation, and coroutine transactions |
| `exposed-dao-example` | Test-based example for DAO entity lookup, optional relationships, eager loading, update/delete, and coroutine transactions |

## DSL vs DAO Pattern Comparison

| Item              | DSL (SQL DSL)                                        | DAO (Entity/EntityClass)                           |
|-----------------|------------------------------------------------------|----------------------------------------------------|
| Schema definition | `object CityTable : Table("cities")`               | `object CityTable : IntIdTable("cities")`          |
| Record insert   | `CityTable.insert { it[name] = "Seoul" }`            | `City.new { name = "Seoul" }`                      |
| Record query    | `CityTable.selectAll().where { id eq 1 }`            | `City.findById(1)` / `City.all()`                  |
| Record update   | `CityTable.update({ id eq 1 }) { it[name] = "..." }` | `city.name = "..."` (auto-applied within transaction) |
| Record delete   | `CityTable.deleteWhere { id eq 1 }`                  | `city.delete()`                                    |
| Relation query  | `CityTable.innerJoin(UserTable).selectAll()`         | `city.users` (Lazy) / `.with(City::users)` (Eager) |
| Result type     | `ResultRow` (Map-like)                               | `Entity` instance (object model)                   |
| Aggregation/join| Freely expressible with DSL chaining                 | Complex aggregations: recommended to mix with DSL  |
| Coroutine support | `newSuspendedTransaction { }`                      | Entity access within `newSuspendedTransaction { }` |
| N+1 risk        | None (explicit JOIN)                                 | Caution needed with Lazy Loading                   |

## User Cities Domain Model

![User Cities Domain Model diagram](../docs/images/readme-diagrams/03-exposed-basic-class-01.png)

## DSL-Style Schema Definition

```kotlin
// DSL — uses plain Table, PrimaryKey declared explicitly
object CityTable : Table("cities") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", length = 50)
    override val primaryKey = PrimaryKey(id, name = "PK_Cities_ID")
}

object UserTable : Table("users") {
    val id = varchar("id", length = 10)
    val name = varchar("name", length = 50)
    val cityId = optReference("city_id", CityTable.id)
    override val primaryKey = PrimaryKey(id, name = "PK_User_ID")
}
```

## DAO-Style Schema Definition

```kotlin
// DAO — inherits IntIdTable, paired with an Entity class
object CityTable : IntIdTable("cities") {
    val name = varchar("name", 50)
}

object UserTable : IntIdTable("users") {
    val name = varchar("name", 50)
    val age = integer("age")
    val cityId = optReference("city_id", CityTable)
}

class City(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<City>(CityTable)
    var name: String by CityTable.name
    val users: SizedIterable<User> by User optionalReferrersOn UserTable.cityId
}

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(UserTable)
    var name: String by UserTable.name
    var age: Int by UserTable.age
    var city: City? by City optionalReferencedOn UserTable.cityId
}
```

## Recommended Study Order

1. `exposed-sql-example` — Basic SELECT/INSERT/UPDATE/DELETE with DSL
2. `exposed-dao-example` — Entity CRUD, relationship mapping, Eager Loading

## Prerequisites

- Kotlin basic syntax and functional idioms
- Relational database fundamentals (tables, PK/FK)

## Running Tests

```bash
# DSL example tests
./gradlew :exposed-sql-example:test

# DAO example tests
./gradlew :exposed-dao-example:test

# Fast tests targeting H2 only
./gradlew :exposed-sql-example:test -PuseFastDB=true
./gradlew :exposed-dao-example:test -PuseFastDB=true
```

## Test Points

- Verify how the same `City`/`User` model is expressed as SQL DSL tables and DAO entities.
- Confirm that query conditions, joins, aggregation, updates, and deletes match expected values.
- Identify DAO relationship reads that can create N+1 query patterns.
- Keep entity traversal inside the transaction boundary and prefer eager loading for relationship-focused tests.

## Next Chapter

- [04-exposed-ddl](../04-exposed-ddl/README.md): Extends into DB connection and schema definition practice.
