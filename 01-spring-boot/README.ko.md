# 01 Spring Boot with Exposed

[English](./README.md) | 한국어

Spring Boot + Exposed로 REST API를 구현하는 챕터입니다. 동기 블로킹(Spring MVC + Virtual Threads)과 비동기 논블로킹(Spring WebFlux + Kotlin Coroutines) 두 가지 웹 모델에서 동일한 영화/배우 도메인을 구현하며 Exposed 트랜잭션 처리 방식의 차이를 비교합니다.

## 챕터 목표

- Spring MVC와 WebFlux에서 Exposed 기반 Repository 패턴을 비교하며 일관된 데이터 흐름을 확인한다.
- Virtual Threads와 Kotlin Coroutines 각각의 트랜잭션/커넥션 처리 방식의 차이를 파악한다.
- Swagger/OpenAPI를 통해 API를 자동 문서화하고 검증한다.

## 선수 지식

- Spring Boot 기본 개념 (컨트롤러, 서비스, 트랜잭션)
- [`00-shared/exposed-shared-tests`](../00-shared/exposed-shared-tests/README.ko.md): 공통 테스트 베이스 클래스와 DB 설정

---

## 포함 모듈

| 모듈                       | 서버     | 동시성 모델                             | 트랜잭션 관리                            |
|--------------------------|--------|------------------------------------|------------------------------------|
| `spring-mvc-exposed`     | Tomcat | Virtual Threads (블로킹 허용)           | `@Transactional` (Spring AOP)      |
| `spring-webflux-exposed` | Netty  | Kotlin Coroutines + Dispatchers.IO | `newSuspendedTransaction { }` (직접) |

### 모듈 비교

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
classDiagram
    class SpringMvcModule {
        <<spring-mvc-exposed>>
        Tomcat + Virtual Threads
        @Transactional
        HttpServletRequest
        ActorRepository (동기)
        MovieRepository (동기)
    }

    class SpringWebFluxModule {
        <<spring-webflux-exposed>>
        Netty + Coroutines
        newSuspendedTransaction
        ServerHttpRequest
        ActorRepository (suspend)
        MovieRepository (suspend)
    }

    class SharedDomain {
        <<공통 도메인>>
        MovieTable / MovieEntity
        ActorTable / ActorEntity
        ActorInMovieTable
        MovieRecord / ActorRecord
    }

    SpringMvcModule --> SharedDomain : 동일 스키마 사용
    SpringWebFluxModule --> SharedDomain : 동일 스키마 사용

    style SpringMvcModule fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style SpringWebFluxModule fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style SharedDomain fill:#FFF3E0,stroke:#FFCC80,color:#E65100
```

---

## 공통 도메인 구조

두 모듈은 동일한 스키마와 REST API 구조를 공유합니다.

### 데이터베이스 스키마

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
erDiagram
    MOVIES {
        bigint id PK
        varchar name
        varchar producer_name
        date release_date
    }
    ACTORS {
        bigint id PK
        varchar first_name
        varchar last_name
        date birthday
    }
    ACTORS_IN_MOVIES {
        bigint movie_id FK
        bigint actor_id FK
    }
    MOVIES ||--o{ ACTORS_IN_MOVIES : "출연"
    ACTORS ||--o{ ACTORS_IN_MOVIES : "출연"
```

### REST API 구조

| 컨트롤러                    | 경로              | 주요 기능                      |
|-------------------------|-----------------|----------------------------|
| `ActorController`       | `/actors`       | 배우 CRUD (조회, 검색, 생성, 삭제)   |
| `MovieController`       | `/movies`       | 영화 CRUD (조회, 검색, 생성, 삭제)   |
| `MovieActorsController` | `/movie-actors` | 영화-배우 관계 조회, 카운트, 제작자 겸 배우 |

---

## 권장 학습 순서

1. **`spring-mvc-exposed`**: 익숙한 동기 모델에서 Exposed DSL/DAO 패턴을 먼저 익힌다.
2. **`spring-webflux-exposed`**: 동일 도메인을 suspend 함수와 `newSuspendedTransaction`으로 구현하는 차이를 확인한다.

---

## 실행 방법

```bash
# Spring MVC 모듈 기동
./gradlew :01-spring-boot:spring-mvc-exposed:bootRun

# Spring WebFlux 모듈 기동
./gradlew :01-spring-boot:spring-webflux-exposed:bootRun

# 각 모듈 테스트
./gradlew :01-spring-boot:spring-mvc-exposed:test
./gradlew :01-spring-boot:spring-webflux-exposed:test

# Swagger UI (두 모듈 모두 동일 경로)
open http://localhost:8080/swagger-ui.html
```

---

## 테스트 포인트

- 동기/비동기 API에서 동일한 도메인 결과가 일관되게 반환되는지 검증한다.
- Swagger UI가 실행 시 `/swagger-ui.html`에서 자동으로 노출되는지 확인한다.
- 잘못된 파라미터(`birthday=invalid-date`)가 전달될 때 예외 없이 전체 목록이 반환되는지 확인한다.

## 성능·안정성 체크포인트

- **MVC**: Virtual Threads를 늘렸을 때 커넥션 풀/DB 부하가 급등하지 않는지 점검한다.
- **WebFlux**: Netty EventLoop와 Exposed JDBC 트랜잭션이 `Dispatchers.IO`로 올바르게 분리되는지 확인한다.
- 두 모듈 모두 `spring.profiles.active=postgres`로 전환하여 PostgreSQL에서 동작을 검증한다.

---

## 다음 챕터

- [02-alternatives-to-jpa](../02-alternatives-to-jpa/README.ko.md): R2DBC, Vert.x, Hibernate Reactive 등 JPA 대안 스택을 학습하는 챕터
