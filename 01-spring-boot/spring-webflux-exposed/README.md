# 01 Spring Boot: Spring WebFlux with Exposed

이 모듈(`spring-webflux-exposed`)은 초보자를 위한 **Spring WebFlux + Exposed** 예제입니다. 영화와 배우 데이터를 다루며, **비동기 REST API
**를 만드는 흐름을 쉽게 따라갈 수 있도록 구성되어 있습니다.

## 이 모듈에서 배우는 것

| 주제                       | 설명                           |
|--------------------------|------------------------------|
| **WebFlux 기본**           | 비동기 API 구조와 Reactive Streams |
| **Exposed + Coroutines** | `newSuspendTransaction` 활용법  |
| **다대다 관계**               | 영화-배우 관계 모델링                 |
| **Repository 패턴**        | 비동기 리포지토리 구현                 |
| **Non-blocking I/O**     | Netty 기반 고성능 처리              |

## Movie 스키마

![Movie Schema](MovieSchema_Dark.png)

### 테이블 정의

```kotlin
object MovieTable: LongIdTable("movies") {
    val name = varchar("name", 255).index()
    val producerName = varchar("producer_name", 255).index()
    val releaseDate = datetime("release_date")
}

object ActorTable: LongIdTable("actors") {
    val firstName = varchar("first_name", 255).index()
    val lastName = varchar("last_name", 255).index()
    val birthday = date("birthday").nullable()
}

object ActorInMovieTable: Table("actors_in_movies") {
    val movieId = reference("movie_id", MovieTable, onDelete = ReferenceOption.CASCADE)
    val actorId = reference("actor_id", ActorTable, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(movieId, actorId)
}
```

## 프로젝트 구조

```
src/main/kotlin/
├── SpringWebfluxApplication.kt      # Spring Boot 진입점 (REACTIVE)
├── config/
│   └── ExposedDatabaseConfig.kt     # DB 연결 설정
├── domain/
│   ├── model/
│   │   ├── MovieSchema.kt           # 테이블 정의
│   │   ├── MovieEntity.kt           # DAO Entity
│   │   ├── MovieRecords.kt          # DTO (API 응답용)
│   │   └── MovieMappers.kt          # ResultRow → DTO 변환
│   ├── repository/
│   │   ├── MovieRepository.kt       # 영화 리포지토리 (suspend)
│   │   └── ActorRepository.kt       # 배우 리포지토리 (suspend)
│   └── controller/
│       ├── MovieController.kt       # 영화 API (suspend)
│       ├── ActorController.kt       # 배우 API (suspend)
│       └── MovieActorsController.kt # 영화-배우 관계 API
└── utils/
    └── CoroutinesUtils.kt           # 코루틴 유틸리티
```

## MVC vs WebFlux 비교

| 항목         | Spring MVC         | Spring WebFlux                    |
|------------|--------------------|-----------------------------------|
| **어노테이션**  | `@GetMapping`      | `@GetMapping` (동일)                |
| **리턴 타입**  | 일반 객체              | `Mono<T>`, `Flux<T>` 또는 `suspend` |
| **트랜잭션**   | `transaction { }`  | `newSuspendTransaction { }`       |
| **스레드 모델** | Thread per Request | Event Loop                        |
| **백프레셔**   | 없음                 | Reactive Streams 지원               |

## 주요 코드 예시

### Suspend Repository

```kotlin
@Repository
class MovieRepository {

    suspend fun findById(id: Long): MovieRecord? = newSuspendedTransaction(Dispatchers.IO) {
        MovieEntity.findById(id)?.toRecord()
    }

    suspend fun findAll(): List<MovieRecord> = newSuspendedTransaction(Dispatchers.IO) {
        MovieEntity.all().map { it.toRecord() }
    }

    suspend fun save(record: MovieRecord): MovieRecord = newSuspendedTransaction(Dispatchers.IO) {
        MovieEntity.new {
            name = record.name
            producerName = record.producerName
            releaseDate = record.releaseDate
        }.toRecord()
    }
}
```

### Suspend Controller

```kotlin
@RestController
@RequestMapping("/api/movies")
class MovieController(private val movieRepository: MovieRepository) {

    @GetMapping("/{id}")
    suspend fun getMovie(@PathVariable id: Long): ResponseEntity<MovieRecord> {
        return movieRepository.findById(id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    @PostMapping
    suspend fun createMovie(@RequestBody request: CreateMovieRequest): ResponseEntity<MovieRecord> {
        val movie = movieRepository.save(request.toRecord())
        return ResponseEntity.ok(movie)
    }

    @GetMapping
    suspend fun listMovies(): List<MovieRecord> {
        return movieRepository.findAll()
    }
}
```

### Flux를 사용하는 경우

```kotlin
@GetMapping("/flux")
fun listMoviesFlux(): Flux<MovieRecord> = flux {
        movieRepository.findAll().forEach { send(it) }
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

## 성능 고려사항

### WebFlux가 유리한 경우

- 높은 동시 연결 수 (수천~수만)
- I/O 바운드 작업이 많은 경우
- 마이크로서비스 간 통신이 잦은 경우
- 메모리 효율성이 중요한 경우

### MVC가 유리한 경우

- 낮은 동시 연결 수
- CPU 바운드 작업이 많은 경우
- 기존 MVC 코드베이스를 유지해야 하는 경우
- 학습 곡선을 최소화해야 하는 경우

## 참고

- WebFlux 환경에서는 `Dispatchers.IO`를 명시적으로 사용하여 블로킹 DB 작업을 격리해야 합니다.
- Exposed는 JDBC 기반이므로 본질적으로 블로킹입니다. 코루틴으로 래핑하여 비동기처럼 동작합니다.
