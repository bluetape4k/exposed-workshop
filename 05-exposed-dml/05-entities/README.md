# 05 Exposed DML: Entity API

이 모듈(`05-entities`)은 Exposed의 강력한 Entity API를 단계별로 학습합니다. 기본 엔티티 작성부터 복잡한 관계 매핑, 라이프사이클 훅, 캐싱까지 DAO 패턴의 모든 측면을 다룹니다.

## 학습 목표

- Entity와 EntityClass를 사용한 객체지향 데이터 접근 이해
- 다양한 기본키 전략(Long, UUID, 복합키) 구현 방법 학습
- Entity 라이프사이클 훅을 통한 감사(Auditing) 기능 구현
- Entity 캐싱을 통한 성능 최적화 기법 습득
- 복잡한 관계(1:1, 1:N, M:N, Self-Reference) 매핑 방법 익히기

## 예제 구성

모든 예제는 `src/test/kotlin/exposed/examples/entities` 아래에 있습니다.

### 기본 Entity

| 파일                  | 설명             | 핵심 기능                          |
|---------------------|----------------|--------------------------------|
| `EntityTestData.kt` | 공통 테스트 데이터     | 테이블 정의, 샘플 데이터                 |
| `Ex01_Entity.kt`    | 기본 Entity CRUD | `new()`, `findById()`, `all()` |

### 라이프사이클 및 캐싱

| 파일                             | 설명              | 핵심 기능                         |
|--------------------------------|-----------------|-------------------------------|
| `Ex02_EntityHook.kt`           | Entity 라이프사이클 훅 | `beforeInsert`, `afterUpdate` |
| `Ex02_EntityHook_Auditable.kt` | 감사(Auditing) 기능 | 생성/수정 일시 자동 설정                |
| `Ex03_EntityCache.kt`          | Entity 캐싱       | 캐시 활용, 캐시 무효화                 |

### 기본키 전략

| 파일                               | 설명             | 핵심 기능                       |
|----------------------------------|----------------|-----------------------------|
| `Ex04_LongIdTableEntity.kt`      | Long 타입 기본키    | `LongIdTable`, `LongEntity` |
| `Ex05_UUIDTableEntity.kt`        | UUID 타입 기본키    | `UUIDTable`, `UUIDEntity`   |
| `Ex06_NonAutoIncEntities.kt`     | 자동 증가하지 않는 기본키 | 수동 ID 할당                    |
| `Ex10_CompositeIdTableEntity.kt` | 복합 기본키         | `CompositeIdTable`          |

### 고급 기능

| 파일                                 | 설명            | 핵심 기능                 |
|------------------------------------|---------------|-----------------------|
| `Ex07_EntityWithBlob.kt`           | BLOB 데이터 처리   | `blob` 컬럼             |
| `Ex08_EntityFieldWithTransform.kt` | 필드 변환 (암호화 등) | `transform()`, 커스텀 매핑 |
| `Ex09_ImmutableEntity.kt`          | 불변 Entity     | 읽기 전용 엔티티             |

### 관계 매핑

| 파일                         | 설명            | 핵심 기능                            |
|----------------------------|---------------|----------------------------------|
| `Ex11_ForeignIdEntity.kt`  | 외래키 관계        | `reference`, `optionalReference` |
| `Ex12_Via.kt`              | 다대다 관계 (via)  | `via`, 중간 테이블                    |
| `Ex13_OrderedReference.kt` | 순서가 있는 참조     | 정렬된 컬렉션                          |
| `Ex31_SelfReference.kt`    | 자기 참조 (계층 구조) | `optReference`, 트리 구조            |

## 핵심 개념

### 1. 기본 Entity 정의

```kotlin
// 테이블 정의
object Users : IntIdTable("users") {
    val name = varchar("name", 255)
    val email = varchar("email", 255).uniqueIndex()
    val age = integer("age").nullable()
}

// Entity 정의
class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(Users)
    
    var name by Users.name
    var email by Users.email
    var age by Users.age
}

// 사용법
transaction {
    // 생성
    val user = User.new {
        name = "John Doe"
        email = "john@example.com"
        age = 30
    }
    
    // 조회
    val found = User.findById(user.id)
    val all = User.all().toList()
    
    // 수정
    user.name = "Jane Doe"
    
    // 삭제
    user.delete()
}
```

### 2. 라이프사이클 훅

