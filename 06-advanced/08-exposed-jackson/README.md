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
%%{init: {'theme': 'base', 'backgroundColor': '#FAFAFA', 'themeVariables': {'background': '#FAFAFA', 'fontFamily': '"Comic Mono", "goorm sans code", "JetBrains Mono", "goorm sans"'}}}%%
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
%%{init: {'theme': 'base', 'backgroundColor': '#FAFAFA', 'themeVariables': {'background': '#FAFAFA', 'fontFamily': '"Comic Mono", "goorm sans code", "JetBrains Mono", "goorm sans"'}}}%%
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

- ObjectMapper configuration
- JSON column mapping
- Version compatibility

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
