# 03 Exposed Basic: Exposed SQL DSL Example

This module (
`exposed-sql-example`) provides practical examples of using Exposed's SQL Domain Specific Language (DSL) for direct interaction with a relational database. It demonstrates how to perform fundamental CRUD (Create, Read, Update, Delete) operations, join tables, and utilize aggregate functions using Exposed's fluent API, without relying on the DAO (Data Access Object) pattern. The examples showcase both synchronous and asynchronous (coroutine-based) approaches to data access for a simple "City and User" domain.

## Key Features and Components:

### 1. Domain Schema (`Schema.kt`)

- **Table Definitions**: Defines `CityTable` and `UserTable` as
  `Table` objects, establishing a schema for cities and users. These definitions include explicit primary keys and a foreign key relationship.
- **Sample Data Insertion**:
  `insertSampleData` function demonstrates how to insert data directly using the SQL DSL, including examples of using SQL functions (
  `trim`, `substring`) within inserts.
- **Test Setup Helpers**: Includes `withCityUsers` and
  `withSuspendedCityUsers` for convenient setup of test environments, including schema creation and sample data population.

### 2. Synchronous SQL DSL Operations (`ExposedSQLExample.kt`)

- **Updating Data**: Examples of performing `UPDATE` statements with `WHERE` clauses to modify records.
- **Deleting Data**: Demonstrations of `DELETE` statements using `deleteWhere` with specific criteria.
- **Joining Tables**: Examples of `INNER JOIN` operations, showing how to connect `UserTable` and
  `CityTable` based on foreign key relationships, both implicitly and explicitly.
- **Querying and Filtering**: Performing `SELECT` queries with various `WHERE` conditions (`eq`, `like`, `isNull`,
  `or`) to filter and retrieve data.
- **Aggregate Functions and Grouping**: Usage of aggregate functions like `count()` in conjunction with
  `groupBy()` for analytical queries (e.g., counting users per city).

### 3. Asynchronous SQL DSL Operations (`ExposedSQLSuspendedExample.kt`)

- **Coroutine Integration
  **: Showcases how to perform the same SQL DSL operations (update, delete, join, query, aggregate) within a Kotlin Coroutines suspended context.
- **Reactive-style Data Access**: Utilizes `runSuspendIO` and
  `withSuspendedCityUsers` to illustrate non-blocking database interactions, making Exposed's SQL DSL suitable for reactive applications.

## Purpose:

This module serves as an excellent resource for understanding:

- The direct SQL DSL approach in Exposed for interacting with databases.
- How to perform common database operations (CRUD, joins, aggregation) using the fluent API.
- Integrating Exposed SQL DSL with Kotlin Coroutines for asynchronous programming.
- The differences between Exposed's DAO and SQL DSL patterns.

## Getting Started:

To explore these examples:

1. Review the source code in `src/test/kotlin/exposed/sql/example`.
2. Run the test cases using your IDE or Gradle to observe Exposed's SQL DSL functionality in action, both synchronously and asynchronously.

This module provides a clear and concise guide to mastering the direct SQL DSL capabilities of Exposed.
