# 06 Advanced: Exposed Crypt

This module (
`01-exposed-crypt`) provides examples and test cases demonstrating the integration of encryption into Exposed applications using the
`exposed-crypt` extension. It showcases how to define and utilize encrypted columns for sensitive data, ensuring data security within your database, whether you're using Exposed's SQL DSL or its powerful Entity API.

## Key Features and Components:

### 1. Encrypted Columns with SQL DSL (`Ex01_EncryptedColumn.kt`)

- Illustrates how to define and interact with encrypted columns using Exposed's SQL Domain Specific Language (DSL).
- Covers the process of writing and reading data to/from encrypted fields, demonstrating transparent encryption and decryption at the database level.

### 2. Encrypted Columns with Entity API (`Ex02_EncryptedColumnWithEntity.kt`)

- Demonstrates the usage of encrypted columns when working with Exposed's Entity API.
- Shows how to seamlessly integrate encryption into your entity definitions, allowing for secure data handling through object-oriented models.

## Purpose:

This module is designed to help users understand:

- How to enhance the security of their Exposed applications by encrypting sensitive data in database columns.
- The implementation of `exposed-crypt` with both Exposed's SQL DSL and Entity API.
- Best practices for handling encrypted data within a Kotlin/Exposed environment.

## Getting Started:

To explore these examples:

1. Review the source code in `src/test/kotlin/exposed/examples/crypt`.
2. Run the test cases using your IDE or Gradle to observe how `exposed-crypt` facilitates data encryption.

This module provides a clear guide to implementing encryption in your Exposed projects.
