---
title: "06 Advanced"
tags: [exposed, crypt, json, jackson, money, custom-column, custom-entity, tink, jasypt]
category: architecture
created: 2026-04-13
updated: 2026-04-13
---

# 06 Advanced

실무 확장: 암복호화, JSON, 날짜/시간, 커스텀 컬럼/엔티티.

## 모듈 (12개)

| 모듈 | 설명 |
|------|------|
| `01-exposed-crypt` | AES/Blowfish/3DES 비결정적 암호화 |
| `02-exposed-javatime` | java.time 타입 매핑 |
| `03-exposed-kotlin-datetime` | kotlinx.datetime 타입 매핑 (KMP) |
| `04-exposed-json` | kotlinx.serialization JSON/JSONB 경로 쿼리 |
| `05-exposed-money` | JavaMoney MonetaryAmount 복합 컬럼 |
| `06-custom-columns` | 압축(GZIP/LZ4/Snappy/ZSTD), 직렬화(Kryo/Fury), 암호화 커스텀 ColumnType |
| `07-custom-entities` | KSUID/Snowflake/Time-based UUID/Base62 UUID 커스텀 ID Entity |
| `08-exposed-jackson` | Jackson 2 JSON/JSONB 컬럼 |
| `09-exposed-fastjson2` | Alibaba Fastjson2 JSON |
| `10-exposed-jasypt` | Jasypt 결정적 암호화 (WHERE 검색 가능) |
| `11-exposed-jackson3` | Jackson 3.x JSON/JSONB |
| `12-exposed-tink` | Google Tink AEAD/DAEAD (DAEAD = WHERE 검색 가능) |

## 권장 학습 순서

`06-custom-columns` → `04-exposed-json` → `01-exposed-crypt` → `12-exposed-tink` → `07-custom-entities`

## 실행

```bash
./gradlew :06-advanced:01-exposed-crypt:test
./gradlew :06-advanced:06-custom-columns:test
```
