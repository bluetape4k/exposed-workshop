# Exposed-Jackson: JSON/JSONB Support with the Jackson Library

This module demonstrates how to use `JSON` and `JSONB` column types in Exposed by leveraging the popular **Jackson
** library for serialization and deserialization. It provides an alternative to the `exposed-json` module (which uses
`kotlinx.serialization`) and is ideal for projects that are already invested in the Jackson ecosystem.

## Learning Objectives

- Define `json` and `jsonb` columns that map to Kotlin data classes using Jackson.
- Store and retrieve complex, nested objects without needing the `@Serializable` annotation.
- Use Jackson-backed columns with the full range of Exposed's JSON query functions, including `.extract<T>()`,
  `.contains()`, and `.exists()`.
- Apply Jackson-based JSON columns in both DSL and DAO programming styles.

## Key Concepts

This module's API is nearly identical to that of `exposed-json`, but the underlying implementation uses Jackson's
`ObjectMapper`.

### Column Types

- **`jackson<T>(name)`**: Defines a column that stores any Jackson-compatible object of type `T` in a standard
  `JSON` text column.
- **`jacksonb<T>(name)`**: Defines a column that stores any Jackson-compatible object of type `T` in an optimized
  `JSONB` (binary JSON) column. This is the recommended choice for databases that support it, like PostgreSQL.

Unlike `exposed-json`, your data classes do **not** need to be marked with
`@Serializable`. They can be standard Kotlin data classes or POJOs.

### Querying Functions

The same powerful querying functions are available:

- **`.extract<T>(path, toScalar)`**: Extracts a value from a JSON document at a specific path.
- **`.contains(value, path)`**: Checks if the JSON document contains a given JSON-formatted string as a value.
- **`.exists(path, optional)`**: Checks for the existence of a value at a given JSONPath expression.

## Examples Overview

### `JacksonSchema.kt`

This file defines the data classes (`User`, `DataHolder`) and the Exposed `Table` objects (`JacksonTable`,
`JacksonBTable`). It also includes DAO `Entity` classes (`JacksonEntity`, `JacksonBEntity`) and test helper functions.

### `JacksonColumnTest.kt` (DSL & DAO with `json`)

This file demonstrates the usage of the `jackson` (text-based JSON) column type. It covers:

- `INSERT`, `UPDATE`, `UPSERT`, and `SELECT` operations.
- Querying using `.extract()`, `.contains()`, and `.exists()`.
- Using the column within a DAO entity.
- Handling collections and nullable JSON columns.

### `JacksonBColumnTest.kt` (DSL & DAO with `jsonb`)

This file mirrors the examples in `JacksonColumnTest.kt` but uses the more performant
`jacksonb` column type. The code is almost identical, showing the consistency of the API.

## Code Snippets

### 1. Defining a Table with a `jacksonb` Column

```kotlin
import io.bluetape4k.exposed.core.jackson.jacksonb

// Standard data class - no @Serializable needed
data class User(val name: String, val team: String?)
data class UserData(val info: User, val logins: Int, val active: Boolean)

object UsersTable: IntIdTable("users") {
    // The column stores the UserData object as JSONB using Jackson
    val data = jacksonb<UserData>("data")
}
```

### 2. Inserting and Querying with Jackson (DSL)

```kotlin
val userData = UserData(info = User("test", "A"), logins = 5, active = true)

// Insert data
UsersTable.insert {
    it[data] = userData
}

// Extract a nested value and use it in a WHERE clause
// Note: Path syntax may differ across databases
val username = UsersTable.data.extract<String>(".info.name")
val userRecord = UsersTable.selectAll().where { username eq "test" }.single()

// The full object is automatically deserialized on read
val retrievedData = userRecord[UsersTable.data]
retrievedData.logins shouldBeEqualTo 5
```

### 3. Using a Jackson Column in an Entity (DAO)

```kotlin
class UserEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<UserEntity>(UsersTable)

    // The property is automatically mapped to/from JSON
    var data by UsersTable.data
}

// Create a new entity
val entity = UserEntity.new {
    data = UserData(info = User("dao_user", "B"), logins = 1, active = true)
}

// Access the property
println(entity.data.info.name) // Prints "dao_user"
```

## Test Execution

**Note
**: JSON/JSONB features are highly database-dependent. Many tests are skipped on databases with limited support (like H2). For best results, run against PostgreSQL.

```bash
# Run all tests in this module
./gradlew :06-advanced:08-exposed-jackson:test

# Run tests for the JSONB column type specifically
./gradlew :06-advanced:08-exposed-jackson:test --tests "exposed.examples.jackson.JacksonBColumnTest"
```

## Further Reading

- [Exposed Jackson](https://debop.notion.site/Exposed-Jackson-1c32744526b0809599a7db2e629a597a)
