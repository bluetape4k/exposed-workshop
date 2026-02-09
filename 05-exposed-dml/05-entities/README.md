# 05 Exposed DML: Entity API

This module (
`05-entities`) provides a comprehensive set of examples and test cases demonstrating Exposed's powerful Entity API for Data Manipulation Language (DML) operations. It covers various aspects of defining, creating, retrieving, updating, and deleting entities, as well as managing complex relationships, different primary key types, entity lifecycle hooks, and caching mechanisms.

## Key Features and Components:

### 1. Entity Test Data and Basic Usage

- **`EntityTestData.kt`
  **: Defines common database tables and sample data used throughout the entity examples, providing a consistent environment for demonstration.
- **`Ex01_Entity.kt`
  **: Illustrates the fundamental usage of the Exposed Entity API for basic CRUD (Create, Read, Update, Delete) operations on entities.

### 2. Entity Lifecycle and Caching

- **`Ex02_EntityHook.kt` / `Ex02_EntityHook_Auditable.kt`
  **: Demonstrates the use of entity hooks to intercept lifecycle events (e.g., `beforeInsert`,
  `afterUpdate`), which can be used for tasks like auditing or data validation.
- **`Ex03_EntityCache.kt`
  **: Explores Exposed's entity caching mechanism, showing how to leverage and manage it for performance optimization.

### 3. Diverse Primary Key Strategies

- **`Ex04_LongIdTableEntity.kt`**: Examples of entities whose primary keys are of type `Long` (using `LongIdTable`).
- **`Ex05_UUIDTableEntity.kt`**: Demonstrates entities with `UUID` primary keys.
- **`Ex06_NonAutoIncEntities.kt`**: Covers entities with non-auto-incrementing primary keys.
- **`Ex10_CompositeIdTableEntity.kt`**: Examples of entities with composite primary keys, composed of multiple columns.

### 4. Advanced Entity Features

- **`Ex07_EntityWithBlob.kt`**: Shows how to handle and store BLOB (Binary Large Object) data within an entity.
- **`Ex08_EntityFieldWithTransform.kt`
  **: Provides examples of transforming entity field values during persistence or retrieval (e.g., encryption, serialization).
- **`Ex09_ImmutableEntity.kt`**: Demonstrates how to define and work with immutable entities using Exposed.

### 5. Entity Relationships

- **`Ex11_ForeignIdEntity.kt`**: Illustrates entities that manage foreign key relationships with other entities.
- **`Ex12_Via.kt`**: Showcases the implementation of many-to-many relationships between entities using the
  `via` keyword in Exposed.
- **`Ex13_OrderedReference.kt`**: Examples related to entities with ordered references or relationships.
- **`Ex31_SelfReference.kt`
  **: Demonstrates how to model and work with self-referencing entities (e.g., hierarchical data structures).

## Purpose:

This module is designed to help users understand:

- The full capabilities of Exposed's Entity API for object-oriented database interaction.
- How to model complex domain models, including various primary key types and relationships.
- Implementing entity lifecycle logic and managing caching.
- Handling diverse data types and advanced entity features.

## Getting Started:

To explore these examples:

1. Review the source code in `src/test/kotlin/exposed/examples/entities`.
2. Run the test cases using your IDE or Gradle to observe how Exposed's Entity API facilitates robust and flexible data manipulation.

This module provides a deep dive into mastering Exposed's Entity API.
