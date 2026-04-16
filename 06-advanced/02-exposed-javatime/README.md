# 06 Advanced: exposed-java-time (02)

English | [한국어](./README.ko.md)

A module for mapping `java.time` types to Exposed columns. Practice time type storage/retrieval and literal/default value handling patterns.

## Learning Objectives

- Learn `LocalDate`, `LocalDateTime`, `Instant` mappings.
- Understand time-related SQL function usage.
- Verify timezone/precision differences across databases.

## Prerequisites

- [`../05-exposed-dml/02-types/README.md`](../05-exposed-dml/02-types/README.md)

## Java Time Type Mapping

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
classDiagram
    class JavaTimeTable {
        +date(name): Column~LocalDate~
        +datetime(name): Column~LocalDateTime~
        +timestamp(name): Column~Instant~
        +timestampWithTimeZone(name): Column~OffsetDateTime~
        +time(name): Column~LocalTime~
        +duration(name): Column~Duration~
    }

    class LocalDate {
        <<java.time>>
    }
    class LocalDateTime {
        <<java.time>>
    }
    class Instant {
        <<java.time>>
    }
    class OffsetDateTime {
        <<java.time>>
    }
    class LocalTime {
        <<java.time>>
    }
    class Duration {
        <<java.time>>
    }

    class DBColumn {
        <<Database>>
        DATE
        DATETIME / TIMESTAMP
        TIMESTAMP / TIMESTAMPTZ
        TIME
        BIGINT / INTERVAL
    }

    JavaTimeTable --> LocalDate : date()
    JavaTimeTable --> LocalDateTime : datetime()
    JavaTimeTable --> Instant : timestamp()
    JavaTimeTable --> OffsetDateTime : timestampWithTimeZone()
    JavaTimeTable --> LocalTime : time()
    JavaTimeTable --> Duration : duration()
    LocalDate --> DBColumn : DATE
    LocalDateTime --> DBColumn : DATETIME
    Instant --> DBColumn : TIMESTAMP
    OffsetDateTime --> DBColumn : TIMESTAMPTZ
    LocalTime --> DBColumn : TIME
    Duration --> DBColumn : BIGINT

    style JavaTimeTable fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style LocalDate fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style LocalDateTime fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style Instant fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style OffsetDateTime fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style LocalTime fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style Duration fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style DBColumn fill:#FFF3E0,stroke:#FFCC80,color:#E65100
```

## Key Concepts

### Java Time Column Declaration

```kotlin
object JavaTimeTable : IntIdTable("java_time_table") {
    val birthDate = date("birth_date")                           // LocalDate
    val lastLogin = datetime("last_login")                       // LocalDateTime
    val createdAt = timestamp("created_at")                      // Instant
    val modifiedAt = timestampWithTimeZone("modified_at")       // OffsetDateTime
    val eventTime = time("event_time")                           // LocalTime
    val duration = duration("duration")                          // Duration
    
    // With defaults
    val recordedAt = datetime("recorded_at").defaultExpression(CurrentDateTime())
    val signedUpAt = timestamp("signed_up_at").clientDefault { Instant.now() }
}
```

### CRUD Operations

```kotlin
withTables(testDB, JavaTimeTable) {
    // INSERT with LocalDate and LocalDateTime
    val id = JavaTimeTable.insertAndGetId {
        it[birthDate] = LocalDate.of(1990, 5, 15)
        it[lastLogin] = LocalDateTime.now()
        it[createdAt] = Instant.now()
        it[modifiedAt] = OffsetDateTime.now()
    }

    // SELECT returns java.time objects
    val row = JavaTimeTable.selectAll().where { 
        JavaTimeTable.id eq id 
    }.single()
    
    val birth = row[JavaTimeTable.birthDate]      // LocalDate
    val login = row[JavaTimeTable.lastLogin]      // LocalDateTime
    val created = row[JavaTimeTable.createdAt]    // Instant
    
    // UPDATE time fields
    JavaTimeTable.update({ JavaTimeTable.id eq id }) {
        it[lastLogin] = LocalDateTime.now()
        it[modifiedAt] = OffsetDateTime.now(ZoneId.of("Asia/Seoul"))
    }
}
```

### Time-based Queries

```kotlin
val recentDate = LocalDate.now().minusMonths(1)

// Query by LocalDate
JavaTimeTable.selectAll()
    .where { JavaTimeTable.birthDate greaterEq recentDate }
    .count()

// Query by Instant with millisecond precision
val oneHourAgo = Instant.now().minusSeconds(3600)
JavaTimeTable.selectAll()
    .where { JavaTimeTable.createdAt greaterEq oneHourAgo }

// Time literal comparisons
JavaTimeTable.selectAll()
    .where { JavaTimeTable.eventTime.cast<LocalTime>(LocalTimeColumnType()) less LocalTime.of(12, 0) }
```

### DAO Pattern

```kotlin
class EventEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<EventEntity>(JavaTimeTable)
    var eventTime by JavaTimeTable.eventTime
    var createdAt by JavaTimeTable.createdAt
}

val event = EventEntity.new {
    eventTime = LocalTime.now()
    createdAt = Instant.now()
}
println("Event at ${event.eventTime} on ${event.createdAt}")
```

## Advanced Scenarios

| File                      | Description          |
|---------------------------|----------------------|
| `Ex01_JavaTime.kt`        | Basic types/functions |
| `Ex02_Defaults.kt`        | Default value handling |
| `Ex03_DateTimeLiteral.kt` | Literal-based queries |
| `Ex04_MiscTable.kt`       | Integrated examples   |

## Precision Differences by Database

| DB         | timestamp Precision | timestampWithTimeZone Support |
|------------|--------------------|-----------------------------|
| PostgreSQL | Microseconds (μs)  | Supported (`TIMESTAMPTZ`)    |
| MySQL V8   | Microseconds (μs)  | Not supported (stores as UTC conversion) |
| MariaDB    | Microseconds (μs)  | Not supported                |
| H2         | Nanoseconds (ns)   | Supported                    |

`timestampWithTimeZone` is not supported on MySQL/MariaDB, and those tests are skipped using `Assumptions.assumeTrue`.

## How to Run

```bash
./gradlew :06-advanced:02-exposed-javatime:test
```

## Advanced Scenarios

### Timezone Handling

The `timestampWithTimeZone` column differs in how databases store timezone information.
Tests verify storage/retrieval consistency across various offsets such as Seoul/Cairo after INSERT, querying as UTC.

- Related file: [`Ex01_JavaTime.kt`](src/test/kotlin/exposed/examples/java/time/Ex01_JavaTime.kt)
- Test: `timestampWithTimeZone` — Verifies storage/retrieval consistency across multiple timezone offsets

### Date Default Value Configuration

Validates various default value strategies such as `defaultExpression(CurrentDateTime)` and `clientDefault { }`.
After changing defaults, `addMissingColumnsStatements` should not generate unnecessary `ALTER TABLE` statements.

- Related file: [`Ex02_Defaults.kt`](src/test/kotlin/exposed/examples/java/time/Ex02_Defaults.kt)
- Tests: `testDateDefaultDoesNotTriggerAlterStatement`, `testTimestampWithTimeZoneDefaultDoesNotTriggerAlterStatement`

## Practice Checklist

- Compare results before and after timezone conversion of the same value.
- Record precision differences (seconds/milliseconds) by database.

## Performance and Stability Checkpoints

- Fix the application reference timezone (UTC recommended)
- Maintain type consistency for time literal comparisons

## Next Module

- [`../03-exposed-kotlin-datetime/README.md`](../03-exposed-kotlin-datetime/README.md)
