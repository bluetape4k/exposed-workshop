# 03 Exposed Basic: Exposed DAO Example

This module (
`exposed-dao-example`) provides practical examples of using the Data Access Object (DAO) pattern in Exposed, Kotlin's powerful ORM framework. It demonstrates how to interact with a relational database using
`Entity` and
`EntityClass` objects, covering fundamental CRUD (Create, Read, Update, Delete) operations and relationship management for a simple "City and User" domain. The examples showcase both synchronous and asynchronous (coroutine-based) approaches to data access.

## Key Features and Components:

### 1. Domain Schema (`Schema.kt`)

- **Table Definitions**: Defines `CityTable` and `UserTable` using
  `IntIdTable`, establishing a schema for cities and users.
- **Entity Definitions**: `City` and `User` are defined as
  `IntEntity` classes, representing the DAO entities that map to the respective tables.
- **Relationships
  **: Demonstrates one-to-many relationship (users in a city) and many-to-one relationship (city for a user) using Exposed's
  `referrersOn` and `referencedOn` functionality.
- **Test Setup Helpers**: Includes `withCityUsers` and
  `withSuspendedCityUsers` for convenient setup of test environments, including schema creation and sample data population.

### 2. Synchronous DAO Operations (`ExposedDaoExample.kt`)

- **Querying Entities**: Examples of retrieving single or multiple `City` and `User` entities using `all()` and
  `find { ... }` methods.
- **Eager Loading**: Demonstrates how to eager load related entities using the
  `.with(Entity::property)` extension function to fetch relationships efficiently.
- **Filtering Data**: Applying `WHERE` clauses with various conditions to filter entities.
- **Creating Entities**: Examples of creating new `User` entities.
- **Updating Entities**: Modifying existing entity properties.
- **Deleting Entities**: Removing entities from the database.

### 3. Asynchronous DAO Operations (`ExposedDaoSuspendedExample.kt`)

- **Coroutine Integration
  **: Showcases how to perform the same DAO operations (querying, eager loading, filtering) within a Kotlin Coroutines suspended context.
- **Reactive-style Data Access**: Utilizes `runSuspendIO` and
  `withSuspendedCityUsers` to illustrate non-blocking database interactions, making Exposed suitable for reactive applications.

## Purpose:

This module serves as an excellent resource for understanding:

- The core concepts of Exposed's DAO pattern.
- How to model domain entities and their relationships.
- Performing standard database operations (CRUD) using entities.
- Integrating Exposed DAO with Kotlin Coroutines for asynchronous programming.

## Getting Started:

To explore these examples:

1. Review the source code in `src/test/kotlin/exposed/dao/example`.
2. Run the test cases using your IDE or Gradle to observe Exposed's DAO functionality in action, both synchronously and asynchronously.

This module provides a clear and concise guide to mastering the basics of Exposed DAO.
