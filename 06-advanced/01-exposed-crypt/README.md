# Exposed-Crypt: Transparent Column Encryption

This module demonstrates how to use the
`exposed-crypt` extension to transparently encrypt and decrypt data in your database columns. This is useful for protecting sensitive information like personal data, secrets, or financial details at rest.

## Learning Objectives

- Understand how to define encrypted columns in Exposed tables.
- Learn to use different encryption algorithms (`AES`, `Blowfish`, `Triple DES`).
- Apply encrypted columns in both DSL and DAO styles.
- Recognize the limitations of searching on encrypted columns.

## Key Concepts

The core of this functionality lies in custom column types that handle encryption and decryption automatically.

- `encryptedVarchar(name: String, colLength: Int, encryptor: Encryptor)`: Defines a
  `VARCHAR` column that stores its content as an encrypted string.
- `encryptedBinary(name: String, colLength: Int, encryptor: Encryptor)`: Defines a `VARBINARY` or
  `BYTEA` column that stores its content as encrypted binary data.
- `Encryptor`: An interface for the encryption/decryption logic. The library provides several implementations in the
  `org.jetbrains.exposed.v1.crypt.Algorithms` object.

**Important Note:** The default
`Encryptor` implementations in Exposed use algorithms that produce different ciphertexts for the same plaintext on each encryption call. This is a security feature to prevent pattern analysis. However, it means you
**cannot** perform direct equality checks (
`where { table.column eq "value" }`) on these columns. For searchable encryption, you would need a deterministic encryption algorithm, like the one provided by Jasypt (see the
`10-exposed-jasypt` example).

## Examples Overview

### `Ex01_EncryptedColumn.kt` (DSL-style)

This file demonstrates the basic usage of encrypted columns with the DSL API.

- **Table Definition**: Shows how to define a table with multiple columns, each using a different encryption algorithm (
  `AES_256_PBE_CBC`, `AES_256_PBE_GCM`, `BLOW_FISH`, `TRIPLE_DES`).
- **Insert & Update
  **: When you insert or update a value, it is automatically encrypted before being sent to the database. The application code only deals with plain text.
- **Select**: When you retrieve data, the column value is automatically decrypted.
- **Search Limitation**: Explicitly shows that a `select` query with a
  `where` clause on an encrypted column will not work as expected.

### `Ex02_EncryptedColumnWithEntity.kt` (DAO-style)

This file shows how to integrate encrypted columns with the DAO API for an entity-like experience.

- **Entity Definition**: An `IntEntity` is defined with properties (`varchar`, `binary`) that are mapped to
  `encryptedVarchar` and `encryptedBinary` columns.
- **CRUD Operations**: Creating (`ETest.new { ... }`), reading (
  `ETest.all()`), and updating entities works seamlessly. The encryption and decryption are completely transparent to the developer.
- **Search Limitation**: Reinforces that finding entities by an encrypted property (
  `ETest.find { TestTable.varchar eq "value" }`) will fail.

## Code Snippets

### 1. Defining a Table with Encrypted Columns (DSL)

```kotlin
val nameEncryptor = Algorithms.AES_256_PBE_CBC("passwd", "5c0744940b5c369b")

object StringTable: IntIdTable("StringTable") {
    val name: Column<String> = encryptedVarchar("name", 80, nameEncryptor)
    val city: Column<String> =
        encryptedVarchar("city", 80, Algorithms.AES_256_PBE_GCM("passwd", "5c0744940b5c369b"))
    val address: Column<String> = encryptedVarchar("address", 100, Algorithms.BLOW_FISH("key"))
}
```

### 2. Using Encrypted Columns with an Entity (DAO)

```kotlin
object TestTable: IntIdTable() {
    private val encryptor = Algorithms.AES_256_PBE_GCM("passwd", "12345678")
    val varchar = encryptedVarchar("varchar", 100, encryptor)
    val binary = encryptedBinary("binary", 100, encryptor)
}

class ETest(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<ETest>(TestTable)

    var varchar: String by TestTable.varchar
    var binary: ByteArray by TestTable.binary
}

// Usage is transparent
val entity = ETest.new {
    varchar = "my secret value"
    binary = "another secret".toByteArray()
}

println(entity.varchar) // Prints "my secret value"
```

## Test Execution

```bash
# Run all tests in this module
./gradlew :06-advanced:01-exposed-crypt:test

# Run a specific test class
./gradlew :06-advanced:01-exposed-crypt:test --tests "exposed.examples.crypt.Ex01_EncryptedColumn"
```

## Further Reading

- [Exposed Crypt 모듈](https://debop.notion.site/Exposed-Crypt-1c32744526b0802da419d5ce74d2c5f3)
