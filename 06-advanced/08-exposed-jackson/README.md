# 06 Advanced: exposed-jackson (08)

English | [한국어](./README.ko.md)

A module for serializing and deserializing JSON columns using Jackson. Provides integration examples suited for projects already using the Jackson ecosystem.

## Learning Objectives

- Learn JSON mapping based on the Jackson ObjectMapper.
- Understand JSON column CRUD and query patterns.
- Manage compatibility when serialization settings change.

## Prerequisites

- [`../04-exposed-json/README.md`](../04-exposed-json/README.md)

## Table Structure

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
erDiagram
    jackson_table {
        SERIAL id PK
        JSON jackson_column
    }
    jackson_b_table {
        SERIAL id PK
        JSONB jackson_b_column
    }
    jackson_arrays {
        SERIAL id PK
        JSON groups
        JSON numbers
    }
    jackson_b_arrays {
        SERIAL id PK
        JSONB groups
        JSONB numbers
    }
```

## Jackson Serialization Flow

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
flowchart LR
    subgraph KotlinObj["Kotlin Object"]
        DH["DataHolder\n(user, logins, active, team)"]
        UG["UserGroup\n(users: List~User~)"]
    end

    subgraph Jackson["Jackson ObjectMapper"]
        SER["ObjectMapper.writeValueAsString()"]
        DESER["ObjectMapper.readValue()"]
    end

    subgraph DBCol["DB Column"]
        JCOL["JSON column\n(text storage)"]
        JBCOL["JSONB column\n(binary, PostgreSQL)"]
    end

    DH -->|INSERT/UPDATE| SER --> JCOL
    DH -->|INSERT/UPDATE| SER --> JBCOL
    JCOL -->|SELECT| DESER --> DH
    JBCOL -->|SELECT| DESER --> DH
    UG -->|INSERT/UPDATE| SER --> JCOL

    classDef blue fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef green fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef orange fill:#FFF3E0,stroke:#FFCC80,color:#E65100

    class DH,UG blue
    class SER,DESER green
    class JCOL,JBCOL orange
```

## Key Concepts

### Jackson ObjectMapper Configuration

```kotlin
// Custom ObjectMapper with specific configuration
val objectMapper = ObjectMapper()
    .registerModule(KotlinModule.Builder().build())
    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

@Serializable
data class DataHolder(
    val user: String,
    val logins: Int,
    val active: Boolean,
    val team: String?,
)

object JacksonTable : IntIdTable("jackson_table") {
    val name = varchar("name", 50)
    // Store Kotlin object as JSON using Jackson
    val data = json<DataHolder>("data", objectMapper).nullable()
}
```

Generated DDL (PostgreSQL):

```sql
CREATE TABLE IF NOT EXISTS jackson_table (
    id    SERIAL PRIMARY KEY,
    name  VARCHAR(50) NOT NULL,
    data  JSON        NULL
)
```

### CRUD Operations with Jackson

```kotlin
withTables(testDB, JacksonTable) {
    // INSERT with Jackson serialization
    val id = JacksonTable.insertAndGetId {
        it[name] = "example"
        it[data] = DataHolder("Alice", 5, true, "Team A")
    }

    // SELECT returns deserialized object
    val row = JacksonTable.selectAll().where { JacksonTable.id eq id }.single()
    val dataObject = row[JacksonTable.data]  // DataHolder instance
    println("User: ${dataObject?.user}, Logins: ${dataObject?.logins}")

    // UPDATE with new object
    JacksonTable.update({ JacksonTable.id eq id }) {
        it[data] = DataHolder("Bob", 10, false, "Team B")
    }
}
```

### DAO Pattern with Jackson

```kotlin
class DataEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DataEntity>(JacksonTable)
    var name by JacksonTable.name
    var data by JacksonTable.data
}

val entity = DataEntity.new {
    name = "test"
    data = DataHolder("Charlie", 3, true, null)
}
println("Entity data: ${entity.data}")
```

### JSON Query with extract (PostgreSQL)

```kotlin
// Extract specific JSON field for filtering
JacksonTable.selectAll()
    .where { JacksonTable.data.extract<String>("$.user") eq "Alice" }
```

## Advanced Scenarios

- **Custom Serializers**: Register additional modules for special types (LocalDateTime, UUID, etc.)
- **Version Compatibility**: Test serialization format when upgrading Jackson versions
- **Performance**: Monitor JSON parsing overhead during large batch operations
- **Null Handling**: Properly configure FAIL_ON_UNKNOWN_PROPERTIES and inclusion policies

## Running Tests

```bash
./gradlew :08-exposed-jackson:test
```

## Practice Checklist

- Verify serialization behavior for date/enum/nullable fields.
- Add regression tests when ObjectMapper options change.

## Performance and Stability Checkpoints

- Excessive polymorphic configuration poses security and performance risks.
- Maintain the serialization format contract consistently across API and storage layers.

## Next Module

- [`../09-exposed-fastjson2/README.md`](../09-exposed-fastjson2/README.md)
