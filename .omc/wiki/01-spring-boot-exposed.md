---
title: "01 Spring Boot with Exposed"
tags: [exposed, spring-boot, spring-mvc, webflux, virtual-threads, coroutines]
category: architecture
created: 2026-04-13
updated: 2026-04-13
---

# 01 Spring Boot with Exposed

Spring Boot + Exposed REST API. 동기(Spring MVC + Virtual Threads)와 비동기(WebFlux + Coroutines) 비교.

## 모듈 비교

| 모듈 | 서버 | 동시성 | 트랜잭션 |
|------|------|--------|--------|
| `spring-mvc-exposed` | Tomcat | Virtual Threads | `@Transactional` |
| `spring-webflux-exposed` | Netty | Coroutines + Dispatchers.IO | `newSuspendedTransaction` |

## 도메인

Movies - Actors 다대다 관계 (ActorInMovieTable).

### REST API

- `/actors` — 배우 CRUD
- `/movies` — 영화 CRUD
- `/movie-actors` — 영화-배우 관계, 카운트, 제작자 겸 배우

## 핵심 패턴

- **MVC**: `@Transactional` + DSL/DAO 혼용. Virtual Thread executor로 블로킹 I/O 동시성 확보.
- **WebFlux**: `newSuspendedTransaction` + suspend fun. Netty EventLoop와 Exposed JDBC를 `Dispatchers.IO`로 분리.
- DAO eager loading: `MovieEntity.findById(id)?.load(MovieEntity::actors)`

## 실행

```bash
./gradlew :spring-mvc-exposed:test
./gradlew :spring-webflux-exposed:test
```
