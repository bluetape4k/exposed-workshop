# Exposed-Fastjson2: JSON/JSONB Support with Alibaba Fastjson2

This module demonstrates how to integrate Alibaba's **Fastjson2** library with Exposed to handle `JSON` and
`JSONB` column types. Fastjson2 is known for its exceptional performance in JSON serialization and deserialization, making this module suitable for applications where JSON processing speed is critical.

## Learning Objectives

- Define `json` and `jsonb` columns that map to Kotlin data classes using Fastjson2.
- Store and retrieve complex, nested objects efficiently with Fastjson2.
- Utilize Exposed's JSON query functions (`.extract<T>()`, `.contains()`, `.exists()`) with Fastjson2-backed columns.
- Apply Fastjson2-based JSON columns in both DSL and DAO programming styles.

## Key Concepts

The API and features of this module are very similar to `exposed-json` (using `kotlinx.serialization`) and
`exposed-jackson` (using Jackson), but with Fastjson2 as the underlying serialization engine.

### Column Types

- **`fastjson<T>(name)`**: Defines a column that stores any Fastjson2-compatible object of type `T` in a standard
  `JSON` text column.
- **`fastjsonb<T>(name)`**: Defines a column that stores any Fastjson2-compatible object of type `T` in an optimized
  `JSONB` (binary JSON) column. This is generally recommended for databases that support it (e.g., PostgreSQL).

Similar to the Jackson integration, your data classes do **not
** necessarily need special annotations. Fastjson2 can typically work with standard Kotlin data classes or POJOs using reflection.

### Querying Functions

The same set of powerful JSON querying functions provided by Exposed are available:

- **`.extract<T>(path, toScalar)`**: Extracts a value from a JSON document at a specific path.
- **`.contains(value, path)`**: Checks if the JSON document contains a given JSON value.
- **`.exists(path, optional)`**: Checks for the existence of a key or value at a given JSONPath expression.

## Examples Overview

### `FastjsonSchema.kt`

This file defines the data classes (`User`, `DataHolder`) and the Exposed `Table` objects (`FastjsonTable`,
`FastjsonBTable`). It also includes DAO `Entity` classes (`FastjsonEntity`,
`FastjsonBEntity`) and test helper functions to facilitate the examples.

### `FastjsonColumnTest.kt` (DSL & DAO with `json`)

This file demonstrates the usage of the
`fastjson` (text-based JSON) column type. It covers standard CRUD operations and JSON-specific queries using
`.extract()`, `.contains()`, and `.exists()`. It also shows integration with the DAO pattern.

### `FastjsonBColumnTest.kt` (DSL & DAO with `jsonb`)

This file mirrors the examples in `FastjsonColumnTest.kt` but focuses on the
`fastjsonb` column type. The code is structured similarly, emphasizing the consistent API across `json` and
`jsonb` types.

## Code Snippets

### 1. Defining a Table with a `fastjsonb` Column

```kotlin
import io.bluetape4k.exposed.core.fastjson2.fastjsonb
import com.alibaba.fastjson.annotation.JSONField // Optional, but can be used for customization

// Standard data class
data class User(val name: String, val team: String?)
data class UserData(val info: User, val logins: Int, val active: Boolean)

object UsersTable : IntIdTable("users") {
    // The column stores the UserData object as JSONB using Fastjson2
    val data = fastjsonb<UserData>("data")
}
```

### 2. Inserting and Querying with Fastjson2 (DSL)

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

### 3. Using a Fastjson2 Column in an Entity (DAO)

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
./gradlew :06-advanced:09-exposed-fastjson2:test

# Run tests for the FastjsonB column type specifically
./gradlew :06-advanced:09-exposed-fastjson2:test --tests "exposed.examples.fastjson2.FastjsonBColumnTest"
```
