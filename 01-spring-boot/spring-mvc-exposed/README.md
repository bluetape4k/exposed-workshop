# 01 Spring Boot: Spring MVC with Exposed

이 모듈(`spring-mvc-exposed`)은 초보자를 위한 **Spring MVC + Exposed** 예제입니다. 영화와 배우 데이터를 다루며, **동기 REST API
**를 만드는 흐름을 쉽게 따라갈 수 있도록 구성되어 있습니다.

## 이 모듈에서 배우는 것

| 주제                  | 설명                    |
|---------------------|-----------------------|
| **Spring MVC 기본**   | 동기 API 구조와 컨트롤러 작성법   |
| **Exposed DAO**     | Entity를 이용한 데이터 저장/조회 |
| **다대다 관계**          | 영화-배우 관계 모델링과 조인 테이블  |
| **Repository 패턴**   | 계층형 아키텍처 기본 구조        |
| **Virtual Threads** | Java 21 가상 스레드 활용     |

## Movie 스키마

![Movie Schema](MovieSchema_Dark.png)

### 테이블 정의

```kotlin
object MovieTable: LongIdTable("movies") {
    val name: Column<String> = varchar("name", 255)
    val producerName: Column<String> = varchar("producer_name", 255)
    val releaseDate: Column<LocalDateTime> = datetime("release_date")
}

object ActorTable: LongIdTable("actors") {
    val firstName: Column<String> = varchar("first_name", 255)
    val lastName: Column<String> = varchar("last_name", 255)
    val birthday: Column<LocalDate?> = date("birthday").nullable()
}

object ActorInMovieTable: Table("actors_in_movies") {
    val movieId: Column<EntityID<Long>> = reference("movie_id", MovieTable, onDelete = ReferenceOption.CASCADE)
    val actorId: Column<EntityID<Long>> = reference("actor_id", ActorTable, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(movieId, actorId)
}
```

### 관계 설명

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────┐
│  Movie      │     │ ActorInMovie     │     │  Actor      │
│─────────────│     │──────────────────│     │─────────────│
│  id (PK)    │◄────│  movie_id (FK)   │     │  id (PK)    │
│  name       │     │  actor_id (FK)   │────►│  firstName  │
│  producer   │     │  (복합 PK)        │     │  lastName   │
│  releaseDate│     └──────────────────┘     │  birthday   │
└─────────────┘                              └─────────────┘
```

## 프로젝트 구조

```
src/main/kotlin/
├── SpringMvcApplication.kt          # Spring Boot 진입점 (SERVLET)
├── config/
│   ├── ExposedDatabaseConfig.kt     # DB 연결 설정
│   ├── SwaggerConfig.kt             # Swagger UI 설정
│   └── TomcatVirtualThreadConfig.kt # Virtual Thread 설정
├── domain/
│   ├── model/
│   │   ├── MovieSchema.kt           # 테이블 정의
│   │   ├── MovieEntity.kt           # DAO Entity
│   │   ├── MovieRecords.kt          # DTO (API 응답용)
│   │   └── MovieMappers.kt          # ResultRow → DTO 변환
│   ├── repository/
│   │   ├── MovieRepository.kt       # 영화 리포지토리
│   │   └── ActorRepository.kt       # 배우 리포지토리
│   └── controller/
│       ├── MovieController.kt       # 영화 API
│       ├── ActorController.kt       # 배우 API
│       └── MovieActorsController.kt # 영화-배우 관계 API
└── DataInitializer.kt               # 샘플 데이터 초기화
```

## 주요 코드 예시

### Entity 정의

```kotlin
class MovieEntity(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<MovieEntity>(MovieTable)

    var name by MovieTable.name
    var producerName by MovieTable.producerName
    var releaseDate by MovieTable.releaseDate

    val actors by ActorEntity via ActorInMovieTable
}
```

### Repository

```kotlin
@Repository
class MovieRepository {

    fun findById(id: Long): MovieRecord? = transaction {
        MovieEntity.findById(id)?.toRecord()
    }

    fun findAll(): List<MovieRecord> = transaction {
        MovieEntity.all().map { it.toRecord() }
    }

    fun save(record: MovieRecord): MovieRecord = transaction {
        MovieEntity.new {
            name = record.name
            producerName = record.producerName
            releaseDate = record.releaseDate
        }.toRecord()
    }
}
```

### Controller

```kotlin
@RestController
@RequestMapping("/api/movies")
class MovieController(private val movieRepository: MovieRepository) {

    @GetMapping("/{id}")
    fun getMovie(@PathVariable id: Long): ResponseEntity<MovieRecord> {
        return movieRepository.findById(id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    @PostMapping
    fun createMovie(@RequestBody request: CreateMovieRequest): ResponseEntity<MovieRecord> {
        val movie = movieRepository.save(request.toRecord())
        return ResponseEntity.ok(movie)
    }
}
```

## 실행 방법

```bash
# 1. DB 설정 확인 (application.yml)
# 2. 애플리케이션 실행
./gradlew bootRun

# 3. API 테스트
curl http://localhost:8080/api/movies
```

## Virtual Threads 설정

Java 21 이상에서 Tomcat이 Virtual Threads를 사용하도록 설정:

```kotlin
@Bean
fun protocolHandlerVirtualThreadExecutorCustomizer(): TomcatProtocolHandlerCustomizer<*> {
    return TomcatProtocolHandlerCustomizer<ProtocolHandler> { protocolHandler ->
        protocolHandler.executor = Executors.newVirtualThreadPerTaskExecutor()
    }
}
```

## Swagger UI

애플리케이션 실행 후 http://localhost:8080/swagger-ui.html 에서 API 문서를 확인할 수 있습니다.