```kotlin
class AuditableEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AuditableEntity>(AuditableTable)
    
    var createdAt by AuditableTable.createdAt
    var updatedAt by AuditableTable.updatedAt
    var createdBy by AuditableTable.createdBy
    
    // 삽입 전 호출
    override fun beforeInsert(flush: Boolean) {
        createdAt = LocalDateTime.now()
        createdBy = getCurrentUser()
    }
    
    // 수정 전 호출
    override fun beforeUpdate(flush: Boolean) {
        updatedAt = LocalDateTime.now()
    }
    
    // 삽입 후 호출
    override fun afterInsert(flush: Boolean) {
        log.info("Entity created: ${this.id}")
    }
    
    // 수정 후 호출
    override fun afterUpdate(flush: Boolean) {
        log.info("Entity updated: ${this.id}")
    }
}
```

### 3. 다양한 기본키 전략

```kotlin
// Long 기본키
object Products : LongIdTable("products") {
    val name = varchar("name", 255)
}
class Product(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Product>(Products)
    var name by Products.name
}

// UUID 기본키
object Sessions : UUIDTable("sessions") {
    val data = text("data")
}
class Session(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Session>(Sessions)
    var data by Sessions.data
}

// 복합 기본키
object OrderItems : CompositeIdTable("order_items") {
    val orderId = reference("order_id", Orders).entityId()
    val productId = reference("product_id", Products).entityId()
    val quantity = integer("quantity")
    
    override val primaryKey = PrimaryKey(orderId, productId)
}
```

### 4. 관계 매핑

```kotlin
// 일대다 관계
object Posts : IntIdTable("posts") {
    val title = varchar("title", 255)
}
object Comments : IntIdTable("comments") {
    val postId = reference("post_id", Posts)
    val content = text("content")
}

class Post(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Post>(Posts)
    
    var title by Posts.title
    val comments by Comment referrersOn Comments.postId
}

class Comment(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Comment>(Comments)
    
    var content by Comments.content
    var post by Post referencedOn Comments.postId
}

// 다대다 관계 (via)
object Students : IntIdTable("students") {
    val name = varchar("name", 255)
}
object Courses : IntIdTable("courses") {
    val name = varchar("name", 255)
}
object Enrollments : Table("enrollments") {
    val studentId = reference("student_id", Students)
    val courseId = reference("course_id", Courses)
    override val primaryKey = PrimaryKey(studentId, courseId)
}

class Student(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Student>(Students)
    
    var name by Students.name
    val courses by Course via Enrollments
}

// 자기 참조 (트리 구조)
object Categories : IntIdTable("categories") {
    val name = varchar("name", 255)
    val parentId = optReference("parent_id", Categories)
}

class Category(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Category>(Categories)
    
    var name by Categories.name
    var parent by Category optionalReferencedOn Categories.parentId
    val children by Category optionalReferrersOn Categories.parentId
}
```

### 5. Eager Loading (N+1 문제 해결)

```kotlin
// Lazy Loading (N+1 발생)
val posts = Post.all().toList()
posts.forEach { post ->
    println(post.comments)  // 각 post마다 쿼리 실행
}

// Eager Loading
val posts = Post.all().with(Post::comments).toList()
posts.forEach { post ->
    println(post.comments)  // 이미 로드됨, 추가 쿼리 없음
}

// 단일 엔티티 Eager Loading
val post = Post.findById(1)?.load(Post::comments)
```

### 6. Entity 캐싱

```kotlin
// 캐시는 기본적으로 활성화됨
transaction {
    val user1 = User.findById(1)  // DB 조회
    val user2 = User.findById(1)  // 캐시에서 조회
    
    // 캐시 무효화
    entityCache.clear()
    
    // 특정 테이블 캐시 무효화
    entityCache.remove(Users)
    
    // 캐시 사용하지 않음
    User.findById(1)?.also { it.refresh(flush = true) }
}
```

### 7. 필드 변환

```kotlin
object Users : IntIdTable("users") {
    // 암호화된 필드
    val password = varchar("password", 255).transform(
        wrap = { encrypted -> decrypt(encrypted) },
        unwrap = { plain -> encrypt(plain) }
    )
    
    // JSON 필드
    val settings = text("settings").transform(
        wrap = { json -> Json.decodeFromString<UserSettings>(json) },
        unwrap = { settings -> Json.encodeToString(settings) }
    )
}
```

## 테스트 실행

```bash
# 전체 테스트 실행
./gradlew :05-exposed-dml:05-entities:test

# 특정 테스트만 실행
./gradlew :05-exposed-dml:05-entities:test --tests "exposed.examples.entities.Ex12_Via"
```

모든 테스트는 `@ParameterizedTest`를 사용하여 H2, MySQL, PostgreSQL 등 다양한 데이터베이스 환경에서 실행됩니다.

## 더 읽어보기

- [7.5 Entities](https://debop.notion.site/1ad2744526b0806f872cc2a78a6e058b?v=1ad2744526b080679361000ca7b72a88)
- [Exposed Wiki: DAO](https://github.com/JetBrains/Exposed/wiki/DAO)
