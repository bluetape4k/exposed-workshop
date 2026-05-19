# 06 Advanced

English | [한국어](./README.ko.md)

A chapter covering custom columns, date/time, JSON, encryption/decryption, and serialization integration topics needed when applying Exposed in production environments.

## Overview

This chapter covers extension scenarios frequently encountered in production beyond basic CRUD. It demonstrates transparently protecting sensitive data with encryption columns, composing flexible schemas with JSON columns, and encapsulating serialization/compression/encryption logic in custom column types tailored to domain requirements.

## Learning Objectives

- Understand custom columns and extension points, and control cross-DB differences in JSON/time type handling.
- Design stable data flows when integrating serialization/encryption with external libraries.
- Validate custom type/entity extension modules through tests.

## Included Modules

| Module                       | Description                                              |
|------------------------------|----------------------------------------------------------|
| `01-exposed-crypt`           | `encryptedVarchar`/`encryptedBinary` encryption columns  |
| `02-exposed-javatime`        | Java Time type mapping (`LocalDate`, `Instant`, etc.)    |
| `03-exposed-kotlin-datetime` | Kotlin `kotlinx-datetime` type mapping                   |
| `04-exposed-json`            | JSON/JSONB column mapping and path queries               |
| `05-exposed-money`           | `BigDecimal`-based monetary type modeling                |
| `06-custom-columns`          | Compression/serialization/encryption custom column types  |
| `07-custom-entities`         | KSUID/Snowflake/UUID-based custom ID Entity              |
| `08-exposed-jackson`         | Jackson ObjectMapper JSON column integration             |
| `09-exposed-fastjson2`       | Fastjson2 JSON column integration                        |
| `10-exposed-jasypt`          | Jasypt-based deterministic encryption (WHERE searchable) |
| `11-exposed-jackson3`        | Jackson3 JSON column integration                         |
| `12-exposed-tink`            | Google Tink AEAD/DAEAD encryption columns                |

## Architecture Overview

![Architecture Overview 1](../docs/images/readme-diagrams/06-advanced-diagram-01.png)

## Module Classification

![Module Classification 2](../docs/images/readme-diagrams/06-advanced-diagram-02.png)

## Recommended Learning Order

1. `06-custom-columns` — Understand the basic structure of ColumnType extensions
2. `04-exposed-json` — JSON/JSONB columns and path queries
3. `01-exposed-crypt` — Transparent encryption/decryption columns
4. `12-exposed-tink` — Advanced AEAD/DAEAD encryption
5. `07-custom-entities` — Custom ID strategies
6. Remaining modules (date/time, serialization library integration)

## Prerequisites

- Content from `05-exposed-dml`
- Basic understanding of JSON/time handling

## How to Run Tests

```bash
# Individual module tests
./gradlew :06-advanced:01-exposed-crypt:test
./gradlew :06-advanced:04-exposed-json:test
./gradlew :06-advanced:06-custom-columns:test
./gradlew :06-advanced:07-custom-entities:test
./gradlew :06-advanced:12-exposed-tink:test

# Quick test targeting H2 only
./gradlew :06-advanced:01-exposed-crypt:test -PuseFastDB=true
```

## Test Points

- Verify no data loss during serialization/deserialization round-trips.
- Validate custom column null/default value handling logic.
- Measure JSON serialization cost and column size increase impact.
- Review index strategies and search constraints when using encryption columns.

## Next Chapter

- [07-jpa](../07-jpa/README.md): Covers practical patterns for migrating existing JPA projects to Exposed.
