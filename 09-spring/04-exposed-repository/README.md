# 09 Spring: ExposedRepository 패턴

이 모듈은 Spring Boot 애플리케이션 내에서 Exposed를 사용한 리포지토리 패턴 구현 방법을 단계별로 학습합니다. 데이터 접근 로직을 캡슐화하여 서비스 레이어와 분리하는 클린 아키텍처 패턴을 다룹니다.

## 학습 목표

- 리포지토리 인터페이스와 구현체 설계 방법 이해
- Exposed 테이블과 쿼리 로직 캡슐화 기법 학습
- Spring 서비스와 리포지토리 통합 방법 습득
- 테스트 용이성을 위한 추상화 계층 구현 익히기

## 주요 기능

| 기능          | 설명                             |
|-------------|--------------------------------|
| 관심사 분리      | 서비스에 비즈니스 로직, 리포지토리에 데이터 접근 로직 |
| Exposed 추상화 | 서비스 계층에서 Exposed 직접 사용 숨김      |
| 테스트 용이성     | 리포지토리를 쉽게 모킹하여 서비스 단위 테스트 가능   |
| 유지보수성       | DB 스키마 변경이 리포지토리 내에 국한됨        |

## 프로젝트 구조

```
src/main/kotlin/
├── domain/
│   ├── model/
│   │   ├── User.kt           # 도메인 엔티티 (DTO)
│   │   └── UserTable.kt      # Exposed 테이블
│   └── repository/
│       ├── UserRepository.kt  # 리포지토리 인터페이스
│       └── UserRepositoryImpl.kt  # Exposed 구현체
├── service/
│   └── UserService.kt        # 비즈니스 로직
└── controller/
    └── UserController.kt     # REST API
```

## 코드 예제

### 도메인 모델

```kotlin
// DTO
data class User(
  val id: Int?,
  val name: String,
  val email: String,
  val createdAt: LocalDateTime?
)

// 테이블
object Users: IntIdTable("users") {
  val name = varchar("name", 255)
  val email = varchar("email", 255).uniqueIndex()
  val createdAt = datetime("created_at").nullable()
}
```

### 리포지토리 인터페이스

```kotlin
interface UserRepository {
    fun findAll(): List<User>
    fun findById(id: Int): User?
  fun findByEmail(email: String): User?
    fun save(user: User): User
  fun delete(id: Int)
  fun count(): Long
}
```

### 리포지토리 구현체

```kotlin
@Repository
class UserRepositoryImpl: UserRepository {

  init {
        transaction {
            SchemaUtils.create(Users)
          // 샘플 데이터
          Users.insert { it[name] = "Alice"; it[email] = "alice@example.com" }
          Users.insert { it[name] = "Bob"; it[email] = "bob@example.com" }
        }
    }

  override fun findAll(): List<User> = transaction {
    Users.selectAll().map { it.toUser() }
    }

  override fun findById(id: Int): User? = transaction {
    Users.select { Users.id eq id }
      .singleOrNull()
      ?.toUser()
  }

  override fun findByEmail(email: String): User? = transaction {
    Users.select { Users.email eq email }
      .singleOrNull()
      ?.toUser()
    }

  override fun save(user: User): User = transaction {
        if (user.id == null) {
          // Insert
          val id = Users.insertAndGetId {
            it[name] = user.name
            it[email] = user.email
            it[createdAt] = user.createdAt ?: LocalDateTime.now()
          }
          user.copy(id = id.value)
        } else {
          // Update
          Users.update({ Users.id eq user.id }) {
            it[name] = user.name
            it[email] = user.email
          }
            user
        }
    }

  override fun delete(id: Int) = transaction {
        Users.deleteWhere { Users.id eq id }
    }

  override fun count(): Long = transaction {
    Users.selectAll().count()
  }

  private fun ResultRow.toUser() = User(
    id = this[Users.id].value,
    name = this[Users.name],
    email = this[Users.email],
    createdAt = this[Users.createdAt]
  )
}
```

### 서비스 레이어

