# 04 Exposed DDL: Schema Definition Language (DDL)

This module (
`02-ddl`) provides a comprehensive set of examples and test cases demonstrating Exposed's Data Definition Language (DDL) capabilities. It covers how to define, create, and manage database schemas, tables, columns, indexes, sequences, and custom data types using Exposed's fluent DSL. These examples are designed to illustrate the programmatic control Exposed offers over database structure.

## Key Features and Components:

### 1. Database and Table Creation

- **`Ex01_CreateDatabase.kt`
  **: While Exposed primarily manages schema within existing databases, this example might illustrate database-level operations or initial setup scripts.
- **`Ex02_CreateTable.kt`
  **: Demonstrates the fundamental process of defining and creating tables using Exposed's DSL, including primary keys and basic column types.
- **`Ex03_CreateMissingTableAndColumns.kt`
  **: Highlights Exposed's schema synchronization features, showing how to automatically create tables and columns that are missing from the database but defined in the code.

### 2. Advanced Column and Schema Elements

- **`Ex04_ColumnDefinition.kt`
  **: Provides examples for defining various column types, specifying constraints (e.g., nullable, unique), and setting default values.
- **`Ex05_CreateIndex.kt`
  **: Illustrates how to create indexes on single or multiple columns to optimize query performance.
- **`Ex06_Sequence.kt`
  **: Demonstrates the use and management of database sequences, which are often used for generating unique identifiers.
- **`Ex07_CustomEnumeration.kt`
  **: Shows how to define and use custom Kotlin enumeration types as database columns, providing type-safe access to predefined values.

### 3. Comprehensive DDL Examples

- **`Ex10_DDL_Examples.kt`
  **: A broader example combining several DDL operations or showcasing more advanced scenarios, providing a holistic view of schema management with Exposed.

## Purpose:

This module is designed to help users understand:

- How to programmatically define and create database schemas with Exposed.
- The flexibility of Exposed's DSL for specifying various column types, constraints, and indexes.
- Techniques for schema evolution, such as creating missing elements.
- Management of advanced DDL features like sequences and custom enumerations.

## Getting Started:

To explore these examples:

1. Review the source code in `src/test/kotlin/exposed/examples/ddl`.
2. Run the test cases using your IDE or Gradle to observe Exposed's DDL capabilities in action.

This module provides a robust foundation for defining and managing your database schema with Exposed.
