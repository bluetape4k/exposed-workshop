# 02 Alternatives to JPA: Vert.x SQL Client Example

This module (
`vertx-sqlclient-example`) provides a collection of test-based examples demonstrating how to perform reactive database operations using the Vert.x SQL Client. It serves as an alternative to traditional JPA, Exposed, or Spring Data R2DBC for building non-blocking and event-driven data access layers. The examples primarily focus on interacting with a PostgreSQL database and feature a simple
`Customer` domain model.

## Key Features and Components:

### 1. Core Test Utilities (`alternative.vertx.sqlclient.example` package)

- **`AbstractSqlClientTest.kt`
  **: A base class for tests, providing common setup and utilities for Vert.x SQL Client test cases.
- **`JDBCPoolExamples.kt`
  **: Demonstrates the usage of Vert.x's JDBC client pool, showcasing how to integrate traditional JDBC drivers into a reactive Vert.x environment.

### 2. Domain Model (`alternative.vertx.sqlclient.example.model` package)

- **`Customer.kt`**: Defines a simple `Customer` data class, serving as the domain entity for the examples.

### 3. PostgreSQL Specific Examples (`alternative.vertx.sqlclient.example.templates` package)

- **`SqlClientTemplatePostgresExamples.kt`**: Contains examples specific to using
  `SqlClientTemplate` with a PostgreSQL database, illustrating common CRUD (Create, Read, Update, Delete) operations and other database interactions in a reactive manner.

## Purpose:

This module highlights the capabilities of Vert.x SQL Client for:

- Performing asynchronous and non-blocking database operations.
- Integrating with various database systems (with specific examples for PostgreSQL).
- Providing a reactive alternative for data persistence layers in Vert.x-based applications.

## Getting Started:

To explore these examples:

1. Ensure you have a PostgreSQL database instance running and configured for testing.
2. Review the test cases in the `src/test/kotlin` directory to understand the implementation details.
3. Run the tests using your IDE or Gradle to see the Vert.x SQL Client in action.

This example provides valuable insights into building reactive data access components using the Vert.x ecosystem.
