---
title: "10 Multi-Tenant"
tags: [exposed, multi-tenant, schema, spring-mvc, virtual-threads, webflux]
category: architecture
created: 2026-04-13
updated: 2026-04-13
---

# 10 Multi-Tenant

Schema 기반 멀티테넌시. Spring MVC / Virtual Thread / WebFlux 세 환경 비교.

## 전략

Shared Database / Separate Schema: 하나의 DB에 테넌트별 스키마 분리 (`korean`, `english`).

## 모듈

| 모듈 | 서버 | 컨텍스트 전파 | 스키마 전환 |
|------|------|------------|---------|
| `01-multitenant-spring-web` | Tomcat | `ThreadLocal` | AOP `@Before` |
| `02-multitenant-spring-web-virtualthread` | Tomcat + VT | `ScopedValue` | AOP `@Before` |
| `03-multitenant-spring-webflux` | Netty | Reactor `Context` | `newSuspendedTransaction` 내부 |

## 공통 흐름

1. Filter가 `X-TENANT-ID` 헤더에서 테넌트 추출
2. 컨텍스트에 바인딩 (ThreadLocal / ScopedValue / ReactorContext)
3. 스키마 전환 (`SchemaUtils.setSchema`)
4. 테넌트 격리된 쿼리 실행

## 실행

```bash
./gradlew :01-multitenant-spring-web:test
./gradlew :02-multitenant-spring-web-virtualthread:test
./gradlew :03-multitenant-spring-webflux:test
```
