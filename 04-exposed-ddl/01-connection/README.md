# 04 Exposed DDL: Connection Management

This module (
`01-connection`) provides a set of examples and test cases demonstrating how to establish, configure, and manage database connections using the Exposed framework. It covers fundamental aspects of connection handling, exception management, timeouts, connection pooling, and working with multiple database instances, particularly highlighting integration with the H2 database.

## Key Features and Components:

### 1. Basic Connection Management (`exposed.examples.connection` package)

- **`Ex01_Connection.kt`
  **: Illustrates basic database connection setup, initialization, and execution of simple database operations using Exposed.
- **`Ex02_ConnectionException.kt`
  **: Demonstrates how Exposed handles various connection-related exceptions and strategies for managing them.
- **`Ex03_ConnectionTimeout.kt`
  **: Provides examples for configuring and handling connection timeouts, ensuring robust application behavior under varying network conditions or database loads.
- **`DataSourceStub.kt`**: A utility class used in tests to simulate a
  `DataSource`, allowing for isolated and controlled testing of connection logic.

### 2. Advanced Connection Scenarios (H2-specific examples in `exposed.examples.connection.h2` package)

- **`Ex01_H2_ConnectionPool.kt`
  **: Showcases how to set up and utilize connection pooling with the H2 database, a critical aspect for performance and resource management in database applications.
- **`Ex02_H2_MultiDatabase.kt`
  **: Provides examples of managing and switching between multiple database connections within a single application, using H2 as the database system.

## Purpose:

This module is designed to help users understand:

- The various ways to configure and establish database connections with Exposed.
- Best practices for handling connection-related errors and timeouts.
- How to implement connection pooling for improved performance.
- Strategies for managing multiple database instances within an Exposed application.

## Getting Started:

To explore these examples:

1. Review the source code in `src/test/kotlin/exposed/examples/connection`.
2. Run the test cases using your IDE or Gradle to observe Exposed's connection management features in action. Some examples are specifically tailored for the H2 database.

This module serves as a comprehensive guide to mastering database connection management with Exposed.
