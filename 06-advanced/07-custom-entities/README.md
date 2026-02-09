# Custom Base Tables and Entities for ID Generation

This module demonstrates a powerful pattern in Exposed: creating reusable base `Table` and
`Entity` classes that encapsulate a specific primary key strategy. Instead of manually defining an
`id` column and its default generator for every table, you can simply inherit from a pre-configured base class.

This approach builds on the concepts from the
`06-custom-columns` module, packaging custom client-side default generators into convenient, reusable abstractions.

## Learning Objectives

- Understand how to create custom base `IdTable` and `Entity` classes.
- Learn how to abstract common ID generation strategies like Snowflake, KSUID, and time-based UUIDs.
- Simplify table definitions by inheriting from custom base classes.
- Use these custom entities seamlessly with both DSL and DAO patterns.

## Key Concepts and Provided Examples

This module provides several ready-to-use base table/entity pairs for different ID generation needs.

- **`SnowflakeIdTable` / `SnowflakeIdEntity`**
    - **ID Type**: `Long`
    - **Generator**: Uses the Snowflake algorithm to generate k-ordered, unique `Long` IDs.
    - **Use Case
      **: A good general-purpose choice for distributed systems where you need roughly time-ordered, unique numeric IDs.

- **`KsuidTable` / `KsuidEntity`**
    - **ID Type**: `String` (varchar 27)
    - **Generator**: Creates a K-Sortable Unique Identifier (KSUID).
    - **Use Case**: Excellent when you need IDs that are both unique and lexicographically sortable by generation time.

- **`KsuidMillisTable` / `KsuidMillisEntity`**
    - **ID Type**: `String` (varchar 27)
    - **Generator**: Creates a KSUID with millisecond precision.
    - **Use Case**: Similar to KSUID, but with finer-grained time resolution.

- **`TimebasedUUIDTable` / `TimebasedUUIDEntity`**
    - **ID Type**: `java.util.UUID`
    - **Generator**: Generates a time-based (Version 1) UUID.
    - **Use Case**: Useful when you need a standard UUID format that is also time-ordered.

- **`TimebasedUUIDBase62Table` / `TimebasedUUIDBase62Entity`**
    - **ID Type**: `String` (varchar 22)
    - **Generator
      **: Generates a time-based UUID and encodes it using Base62 for a shorter, more URL-friendly string representation.
    - **Use Case
      **: When you want time-ordered UUIDs but need a more compact string format than the standard 36-character UUID string.

## How It Works

These base tables are typically implemented as an `abstract class` that inherits from `IdTable`. The
`id` column is overridden and configured with the desired type and a `clientDefault` generator. The corresponding
`Entity` and `EntityClass` are also created to complete the abstraction.

## Code Snippet: Using `SnowflakeIdTable`

By inheriting from `SnowflakeIdTable`, you get the `id` column and its automatic generation for free.

```kotlin
import io.bluetape4k.exposed.dao.id.SnowflakeIdTable
import io.bluetape4k.exposed.dao.id.SnowflakeIdEntity
import io.bluetape4k.exposed.dao.id.SnowflakeIdEntityClass
import io.bluetape4k.exposed.dao.id.SnowflakeIdEntityID

// 1. Define the table by inheriting from SnowflakeIdTable
object Products: SnowflakeIdTable("products") {
    val name = varchar("name", 255)
    val price = integer("price")
}

// 2. Define the entity by inheriting from SnowflakeIdEntity
class Product(id: SnowflakeIdEntityID): SnowflakeIdEntity(id) {
    companion object: SnowflakeIdEntityClass<Product>(Products)

    var name by Products.name
    var price by Products.price
}

// 3. Use it. The ID is generated automatically.
transaction {
    // DAO-style
    val newProduct = Product.new {
        name = "Laptop"
        price = 1200
    }
    // The ID is already assigned: newProduct.id

    // DSL-style
    Products.insert {
        it[name] = "Mouse"
        it[price] = 25
    }
    // A Snowflake ID is automatically generated and inserted.
}
```

This pattern significantly reduces boilerplate code and ensures a consistent primary key strategy across all your tables that use it.

## Test Execution

The tests in this module demonstrate creating, batch-inserting, and retrieving records for each of the custom ID table types, in both standard and coroutine contexts.

```bash
# Run all tests in this module
./gradlew :06-advanced:07-custom-entities:test

# Run tests for a specific entity type, e.g., Snowflake
./gradlew :06-advanced:07-custom-entities:test --tests "exposed.examples.custom.entities.SnowflakeIdTableTest"
```

## Further Reading

- [Custom IdTable & Entities](https://debop.notion.site/Custom-Table-Entities-1c32744526b0804bad10ea3a0dce6c13)
