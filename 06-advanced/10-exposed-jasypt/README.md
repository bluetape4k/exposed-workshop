# Exposed-Jasypt: Deterministic Encryption with Jasypt

This module demonstrates how to integrate the **Jasypt (Java Simplified Encryption)
** library with Exposed to provide transparent column encryption and decryption. A key feature of this integration is its
**deterministic
** nature, which means the same plaintext input will always produce the same ciphertext output. This property is crucial for allowing encrypted data to be directly used in
`WHERE` clauses for equality checks.

This module addresses the limitation of non-deterministic encryption schemes (like the default
`exposed-crypt` module) where encrypted data cannot be directly queried.

## Learning Objectives

- Understand how to define Jasypt-encrypted columns (`jasyptVarChar`, `jasyptBinary`).
- Perform transparent encryption and decryption of string and binary data.
- Leverage deterministic encryption to query encrypted columns directly in `WHERE` clauses.
- Apply Jasypt-encrypted columns in both DSL and DAO programming styles.

## Key Concepts

### Deterministic Encryption

Unlike many standard encryption approaches that add randomness (salting, IVs) to produce different ciphertexts for the same plaintext, Jasypt can be configured to produce consistent ciphertext. This allows SQL equality comparisons (
`WHERE encrypted_column = 'encrypted_value'`) to work correctly.

**Trade-off
**: While enabling searchability, deterministic encryption provides less cryptographic strength against attacks that exploit patterns in repeated data. It's suitable for scenarios where searchability is a strict requirement and the data's sensitivity allows for this trade-off.

### Column Types

- **`jasyptVarChar(name, length, encryptor)`**: Defines a column for encrypting `String` values using Jasypt.
- **`jasyptBinary(name, length, encryptor)`**: Defines a column for encrypting `ByteArray` values using Jasypt.

### `Encryptors`

The `Encryptors` enum (e.g., `Encryptors.AES`,
`Encryptors.RC4`) specifies the encryption algorithm and implicitly handles the key management setup for Jasypt.

## Examples Overview

### `JasyptColumnTypeTest.kt` (DSL-style)

This file demonstrates the usage of Jasypt-encrypted columns within the Exposed DSL.

- **CRUD Operations**: Shows how to `insert` and `update` both encrypted `String` and
  `ByteArray` fields. The encryption/decryption is transparent.
- **Searchability**: Explicitly highlights that encrypted columns can be used in `WHERE` clauses for
  `eq` comparisons, which is a major advantage of deterministic encryption.

### `JasyptColumnTypeDaoTest.kt` (DAO-style)

This file mirrors `JasyptColumnTypeTest.kt` but applies the concepts to the Exposed DAO API.

- **Entity Mapping**: Shows how `jasyptVarChar` and `jasyptBinary` columns are mapped to entity properties.
- **Seamless DAO Usage
  **: CRUD operations on entities are transparent, and querying by encrypted properties works as expected.

## Code Snippets

### 1. Defining a Table with Jasypt-Encrypted Columns

```kotlin
import io.bluetape4k.exposed.core.jasypt.jasyptVarChar
import io.bluetape4k.exposed.core.jasypt.jasyptBinary
import io.bluetape4k.crypto.encrypt.Encryptors

object UserSecrets: IntIdTable("user_secrets") {
    val username = varchar("username", 255)

    // Encrypted string column for an API key, using AES.
    // This column will be searchable.
    val apiKey = jasyptVarChar("api_key", 512, Encryptors.AES)

    // Encrypted binary column for a secret token, using RC4.
    // This column will also be searchable.
    val secretToken = jasyptBinary("secret_token", 256, Encryptors.RC4)
}
```

### 2. Inserting and Querying Encrypted Data (DSL)

```kotlin
// Insert an encrypted record
val id = UserSecrets.insertAndGetId {
    it[username] = "john.doe"
    it[apiKey] = "my_super_secret_api_key_123"
    it[secretToken] = "binary_token_data".toByteArray()
}

// Retrieve and verify
val retrievedUser = UserSecrets.selectAll().where { UserSecrets.id eq id }.single()
retrievedUser[UserSecrets.username] shouldBeEqualTo "john.doe"
retrievedUser[UserSecrets.apiKey] shouldBeEqualTo "my_super_secret_api_key_123"
retrievedUser[UserSecrets.secretToken].toUtf8String() shouldBeEqualTo "binary_token_data"

// Querying on an encrypted column (works because encryption is deterministic)
val userByApiKey = UserSecrets.selectAll().where { UserSecrets.apiKey eq "my_super_secret_api_key_123" }.single()
userByApiKey[UserSecrets.username] shouldBeEqualTo "john.doe"
```

## Test Execution

```bash
# Run all tests in this module
./gradlew :06-advanced:10-exposed-jasypt:test

# Run tests for a specific class
./gradlew :06-advanced:10-exposed-jasypt:test --tests "exposed.examples.jasypt.JasyptColumnTypeTest"
```
