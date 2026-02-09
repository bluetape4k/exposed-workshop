# Custom Columns and Default Value Generators

This module is a collection of advanced examples demonstrating how to extend Exposed's functionality by creating custom column types and client-side default value generators. These techniques allow you to add features like transparent encryption, compression, binary serialization, and custom ID generation directly into your table definitions.

## Learning Objectives

- Create and use custom client-side default value generators for columns (e.g., for unique IDs).
- Implement custom column types for transparent data transformation, such as compression and encryption.
- Understand how to build searchable (deterministic) encrypted columns.
- Learn to store arbitrary Kotlin objects in binary columns using serialization.
- Combine multiple transformations, like serialization and compression.

---

## 1. Custom Client-Side Default Generators

**(Source: `CustomClientDefaultFunctionsTest.kt`)**

Exposed's
`clientDefault` mechanism can be wrapped in extension functions to create reusable, descriptive ID generators. These functions are called by your application
*before* the `INSERT` statement is sent to the database.

### Key Concepts

- **`clientDefault { ... }`
  **: A function on a column definition that executes a lambda to generate a default value if one isn't provided.
- **Extension Functions**: By wrapping `clientDefault` in your own functions, you can create a clean, declarative API.

### Examples

- **.timebasedGenerated()**: Generates a time-based (version 1) UUID.
- **.snowflakeGenerated()**: Generates a unique, k-ordered ID using the Snowflake algorithm.
- **.ksuidGenerated()**: Generates a K-Sortable Unique Identifier, which is time-ordered and lexicographically sortable.

### Code Snippet

```kotlin
import io.bluetape4k.exposed.core.ksuidGenerated
import io.bluetape4k.exposed.core.snowflakeGenerated
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object ClientGenerated: IntIdTable() {
    // These columns will be auto-populated on insert if no value is provided.
    val snowflake: Column<Long> = long("snowflake").snowflakeGenerated()
    val ksuid: Column<String> = varchar("ksuid", 27).ksuidGenerated()
}

// Usage (DSL)
ClientGenerated.insert {
    // No need to specify values for snowflake or ksuid
}

// Usage (DAO)
class ClientGeneratedEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<ClientGeneratedEntity>(ClientGenerated)
    // ...
}
ClientGeneratedEntity.new {
    // Properties are generated automatically
}
```

---

## 2. Transparent Compression

**(Source: `compress/`)**

This example shows how to create custom column types that automatically compress data before writing it to the database and decompress it upon reading. This is ideal for reducing storage space for large
`TEXT` or `BLOB` fields.

### Key Concepts

- **`compressedBinary(name, length, compressor)`**: A custom column type that maps to a `VARBINARY` database column.
- **`compressedBlob(name, compressor)`**: A custom column type that maps to a `BLOB` database column.
- **`Compressors`**: An object/enum providing different compression algorithms, such as `LZ4`, `Snappy`, and `Zstd`.

### Code Snippet

```kotlin
import io.bluetape4k.exposed.core.compress.compressedBlob
import io.bluetape4k.io.compressor.Compressors

private object CompressedTable: IntIdTable() {
    // This column will store Zstd-compressed data in a BLOB field.
    val compressedContent = compressedBlob("zstd_blob", Compressors.Zstd).nullable()
}

// Usage
val largeData = "some very long string...".toByteArray()
CompressedTable.insert {
    // The `largeData` ByteArray is automatically compressed here.
    it[compressedContent] = largeData
}

val row = CompressedTable.selectAll().single()
// The data is automatically decompressed when read.
val originalData = row[CompressedTable.compressedContent]
```

---

## 3. Searchable (Deterministic) Encryption

**(Source: `encrypt/`)**

This example implements custom column types for transparent, **deterministic
** encryption. "Deterministic" means that the same input will always produce the same encrypted output.

This is a critical distinction from the
`exposed-crypt` module, which uses non-deterministic encryption. While less secure, deterministic encryption allows you to perform direct equality checks on the encrypted data in
`WHERE` clauses.

### Key Concepts

- **`encryptedVarChar(name, length, encryptor)`**: A custom column for storing encrypted strings that can be searched.
- **`encryptedBinary(name, length, encryptor)`
  **: A custom column for storing encrypted byte arrays that can be searched.
- **`Encryptors`**: Provides various symmetric encryption algorithms (`AES`,
  `RC4`, etc.) configured for deterministic output.

### Code Snippet

```kotlin
import io.bluetape4k.exposed.core.encrypt.encryptedVarChar
import io.bluetape4k.crypto.encrypt.Encryptors

private object EncryptedUsers: IntIdTable("EncryptedUsers") {
    val email = encryptedVarChar("email", 256, Encryptors.AES)
}

// Usage
val userEmail = "test@example.com"
EncryptedUsers.insert {
    it[email] = userEmail
}

// Because encryption is deterministic, we can search by the plaintext value.
val user = EncryptedUsers.selectAll().where { EncryptedUsers.email eq userEmail }.single()

// The value is decrypted automatically upon retrieval.
user[EncryptedUsers.email] shouldBeEqualTo userEmail
```

---

## 4. Binary Serialization

**(Source: `serialization/`)**

This example shows how to store any `java.io.Serializable` Kotlin object in a binary database column (`VARBINARY` or
`BLOB`). This is an alternative to JSON for storing structured data, and can be more space-efficient, especially when combined with compression.

### Key Concepts

- **`binarySerializedBinary<T>(name, length, serializer)`**: Maps a `Serializable` object of type `T` to a
  `VARBINARY` column.
- **`binarySerializedBlob<T>(name, serializer)`**: Maps a `Serializable` object of type `T` to a `BLOB` column.
- **`BinarySerializers`
  **: Provides different binary serialization libraries, often combined with a compression algorithm (e.g., `LZ4Kryo`,
  `ZstdFory`).

### Code Snippet

```kotlin
import io.bluetape4k.exposed.core.serializable.binarySerializedBlob
import io.bluetape4k.io.serializer.BinarySerializers
import java.io.Serializable

// The data class must be Serializable
data class UserProfile(val username: String, val settings: Map<String, String>): Serializable

private object UserData: IntIdTable("UserData") {
    // Store a UserProfile object in a BLOB, serialized with Kryo and compressed with LZ4.
    val profile = binarySerializedBlob<UserProfile>("profile", BinarySerializers.LZ4Kryo)
}

// Usage
val userProfile = UserProfile("john.doe", mapOf("theme" to "dark", "lang" to "en"))
UserData.insert {
    it[profile] = userProfile
}

// The object is automatically deserialized and decompressed on read.
val retrievedProfile = UserData.selectAll().first()[UserData.profile]
retrievedProfile.settings["theme"] shouldBeEqualTo "dark"
```

## Test Execution

```bash
# Run all tests in this module
./gradlew :06-advanced:06-custom-columns:test

# Run tests for a specific feature, e.g., compression
./gradlew :06-advanced:06-custom-columns:test --tests "exposed.examples.custom.columns.compress.*"
```
