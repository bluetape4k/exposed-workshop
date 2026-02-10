# 05 Exposed DML: Transaction Management

This module (
`04-transactions`) provides a comprehensive set of examples and test cases demonstrating Exposed's robust transaction management capabilities. It covers various aspects of database transactions, including isolation levels, execution contexts, nested transactions, transaction rollbacks, and integration with Kotlin Coroutines for asynchronous transaction handling.

## Key Features and Components:

### 1. Transaction-Specific Schema (`TransactionTables.kt`)

- Defines the necessary
  `Table` objects that are used across all transaction examples, providing a consistent data model for demonstrating transaction behaviors.

### 2. Core Transaction Concepts

- **`Ex01_TransactionIsolation.kt`
  **: Illustrates how to configure and observe different database transaction isolation levels (e.g., READ UNCOMMITTED, READ COMMITTED, REPEATABLE READ, SERIALIZABLE) with Exposed.
- **`Ex02_TransactionExec.kt`**: Demonstrates the fundamental usage of Exposed's
  `transaction { ... }` block for executing database operations within a transactional context, ensuring atomicity.
- **`Ex03_Parameterization.kt`
  **: May cover techniques for passing parameters into transaction blocks or customizing transaction properties.
- **`Ex04_QueryTimeout.kt`
  **: Provides examples on how to set and manage query timeouts within an Exposed transaction, enhancing control over long-running operations.

### 3. Advanced Transaction Scenarios

- **`Ex05_NestedTransactions.kt`
  **: Showcases the behavior and management of nested transactions (e.g., using savepoints) in a synchronous context.
- **`Ex05_NestedTransactions_Coroutines.kt`
  **: Extends the nested transaction concept to Kotlin Coroutines, demonstrating how to handle transactional boundaries in an asynchronous, non-blocking environment.
- **`Ex06_RollbackTransaction.kt`
  **: Illustrates scenarios where transactions are explicitly rolled back or implicitly rolled back due to exceptions, ensuring data integrity.
- **`Ex07_ThreadLocalManager.kt`
  **: Discusses or demonstrates the use of thread-local storage for managing transaction contexts, especially in environments where manual control over transaction boundaries is required.

## Purpose:

This module is designed to help users understand:

- How to effectively manage database transactions with Exposed to ensure data consistency and integrity.
- The impact of different transaction isolation levels.
- Techniques for handling complex transaction scenarios, including nesting and rollbacks.
- Integrating Exposed transactions with Kotlin Coroutines for building scalable reactive applications.

## Getting Started:

To explore these examples:

1. Review the source code in `src/test/kotlin/exposed/examples/transactions`.
2. Run the test cases using your IDE or Gradle to observe Exposed's transaction management features in action.

This module provides a comprehensive guide to mastering transaction handling with Exposed.

## Further Reading

- [7.4 Transactions](https://debop.notion.site/1ca2744526b080a69567d993571e21aa?v=1ca2744526b081bdab55000c5928063a)
