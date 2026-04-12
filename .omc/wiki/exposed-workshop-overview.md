---
title: Exposed Workshop Overview
tags: [exposed, kotlin, workshop, overview]
category: architecture
created: 2026-04-13
updated: 2026-04-13
---

# Exposed Workshop Overview

Kotlin Exposed 프레임워크의 단계별 학습 워크샵. Kotlin 2.3 / Java 21 / Spring Boot 3.x / Gradle 멀티모듈.

## 기술 스택

- Kotlin 2.3.20, Java 21, Exposed 1.1.1, Spring Boot 3.5.11, Kotlinx Coroutines 1.10.2, Bluetape4k 1.5.0-Beta1, Gradle 9.4.0

## 학습 순서

1. **00-shared** — 공통 테스트 유틸리티 ([[00-shared-test-infra]])
2. **01-spring-boot** — Spring MVC/WebFlux + Exposed REST API ([[01-spring-boot-exposed]])
3. **02-alternatives-to-jpa** — JPA 대안 기술 비교 ([[02-alternatives-to-jpa]])
4. **03-exposed-basic** — DSL/DAO 패턴 기초 ([[03-exposed-basic]])
5. **04-exposed-ddl** — 연결 관리 + 스키마 정의 ([[04-exposed-ddl]])
6. **05-exposed-dml** — DML/타입/함수/트랜잭션/Entity ([[05-exposed-dml]])
7. **06-advanced** — 암호화/JSON/커스텀 타입 ([[06-advanced]])
8. **07-jpa** — JPA 마이그레이션 ([[07-jpa-migration]])
9. **08-coroutines** — 코루틴/Virtual Thread ([[08-coroutines]])
10. **09-spring** — Spring 트랜잭션/캐시/Repository ([[09-spring-integration]])
11. **10-multi-tenant** — 멀티테넌시 ([[10-multi-tenant]])
12. **11-high-performance** — 캐시/라우팅/벤치마크 ([[11-high-performance]])

## 빌드 & 테스트

```bash
./gradlew clean build
./gradlew test -PuseFastDB=true        # H2만
./gradlew test -PuseDB=H2,POSTGRESQL   # 특정 DB
```

## Notion Ebook

상세 설명: [Kotlin Exposed Book](https://debop.notion.site/Kotlin-Exposed-Book-1ad2744526b080428173e9c907abdae2)