```kotlin
@Service
class UserService(private val userRepository: UserRepository) {

  fun getAllUsers(): List<User> = userRepository.findAll()

  fun getUser(id: Int): User? = userRepository.findById(id)

  fun getUserByEmail(email: String): User? = userRepository.findByEmail(email)

  fun createUser(name: String, email: String): User {
    // 비즈니스 로직
    require(name.isNotBlank()) { "이름은 필수입니다" }
    require(email.contains("@")) { "올바른 이메일 형식이 아닙니다" }

    // 중복 체크
    userRepository.findByEmail(email)?.let {
      throw UserAlreadyExistsException("이미 존재하는 이메일입니다")
    }

    return userRepository.save(User(null, name, email, null))
  }

  fun updateUser(id: Int, name: String, email: String): User {
    val user = userRepository.findById(id)
      ?: throw UserNotFoundException("사용자를 찾을 수 없습니다")

    return userRepository.save(user.copy(name = name, email = email))
  }

  fun deleteUser(id: Int) {
    userRepository.findById(id)
      ?: throw UserNotFoundException("사용자를 찾을 수 없습니다")

    userRepository.delete(id)
  }
}
```

### 컨트롤러

```kotlin
@RestController
@RequestMapping("/api/users")
class UserController(private val userService: UserService) {

  @GetMapping
  fun getAllUsers(): ResponseEntity<List<User>> {
    return ResponseEntity.ok(userService.getAllUsers())
  }

  @GetMapping("/{id}")
  fun getUser(@PathVariable id: Int): ResponseEntity<User> {
    return userService.getUser(id)
      ?.let { ResponseEntity.ok(it) }
      ?: ResponseEntity.notFound().build()
  }

  @PostMapping
  fun createUser(@RequestBody request: CreateUserRequest): ResponseEntity<User> {
    return try {
      val user = userService.createUser(request.name, request.email)
      ResponseEntity.status(HttpStatus.CREATED).body(user)
    } catch (e: UserAlreadyExistsException) {
      ResponseEntity.status(HttpStatus.CONFLICT).build()
    }
  }

  @PutMapping("/{id}")
  fun updateUser(
    @PathVariable id: Int,
    @RequestBody request: UpdateUserRequest
  ): ResponseEntity<User> {
    return try {
      val user = userService.updateUser(id, request.name, request.email)
      ResponseEntity.ok(user)
    } catch (e: UserNotFoundException) {
      ResponseEntity.notFound().build()
    }
  }

  @DeleteMapping("/{id}")
  fun deleteUser(@PathVariable id: Int): ResponseEntity<Void> {
    return try {
      userService.deleteUser(id)
      ResponseEntity.noContent().build()
    } catch (e: UserNotFoundException) {
      ResponseEntity.notFound().build()
    }
  }
}
```

## DAO 스타일 리포지토리

```kotlin
// Entity 정의
class UserEntity(id: EntityID<Int>): IntEntity(id) {
  companion object: IntEntityClass<UserEntity>(Users)

  var name by Users.name
  var email by Users.email
  var createdAt by Users.createdAt
}

// DAO 기반 리포지토리
@Repository
class UserDaoRepository: UserRepository {

  override fun findById(id: Int): User? = transaction {
    UserEntity.findById(id)?.toDomain()
  }

  override fun save(user: User): User = transaction {
    if (user.id == null) {
      UserEntity.new {
        name = user.name
        email = user.email
        createdAt = user.createdAt ?: LocalDateTime.now()
      }.toDomain()
    } else {
      UserEntity[user.id].apply {
        name = user.name
        email = user.email
      }.toDomain()
    }
  }

  private fun UserEntity.toDomain() = User(
    id = this.id.value,
    name = this.name,
    email = this.email,
    createdAt = this.createdAt
  )
}
```

## 실행

```bash
# 애플리케이션 실행
./gradlew bootRun

# API 테스트
curl http://localhost:8080/api/users

# 테스트 실행
./gradlew :09-spring:04-exposed-repository:test
```

## 더 읽어보기

- [ExposedRepository with Spring Web](https://debop.notion.site/ExposedRepository-1c32744526b080208e5ee03b900d2c5e)
- [Hexagonal Architecture](https://www.baeldung.com/hexagonal-architecture-ports-adapters)
