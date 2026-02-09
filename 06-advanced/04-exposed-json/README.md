# Exposed-Json: Storing and Querying JSON/JSONB Data

This module demonstrates how to use the `exposed-json` extension to map
`@Serializable` Kotlin classes to native database `JSON` and
`JSONB` columns. This allows for storing complex, schemaless data directly within your relational database and leveraging powerful, database-specific JSON query functions.

This functionality relies on the `kotlinx.serialization` library.

## Learning Objectives

- Define table columns that map to `JSON` and `JSONB` data types.
- Store and retrieve complex, nested Kotlin objects and collections in a single column.
- Use the `.extract<T>()` function to query for specific fields within a JSON object.
- Filter rows using `.contains()` and `.exists()` operators to check for the presence of data within a JSON structure.
- Understand the difference between `json` and `jsonb` column types.
- Apply JSON columns in both DSL and DAO styles.

## Key Concepts

### `json` vs. `jsonb`

- **`json<T>(name, jsonMapper)`**: Stores data as a plain text
  `JSON` string. It preserves the exact input format, including whitespace and duplicate keys. It's generally faster to write but slower to query.
- **`jsonb<T>(name, jsonMapper)`
  **: Stores data in a decomposed binary format. Writes are slightly slower due to the conversion overhead, but it is much more efficient to query and can be indexed (especially in PostgreSQL). It does not preserve whitespace or duplicate keys.

**Recommendation:** Use
`jsonb` whenever your database supports it (like PostgreSQL) and you need to perform queries on the JSON data.

### Querying Functions

- **`.extract<T>(path, toScalar)`
  **: Extracts a value from a JSON document at a specific path. The path syntax varies by database (e.g.,
  `".user.name"` for MySQL, `"user", "name"` for PostgreSQL).
- **`.contains(value, path)`
  **: Checks if the JSON document contains a given JSON-formatted string as a value, optionally at a specific path. In PostgreSQL, this uses the efficient
  `@>` operator.
- **`.exists(path, optional)`
  **: Checks if a value exists at a given JSONPath expression. This is a powerful way to query for the structure of your JSON documents.

## Examples Overview

### `JsonTestData.kt`

This file defines the `@Serializable` data classes (`User`, `DataHolder`, `UserGroup`) and the Exposed `Table` objects (
`JsonTable`, `JsonBTable`) that are used throughout the examples.

### `Ex01_JsonColumn.kt` (DSL & DAO with `json`)

This file demonstrates the usage of the `json` column type.

- **DSL**: Shows how to `insert`, `update`, and `select` using the DSL.
- **DAO**: A `JsonEntity` class is used to show how `json` columns can be used as properties in the DAO pattern.
- **Querying**: Demonstrates extracting values with `.extract<T>()` and filtering records using `.contains()` and
  `.exists()`.

### `Ex02_JsonBColumn.kt` (DSL & DAO with `jsonb`)

This file mirrors the examples in `Ex01_JsonColumn.kt` but uses the
`jsonb` column type. The code is nearly identical, highlighting that the primary difference is in the table definition and the underlying database performance and capabilities, rather than the Exposed API.

## Code Snippets

### 1. Defining a Table with `jsonb` Column

```kotlin
import org.jetbrains.exposed.v1.json.jsonb
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class User(val name: String, val team: String?)

@Serializable
data class DataHolder(val user: User, val logins: Int, val active: Boolean)

object UserTable: IntIdTable("users") {
    // The column stores the entire DataHolder object as JSONB
    val data = jsonb<DataHolder>("data", Json.Default)
}
```

### 2. Inserting and Updating JSON Data

```kotlin
// Insert a new record
val id = UserTable.insertAndGetId {
    it[data] = DataHolder(User("John Doe", "A-Team"), 15, true)
}

// Update the JSON data in the record
UserTable.update({ UserTable.id eq id }) {
    it[data] = DataHolder(User("John Doe", "A-Team"), 16, false)
}
```

### 3. Querying with `.extract()`

```kotlin
// Extract the 'active' boolean field from the JSONB column
// Note: path syntax may vary by database
val isActive = UserTable.data.extract<Boolean>(".active", toScalar = true)

// Find all inactive users
val inactiveUsers = UserTable.selectAll().where { isActive eq false }.toList()
```

### 4. Filtering with `.contains()` (PostgreSQL & MySQL)

```kotlin
// Find all users that have "active":false in their data
val userIsInactive = UserTable.data.contains("""{"active":false}""")
val result = UserTable.selectAll().where { userIsInactive }.toList()
```

## Test Execution

**Note
**: JSON/JSONB features are highly database-dependent. Many tests are skipped on databases with limited support (like H2). For best results, run against PostgreSQL.

```bash
# Run all tests in this module
./gradlew :06-advanced:04-exposed-json:test

# Run tests for the JSONB column type
./gradlew :06-advanced:04-exposed-json:test --tests "exposed.examples.json.Ex02_JsonBColumn"
```

## Further Reading

- [Exposed Json](https://debop.notion.site/Exposed-Json-1c32744526b080a9bee3d7b92463e90c)
