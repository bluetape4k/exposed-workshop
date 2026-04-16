# 06 Advanced: exposed-jasypt (10)

English | [한국어](./README.ko.md)

> **⚠️ Deprecated**: This module is deprecated. For new development, use [`bluetape4k-exposed-tink`](../12-exposed-tink/README.md) instead.

A module covering deterministic encryption columns using Jasypt. Learn to balance security and queryability in domains that require searchable encryption.

## Learning Objectives

- Understand how deterministic encryption columns work.
- Learn to design encryption fields that support `WHERE` clause searches.
- Understand the security trade-offs compared to standard encryption.

## Prerequisites

- [`../01-exposed-crypt/README.md`](../01-exposed-crypt/README.md)

## Jasypt Encryption Processing Flow

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
flowchart LR
    subgraph Write["Write Path (Deterministic Encryption)"]
        PT1["Plaintext"]
        JENC["Jasypt Encryptor\n.encrypt()"]
        DB_W["DB Storage\n(ciphertext — always identical)"]
    end

    subgraph Read["Read Path"]
        DB_R["DB Query\n(ciphertext)"]
        JDEC["Jasypt Encryptor\n.decrypt()"]
        PT2["Plaintext"]
    end

    subgraph Search["WHERE Searchable"]
        PT3["Search plaintext"]
        JENC2["Jasypt Encryptor\n.encrypt()"]
        WHERE["WHERE col = 'ciphertext'"]
        RESULT["Result rows"]
    end

    PT1 -->|INSERT/UPDATE| JENC --> DB_W
    DB_R -->|SELECT| JDEC --> PT2
    PT3 --> JENC2 --> WHERE --> RESULT

    subgraph Algorithms["Supported Algorithms"]
        AES["DeterministicAES"]
        RC4["DeterministicRC4"]
        TDES["TripleDES"]
        RC2["RC2"]
    end

    classDef blue fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef green fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef orange fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    classDef purple fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A

    class PT1,PT2,PT3 blue
    class JENC,JDEC,JENC2 green
    class DB_W,DB_R orange
    class WHERE,RESULT purple
    class AES,RC4,TDES,RC2 orange
```

## Key Concepts

### Deterministic Encrypted Column Declaration

```kotlin
// Jasypt encryptor for deterministic encryption
val emailEncryptor = Algorithms.DeterministicAES("mySecretPassword", "fixedSalt12345678")

object JasyptTable : IntIdTable("jasypt_table") {
    val name = varchar("name", 100)
    // Deterministic encryption — same plaintext always produces same ciphertext
    val email = encryptedVarchar("email", 200, emailEncryptor)
}
```

Generated DDL (PostgreSQL):

```sql
CREATE TABLE IF NOT EXISTS jasypt_table (
    id     SERIAL PRIMARY KEY,
    name   VARCHAR(100) NOT NULL,
    email  VARCHAR(200) NOT NULL
)
```

### CRUD with Deterministic Encryption

```kotlin
withTables(testDB, JasyptTable) {
    // INSERT — automatic encryption
    val id = JasyptTable.insertAndGetId {
        it[name] = "Alice"
        it[email] = "alice@example.com"  // Stored as encrypted value
    }

    // SELECT — automatic decryption
    val row = JasyptTable.selectAll().where { JasyptTable.id eq id }.single()
    println("Email: ${row[JasyptTable.email]}")  // alice@example.com (decrypted)

    // UPDATE with new encrypted value
    JasyptTable.update({ JasyptTable.id eq id }) {
        it[email] = "alice.new@example.com"
    }
}
```

### WHERE Clause Search on Encrypted Column

```kotlin
// Key advantage: Search works on encrypted field without decryption
// The WHERE value is also encrypted for comparison
JasyptTable.selectAll()
    .where { JasyptTable.email eq "alice@example.com" }  // Encrypts plaintext internally
    .forEach { row -> println(row[JasyptTable.name]) }

// NOTE: Identical plaintext always produces identical ciphertext
// allowing deterministic comparison in WHERE clauses
```

### DAO Pattern with Deterministic Encryption

```kotlin
class AccountEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AccountEntity>(JasyptTable)
    var name by JasyptTable.name
    var email by JasyptTable.email
}

val account = AccountEntity.new {
    name = "Bob"
    email = "bob@example.com"
}

// Query by encrypted field
val found = AccountEntity.find { JasyptTable.email eq "bob@example.com" }.firstOrNull()
println("Found: ${found?.name}")  // Found: Bob
```

## Advanced Scenarios

- **Key Rotation**: Plan strategy to re-encrypt data with new key/salt
- **Pattern Exposure Risk**: Deterministic encryption reveals when same plaintext appears
- **Salt Management**: Secure storage and rotation of encryption salt
- **Null Handling**: How NULL values in encrypted columns are handled

## Running Tests

```bash
./gradlew :10-exposed-jasypt:test
```

## Practice Checklist

- Verify that identical plaintext input always produces the same ciphertext.
- Validate that search conditions work correctly on encrypted fields.

## Performance and Stability Checkpoints

- Deterministic encryption risks pattern exposure, so apply only where necessary.
- Prepare a key rotation plan and data re-encryption strategy.

## Next Module

- [`../11-exposed-jackson3/README.md`](../11-exposed-jackson3/README.md)
