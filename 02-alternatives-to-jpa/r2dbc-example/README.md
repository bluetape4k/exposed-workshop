# 02 JPA 대안: R2DBC Example

이 모듈(`r2dbc-example`)은 **R2DBC(Reactive Relational Database Connectivity)**와 **Spring Data R2DBC
**를 사용하여 반응형 Spring Boot 애플리케이션을 구축하는 예제입니다.

## R2DBC란?

R2DBC는 반응형 프로그래밍을 위한 데이터베이스 연결 표준입니다. JDBC의 블로킹 한계를 극복하고, 완전한 Non-blocking 데이터베이스 액세스를 제공합니다.

### 주요 특징

| 특징                   | 설명                             |
|----------------------|--------------------------------|
| **Non-blocking**     | 완전한 비동기 I/O                    |
| **Reactive Streams** | Publisher/Subscriber 패턴        |
| **Spring 통합**        | Spring Data R2DBC 제공           |
| **다양한 DB 지원**        | PostgreSQL, MySQL, H2, MSSQL 등 |

## JDBC vs R2DBC

```
┌─────────────────────────────────────────────────────────────┐
│                         JDBC                                 │
│  Thread ──► Connection ──► Statement ──► ResultSet (Block)  │
│                          ▲                                   │
│                          │ 대기                              │
│                          ▼                                   │
│                    [DB Response]                             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                         R2DBC                                │
│  Thread ──► Connection ──► Statement ──► Publisher ──► 구독  │
│                                                          │   │
│                                          (Non-blocking)  │   │
│                                                          ▼   │
│                                                    [OnNext]   │
└─────────────────────────────────────────────────────────────┘
```

## 프로젝트 구조

```
src/main/kotlin/
├── R2dbcApplication.kt                    # Spring Boot 진입점 (REACTIVE)
├── domain/
│   ├── model/
│   │   ├── Post.kt                        # Post 엔티티
│   │   ├── Comment.kt                     # Comment 엔티티
│   │   └── Customer.kt                    # Customer 엔티티
│   └── repository/
│       ├── PostRepository.kt              # Post 리포지토리
│       ├── CommentRepository.kt           # Comment 리포지토리
│       └── CustomerRepository.kt          # Customer 리포지토리
├── controller/
│   ├── PostController.kt                  # Post REST API
│   └── CommentController.kt               # Comment REST API
├── config/
│   └── R2dbcConfig.kt                     # R2DBC 설정
├── exceptions/
│   └── GlobalExceptionHandler.kt          # 예외 처리
└── utils/
    └── R2dbcUtils.kt                      # 유틸리티
```

## 도메인 모델

### Entity 정의

```kotlin
@Table("posts")
data class Post(
    @Id
    val id: Long? = null,

    val title: String,

    val content: String,

    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Table("comments")
data class Comment(
    @Id
    val id: Long? = null,

    val content: String,

    @Column("post_id")
    val postId: Long,

    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

### ERD

```
┌─────────────────┐       ┌─────────────────┐
│     posts       │       │    comments     │
│─────────────────│       │─────────────────│
│  id (PK)        │       │  id (PK)        │
│  title          │       │  content        │
│  content        │       │  post_id (FK)   │
│  created_at     │       │  created_at     │
└─────────────────┘       └─────────────────┘
        ▲                         │
        │                         │
        └─────────────────────────┘
              1 : N 관계
```

## 반응형 리포지토리

### 기본 Repository

```kotlin
interface PostRepository: ReactiveCrudRepository<Post, Long> {

    fun findByTitleContaining(title: String): Flux<Post>

    fun findByCreatedAtAfter(date: LocalDateTime): Flux<Post>
}
```

### 커스텀 쿼리

```kotlin
@Repository
class CustomPostRepository(private val r2dbcEntityTemplate: R2dbcEntityTemplate) {

    fun findWithComments(postId: Long): Mono<PostWithComments> {
        return r2dbcEntityTemplate
            .select(Post::class.java)
            .matching(Query.query(Criteria.where("id").`is`(postId)))
            .one()
            .flatMap { post ->
                r2dbcEntityTemplate
                    .select(Comment::class.java)
                    .matching(Query.query(Criteria.where("post_id").`is`(postId)))
                    .all()
                    .collectList()
                    .map { comments -> PostWithComments(post, comments) }
            }
    }
}
```

## REST Controller

```kotlin
@RestController
@RequestMapping("/api/posts")
class PostController(private val postRepository: PostRepository) {

    @GetMapping
    fun getAll(): Flux<Post> {
        return postRepository.findAll()
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): Mono<ResponseEntity<Post>> {
        return postRepository.findById(id)
            .map { ResponseEntity.ok(it) }
            .defaultIfEmpty(ResponseEntity.notFound().build())
    }

    @PostMapping
    fun create(@RequestBody request: CreatePostRequest): Mono<Post> {
        val post = Post(
            title = request.title,
            content = request.content
        )
        return postRepository.save(post)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: UpdatePostRequest
    ): Mono<ResponseEntity<Post>> {
        return postRepository.findById(id)
            .flatMap { post ->
                val updated = post.copy(
                    title = request.title ?: post.title,
                    content = request.content ?: post.content
                )
                postRepository.save(updated)
            }
            .map { ResponseEntity.ok(it) }
            .defaultIfEmpty(ResponseEntity.notFound().build())
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): Mono<ResponseEntity<Void>> {
        return postRepository.deleteById(id)
            .then(Mono.just(ResponseEntity.noContent().build<Void>()))
    }
}
```

## 설정

### application.yml

```yaml
spring:
    r2dbc:
        url: r2dbc:postgresql://localhost:5432/testdb
        username: user
        password: password
        pool:
            initial-size: 5
            max-size: 20
```

### R2DBC 설정

```kotlin
@Configuration
class R2dbcConfig {

    @Bean
    fun r2dbcEntityTemplate(connectionFactory: ConnectionFactory): R2dbcEntityTemplate {
        return R2dbcEntityTemplate(connectionFactory)
    }
}
```

## 실행 방법

```bash
# 1. DB 설정 (application.yml)
# 2. 애플리케이션 실행
./gradlew bootRun

# 3. API 테스트
curl http://localhost:8080/api/posts
```

## R2DBC 제한사항

| 항목               | JPA                             | R2DBC                   |
|------------------|---------------------------------|-------------------------|
| **연관관계 매핑**      | 지원 (`@OneToMany`, `@ManyToOne`) | 미지원 (직접 조회 필요)          |
| **Lazy Loading** | 지원                              | 미지원                     |
| **트랜잭션**         | 선언적 (`@Transactional`)          | `TransactionalOperator` |
| **JPQL**         | 지원                              | 미지원 (Native Query 사용)   |

## 참고

- R2DBC는 연관관계 매핑을 자동으로 처리하지 않습니다.
- `@Transactional` 대신 `TransactionalOperator`를 사용해야 합니다.
- 복잡한 쿼리는 `DatabaseClient` 또는 Native Query를 사용합니다.
