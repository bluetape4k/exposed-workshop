# 02 JPA 대안: Hibernate Reactive Example

이 모듈(`hibernate-reactive-example`)은 **Hibernate Reactive
**를 사용하여 반응형 Spring Boot 애플리케이션을 구축하는 예제입니다. 전통적인 JPA나 Exposed의 대안으로 비동기/논블로킹 데이터베이스 작업을 수행하는 방법을 학습합니다.

## Hibernate Reactive란?

Hibernate Reactive는 Hibernate ORM의 반응형 버전으로, Non-blocking 데이터베이스 액세스를 가능하게 합니다. 기존 JPA 지식을 활용하면서도 Vert.x 기반의 반응형 프로그래밍 모델을 사용할 수 있습니다.

### 주요 특징

| 특징               | 설명                           |
|------------------|------------------------------|
| **JPA 호환**       | 기존 JPA 어노테이션 그대로 사용          |
| **Non-blocking** | Vert.x 기반 비동기 실행             |
| **Mutiny 지원**    | Mutiny API를 통한 반응형 스트림       |
| **Stage API**    | CompletableFuture 스타일 API 지원 |

## 프로젝트 구조

```
src/main/kotlin/
├── HibernateReactiveApplication.kt        # Spring Boot 진입점 (REACTIVE)
├── domain/
│   ├── model/
│   │   ├── Team.kt                        # Team 엔티티
│   │   └── Member.kt                      # Member 엔티티
│   ├── repository/
│   │   ├── TeamRepository.kt              # Team 리포지토리
│   │   └── MemberRepository.kt            # Member 리포지토리
│   └── controller/
│       ├── TeamController.kt              # Team REST API
│       └── MemberController.kt            # Member REST API
├── dto/
│   └── TeamDto.kt                         # 데이터 전송 객체
├── mapper/
│   └── TeamMapper.kt                      # Entity ↔ DTO 변환
└── utils/
    └── ReactiveUtils.kt                   # 반응형 유틸리티
```

## 도메인 모델

### Entity 정의

```kotlin
@Entity
@Table(name = "teams")
class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    var name: String = ""

    @OneToMany(mappedBy = "team", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var members: MutableList<Member> = mutableListOf()
}

@Entity
@Table(name = "members")
class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    var name: String = ""
    var age: Int = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    var team: Team? = null
}
```

### ERD

```
┌─────────────────┐       ┌─────────────────┐
│     teams       │       │    members      │
│─────────────────│       │─────────────────│
│  id (PK)        │       │  id (PK)        │
│  name           │       │  name           │
│                 │       │  age            │
│                 │       │  team_id (FK)   │
└─────────────────┘       └─────────────────┘
        ▲                         │
        │                         │
        └─────────────────────────┘
              1 : N 관계
```

## 반응형 리포지토리

### Mutiny 기반

```kotlin
@Repository
class TeamRepository(private val sessionFactory: Mutiny.SessionFactory) {

    fun findAll(): Uni<List<Team>> {
        return sessionFactory.withSession { session ->
            session.createQuery("FROM Team", Team::class.java)
                .resultList
        }
    }

    fun findById(id: Long): Uni<Team?> {
        return sessionFactory.withSession { session ->
            session.find(Team::class.java, id)
        }
    }

    fun save(team: Team): Uni<Team> {
        return sessionFactory.withTransaction { session ->
            session.persist(team).map { team }
        }
    }
}
```

### Stage 기반 (CompletableFuture)

```kotlin
@Repository
class TeamStageRepository(private val sessionFactory: Stage.SessionFactory) {

    fun findAll(): CompletionStage<List<Team>> {
        return sessionFactory.withSession { session ->
            session.createQuery("FROM Team", Team::class.java)
                .getResultList()
        }
    }
}
```

## REST Controller

```kotlin
@RestController
@RequestMapping("/api/teams")
class TeamController(private val teamRepository: TeamRepository) {

    @GetMapping
    fun getAll(): Uni<ResponseEntity<List<TeamDto>>> {
        return teamRepository.findAll()
            .map { teams -> teams.map { it.toDto() } }
            .map { ResponseEntity.ok(it) }
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): Uni<ResponseEntity<TeamDto>> {
        return teamRepository.findById(id)
            .map { team -> team?.toDto() }
            .map { dto ->
                dto?.let { ResponseEntity.ok(it) }
                    ?: ResponseEntity.notFound().build()
            }
    }

    @PostMapping
    fun create(@RequestBody request: CreateTeamRequest): Uni<ResponseEntity<TeamDto>> {
        val team = Team().apply { name = request.name }
        return teamRepository.save(team)
            .map { ResponseEntity.ok(it.toDto()) }
    }
}
```

## 설정

### application.yml

```yaml
spring:
    datasource:
        url: jdbc:postgresql://localhost:5432/testdb
        username: user
        password: password

quarkus:
    hibernate-orm:
        database:
            generation: drop-and-create
```

## 실행 방법

```bash
# 1. DB 설정 (application.yml)
# 2. 애플리케이션 실행
./gradlew bootRun

# 3. API 테스트
curl http://localhost:8080/api/teams
```

## Hibernate Reactive vs JPA vs Exposed

| 항목          | JPA           | Hibernate Reactive | Exposed                 |
|-------------|---------------|--------------------|-------------------------|
| **실행 모델**   | Blocking      | Non-blocking       | Blocking (Coroutine 지원) |
| **API 스타일** | EntityManager | Mutiny/Stage       | DSL/DAO                 |
| **코루틴**     | 지원 안함         | 제한적 지원             | 완벽 지원                   |
| **학습 곡선**   | 중간            | 높음                 | 낮음                      |

## 참고

- Hibernate Reactive는 JPA의 모든 기능을 지원하지 않습니다.
- Lazy Loading은 세션이 열려 있는 동안에만 작동합니다.
- 트랜잭션 관리가 기존 JPA와 다릅니다.
