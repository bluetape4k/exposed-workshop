# Exposed-Jackson3: JSON/JSONB Support with Jackson 3

This module is an updated version of the `Exposed-Jackson` integration, specifically designed to work with *
*[Jackson 3.x](https://github.com/fasterxml/jackson)**. It provides `JSON` and
`JSONB` column types in Exposed, leveraging the latest features and improvements of the Jackson 3 library for object serialization and deserialization.

## Learning Objectives

- Define `json` and `jsonb` columns that map to Kotlin data classes using Jackson 3.
- Store and retrieve complex, nested objects efficiently using Jackson 3.
- Utilize Exposed's JSON query functions (`.extract<T>()`, `.contains()`, `.exists()`) with Jackson 3-backed columns.
- Apply Jackson 3-based JSON columns in both DSL and DAO programming styles.

## Key Concepts

The API and features of this module are consistent with other JSON integration modules like `exposed-json` and
`exposed-jackson` (Jackson 2), ensuring a familiar development experience while using the newer Jackson version.

### Column Types

- **`jackson<T>(name)`**: Defines a column that stores any Jackson 3-compatible object of type `T` in a standard
  `JSON` text column.
- **`jacksonb<T>(name)`**: Defines a column that stores any Jackson 3-compatible object of type `T` in an optimized
  `JSONB` (binary JSON) column. This is the recommended choice for databases that support it, like PostgreSQL.

Your data classes do **not** need to be marked with
`@Serializable`. They can be standard Kotlin data classes or POJOs, as Jackson typically uses reflection for object mapping.

### Querying Functions

The full suite of JSON querying functions provided by Exposed remains available:

- **`.extract<T>(path, toScalar)`**: Extracts a value from a JSON document at a specific path.
- **`.contains(value, path)`**: Checks if the JSON document contains a given JSON value.
- **`.exists(path, optional)`**: Checks for the existence of a key or value at a given JSONPath expression.

## Examples Overview

### `JacksonSchema.kt`

This file defines the data classes (`User`, `DataHolder`) and the Exposed `Table` objects (`JacksonTable`,
`JacksonBTable`). It also includes DAO `Entity` classes (`JacksonEntity`, `JacksonBEntity`) and test helper functions.

### `JacksonColumnTest.kt` (DSL & DAO with `json`)

This file demonstrates the usage of the
`jackson` (text-based JSON) column type with Jackson 3. It covers standard CRUD operations and JSON-specific queries using
`.extract()`, `.contains()`, and `.exists()`. It also shows integration with the DAO pattern.

### `JacksonBColumnTest.kt` (DSL & DAO with `jsonb`)

This file mirrors the examples in `JacksonColumnTest.kt` but focuses on the
`jacksonb` column type with Jackson 3. The code is structured similarly, emphasizing the consistent API across
`json` and `jsonb` types.

## Code Snippets

### 1. Defining a Table with a `jacksonb` Column (Jackson 3)

```kotlin
import io.bluetape4k.exposed.core.jackson3.jacksonb
import com.fasterxml.jackson.annotation.JsonCreator // Example for Jackson 3 annotations

// Standard data class
data class User(val name: String, val team: String?)
data class UserData(val info: User, val logins: Int, val active: Boolean)

object UsersTable: IntIdTable("users") {
    // The column stores the UserData object as JSONB using Jackson 3
    val data = jacksonb<UserData>("data")
}
```

### 2. Inserting and Querying with Jackson 3 (DSL)

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

### 3. Using a Jackson 3 Column in an Entity (DAO)

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
./gradlew :06-advanced:11-exposed-jackson3:test

# Run tests for the JSONB column type specifically
./gradlew :06-advanced:11-exposed-jackson3:test --tests "exposed.examples.jackson3.JacksonBColumnTest"
```

## Further Reading

- [Exposed Jackson](https://debop.notion.site/Exposed-Jackson-1c32744526b0809599a7db2e629a597a)
