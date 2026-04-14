# 06 Advanced: exposed-fastjson2 (09)

English | [한국어](./README.ko.md)

A module for handling JSON columns using Fastjson2. Provides integration patterns for environments that require an alternative serialization stack to Jackson.

## Learning Objectives

- Learn JSON mapping based on Fastjson2.
- Understand the differences compared to existing JSON modules.
- Validate serialization configuration and security options.

## Prerequisites

- [`../04-exposed-json/README.md`](../04-exposed-json/README.md)

## Fastjson2 Processing Flow

```mermaid
%%{init: {'theme': 'base', 'backgroundColor': '#FAFAFA', 'themeVariables': {'background': '#FAFAFA', 'fontFamily': '"Comic Mono", "goorm sans code", "JetBrains Mono", "goorm sans"'}}}%%
flowchart LR
    subgraph KotlinObj["Kotlin Object"]
        DH["DataHolder\n(user, logins, active, team)"]
        UG["UserGroup\n(users: List~User~)"]
    end

    subgraph Fastjson2["Fastjson2 Engine"]
        SER["JSON.toJSONString()"]
        DESER["JSON.parseObject()"]
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

    subgraph Comparison["Advantages over Jackson"]
        PERF["High-performance parsing"]
        SEC["Security options\n(auto-type restrictions)"]
        COMPAT["Partial Jackson API\ncompatibility"]
    end

    Fastjson2 --> PERF & SEC & COMPAT

    classDef blue fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef green fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef orange fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    classDef teal fill:#E0F2F1,stroke:#80CBC4,color:#00695C

    class DH,UG blue
    class SER,DESER green
    class JCOL,JBCOL orange
    class PERF,SEC,COMPAT teal
```

## Key Concepts

- Fastjson2 serialization/deserialization
- JSON column mapping
- Per-library compatibility

## Running Tests

```bash
./gradlew :09-exposed-fastjson2:test
```

## Practice Checklist

- Compare Jackson and Fastjson2 serialization output for the same data.
- Review security-related options (e.g., auto-type).

## Performance and Stability Checkpoints

- Data compatibility testing is mandatory when changing serialization libraries.
- Strengthen the security policy for external JSON input parsing paths.

## Next Module

- [`../10-exposed-jasypt/README.md`](../10-exposed-jasypt/README.md)
