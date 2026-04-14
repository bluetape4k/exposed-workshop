# 06 Advanced: exposed-jackson3 (11)

English | [한국어](./README.ko.md)

A module for integrating JSON columns using Jackson 3. Covers the serialization compatibility verification points needed when migrating from Jackson 2 to Jackson 3.

## Learning Objectives

- Learn Jackson 3-based mapping patterns.
- Understand the impact of breaking changes compared to Jackson 2.
- Verify JSON storage format compatibility through tests.

## Prerequisites

- [`../08-exposed-jackson/README.md`](../08-exposed-jackson/README.md)

## Jackson 3 Processing Flow

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
flowchart LR
    subgraph KotlinObj["Kotlin Object"]
        DH["DataHolder\n(user, logins, active, team)"]
        UG["UserGroup\n(users: List~User~)"]
    end

    subgraph Jackson3["Jackson 3 ObjectMapper"]
        SER["ObjectMapper.writeValueAsString()\n(Jackson 3 API)"]
        DESER["ObjectMapper.readValue()\n(Jackson 3 API)"]
    end

    subgraph DBCol["DB Column"]
        JCOL["JSON column\n(text storage)"]
        JBCOL["JSONB column\n(PostgreSQL)"]
    end

    DH -->|INSERT/UPDATE| SER --> JCOL
    DH -->|INSERT/UPDATE| SER --> JBCOL
    JCOL -->|SELECT| DESER --> DH
    JBCOL -->|SELECT| DESER --> DH
    UG -->|INSERT/UPDATE| SER --> JCOL

    subgraph Migration["Jackson 2 → 3 Changes"]
        PKG["Package: com.fasterxml → tools.jackson"]
        MOD["Module system changes"]
        COMPAT["Backward compatibility concerns"]
    end

    classDef blue fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef green fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef orange fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    classDef yellow fill:#FFFDE7,stroke:#FFF176,color:#F57F17

    class DH,UG blue
    class SER,DESER green
    class JCOL,JBCOL orange
    class PKG,MOD,COMPAT yellow
```

## Key Concepts

- Jackson 3 ObjectMapper configuration
- JSON column serialization contract
- Migration regression testing

## Running Tests

```bash
./gradlew :11-exposed-jackson3:test
```

## Practice Checklist

- Compare Jackson 2 and Jackson 3 serialization output for compatibility.
- Pin failing migration cases as regression tests.

## Performance and Stability Checkpoints

- Data contract testing is mandatory on major library upgrades.
- Centralize serialization configuration to maintain consistency across modules.

## Next Chapter

- [`../../07-jpa/README.md`](../../07-jpa/README.md)
