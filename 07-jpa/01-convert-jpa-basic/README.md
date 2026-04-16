# 07 JPA Migration: Basic Migration (01-convert-jpa-basic)

English | [한국어](./README.ko.md)

An introductory module for migrating basic JPA CRUD and relationship code to Exposed. Covers migration patterns that preserve functional equivalence while reducing dependencies.

## Learning Objectives

- Replace JPA Entity-centric code with Exposed DSL/DAO.
- Write equivalence tests comparing results before and after migration.
- Build an incremental migration strategy.

## Prerequisites

- JPA/Hibernate basics
- [`../../05-exposed-dml/README.md`](../../05-exposed-dml/README.md)

## JPA ↔ Exposed Basic CRUD Conversion Reference

| Operation       | JPA                                                          | Exposed DSL                                                | Exposed DAO                                      |
|-----------------|--------------------------------------------------------------|------------------------------------------------------------|--------------------------------------------------|
| Entity definition | `@Entity @Table(name="...")` class                         | `object XxxTable : LongIdTable("...")`                     | `class Xxx(id: EntityID<Long>) : LongEntity(id)` |
| Column definition | `@Column(name="col") val field: Type`                      | `val col = varchar("col", 128)`                            | `var field by XxxTable.col`                      |
| Save            | `em.persist(entity)`                                         | `XxxTable.insert { it[col] = value }`                      | `Xxx.new { field = value }`                      |
| Find (single)   | `em.find(Xxx::class.java, id)`                               | `XxxTable.selectAll().where { id eq targetId }.single()`   | `Xxx.findById(id)`                               |
| Find (list)     | `em.createQuery("SELECT x FROM Xxx x").resultList`           | `XxxTable.selectAll().toList()`                            | `Xxx.all().toList()`                             |
| Conditional find | `em.createQuery("SELECT x FROM Xxx x WHERE x.name = :name")` | `XxxTable.selectAll().where { name eq value }`            | `Xxx.find { XxxTable.name eq value }`            |
| Update          | Field change within persistence context (dirty checking)     | `XxxTable.update({ id eq targetId }) { it[col] = newVal }` | `entity.field = newVal`                          |
| Delete          | `em.remove(entity)`                                          | `XxxTable.deleteWhere { id eq targetId }`                  | `entity.delete()`                                |
| Transaction     | `@Transactional` / `em.transaction.begin()`                  | `transaction { ... }`                                      | `transaction { ... }`                            |
| Batch insert    | Repeated `em.persist()` + `flush()`                          | `XxxTable.batchInsert(list) { ... }`                       | Repeated `Xxx.new { ... }` then `flushCache()`   |
| Pagination      | `query.setFirstResult(offset).setMaxResults(limit)`          | `.limit(limit).offset(offset)`                             | `.limit(limit, offset)`                          |

## Key Concepts

### DSL Style (Direct SQL Control)

```kotlin
// Table definition
object SimpleTable : LongIdTable("simple_entity") {
    val name        = varchar("name", 255).uniqueIndex()
    val description = text("description").nullable()
}

// Insert
SimpleTable.batchInsert(names) { name ->
    this[SimpleTable.name] = name
    this[SimpleTable.description] = faker.lorem().sentence()
}

// Query + pagination
val names: List<String> = SimpleTable
    .select(SimpleTable.name)
    .limit(2).offset(2)
    .map { it[SimpleTable.name] }
```

### DAO Style (Object-centric)

```kotlin
// Entity class
class SimpleEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<SimpleEntity>(SimpleTable)
    var name        by SimpleTable.name
    var description by SimpleTable.description
}

// CRUD
transaction {
    val entity = SimpleEntity.new {
        name = "example"
        description = "test"
    }
    val found = SimpleEntity.findById(entity.id)
    found?.name = "updated"
}
```

## Relationship Mapping Conversion Diagram

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
classDiagram
    class BlogTable {
        +EntityID~Long~ id
        +String title
        +String content
    }
    class PostTable {
        +EntityID~Long~ id
        +EntityID~Long~ blogId
        +String content
    }
    class TagTable {
        +EntityID~Long~ id
        +String name
    }
    class BlogTagTable {
        +EntityID~Long~ blogId
        +EntityID~Long~ tagId
    }
    class PersonTable {
        +EntityID~Long~ id
        +String name
    }
    class AddressTable {
        +EntityID~Long~ id
        +EntityID~Long~ personId
        +String street
    }

    BlogTable "1" --> "0..*" PostTable : One-to-Many blogId FK
    BlogTable "0..*" --> "0..*" TagTable : Many-to-Many via BlogTagTable
    BlogTagTable --> BlogTable
    BlogTagTable --> TagTable
    PersonTable "1" --> "0..*" AddressTable : One-to-Many personId FK

    style BlogTable fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style PostTable fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style TagTable fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style BlogTagTable fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style PersonTable fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    style AddressTable fill:#FFF3E0,stroke:#FFCC80,color:#E65100
```

## Domain ERDs

### SimpleSchema ERD

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
erDiagram
    simple_entity {
        BIGSERIAL id PK
        VARCHAR_255 name "UNIQUE NOT NULL"
        TEXT description "NULL"
    }
```

### PersonSchema ERD

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
erDiagram
    addresses {
        BIGSERIAL id PK
        VARCHAR_255 street "NOT NULL"
        VARCHAR_255 city "NOT NULL"
        VARCHAR_2 state "NOT NULL"
        VARCHAR_10 zip "NULL"
    }
    persons {
        BIGSERIAL id PK
        VARCHAR_50 first_name "NOT NULL"
        VARCHAR_50 last_name "NOT NULL"
        DATE birth_date "NOT NULL"
        BOOLEAN employed "DEFAULT TRUE"
        VARCHAR_255 occupation "NULL"
        BIGINT address_id FK
    }
    persons }o--|| addresses : "N:1 (address_id)"
```

### BlogSchema ERD

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
erDiagram
    posts {
        BIGSERIAL id PK
        VARCHAR_255 title "NOT NULL"
    }
    post_details {
        BIGINT id PK "FK to posts"
        DATE created_on "NOT NULL"
        VARCHAR_255 created_by "NOT NULL"
    }
    post_comments {
        BIGSERIAL id PK
        BIGINT post_id FK
        VARCHAR_255 review "NOT NULL"
    }
    tags {
        BIGSERIAL id PK
        VARCHAR_255 name "NOT NULL"
    }
    post_tags {
        BIGSERIAL id PK
        BIGINT post_id FK
        BIGINT tag_id FK
    }

    posts ||--|| post_details : "1:1 (shared PK)"
    posts ||--o{ post_comments : "1:N (post_id)"
    posts ||--o{ post_tags : "N:N via post_tags"
    tags ||--o{ post_tags : "N:N via post_tags"
```

### BookSchema ERD (Composite PK)

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
erDiagram
    publishers {
        INT pub_id PK "autoIncrement"
        UUID isbn_code PK "autoGenerate"
        VARCHAR_32 publisher_name "NOT NULL"
    }
    authors {
        SERIAL id PK
        INT publisher_id FK
        UUID publisher_isbn FK
        VARCHAR_32 pen_name "NOT NULL"
    }
    books {
        INT book_id PK "autoIncrement"
        VARCHAR_32 title "NOT NULL"
        INT author_id FK "NULL"
    }
    reviews {
        VARCHAR_8 code PK
        BIGINT rank PK
        INT book_id FK "NOT NULL"
    }
    offices {
        VARCHAR_8 zip_code PK
        VARCHAR_64 name PK
        INT area_code PK
        BIGINT staff "NULL"
        INT publisher_id FK "NULL"
        UUID publisher_isbn FK "NULL"
    }

    publishers ||--o{ authors : "1:N (pub_id, isbn_code)"
    authors ||--o{ books : "1:N (author_id)"
    books ||--o{ reviews : "1:N (book_id)"
    publishers ||--o{ offices : "1:N (publisher_id, publisher_isbn)"
```

### Entity Class Diagram — JPA Entity vs Exposed DAO

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
classDiagram
    direction LR
    class JPA_SimpleEntity {
        <<JPA Entity>>
        @Entity @Table(name="simple_entity")
        @Id @GeneratedValue Long id
        @Column String name
        @Column String description
    }
    class Exposed_SimpleTable {
        <<Exposed Table (DSL)>>
        object SimpleTable : LongIdTable
        val name = varchar(255).uniqueIndex()
        val description = text().nullable()
    }
    class Exposed_SimpleEntity {
        <<Exposed Entity (DAO)>>
        class SimpleEntity : LongEntity
        var name by SimpleTable.name
        var description by SimpleTable.description
        companion object : LongEntityClass
    }

    JPA_SimpleEntity ..> Exposed_SimpleTable : DSL migration
    JPA_SimpleEntity ..> Exposed_SimpleEntity : DAO migration

    class JPA_PersonEntity {
        <<JPA Entity>>
        @Entity @ManyToOne Address address
        String firstName / lastName
        LocalDate birthDate
        Boolean employed
    }
    class Exposed_PersonDAO {
        <<Exposed DAO>>
        class Person : LongEntity
        var address by Address referencedOn addressId
        var firstName / lastName / birthDate
        var employed / occupation
    }

    JPA_PersonEntity ..> Exposed_PersonDAO : migration

    style JPA_SimpleEntity fill:#FFEBEE,stroke:#EF9A9A,color:#C62828
    style Exposed_SimpleTable fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style Exposed_SimpleEntity fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style JPA_PersonEntity fill:#FFEBEE,stroke:#EF9A9A,color:#C62828
    style Exposed_PersonDAO fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
```

## JPA Annotation → Exposed Mapping Reference

| JPA Annotation        | Exposed Implementation                                    | Notes                          |
|-----------------------|-----------------------------------------------------------|--------------------------------|
| `@OneToOne` (unidirectional) | `reference("col", OtherTable)` + `referencedOn`  | `reference` on FK-owning side  |
| `@OneToOne` (bidirectional) | Unidirectional + `optionalReferrersOn`             | `referrersOn` on back-reference side |
| `@OneToOne @MapsId`   | `IdTable` + `override val id = reference("id", Parent)`  | Shared PK pattern              |
| `@OneToMany`          | `referrersOn` (back-reference)                            | FK defined on child table      |
| `@ManyToOne`          | `reference("fk", ParentTable)` + `referencedOn`          | FK defined on child table      |
| `@ManyToMany`         | `via` + junction table                                    | Junction table explicitly defined |
| `@JoinColumn`         | FK column name specified directly                         | `reference("col_name", ...)`   |
| `@EmbeddedId`         | `CompositeIdTable`                                        | Composite PK table             |
| `@IdClass`            | `CompositeIdTable` + `addIdColumn`                        | Alternative composite PK       |
| `cascade = PERSIST`   | `SchemaUtils` + `ReferenceOption`                         | DB-level CASCADE configuration |
| `FetchType.EAGER`     | `.load(relation)` or `JOIN` query                         | Explicit eager loading         |
| `FetchType.LAZY`      | Default behavior (loaded on access within transaction)    | Mind transaction scope         |

## Example Map

Source location: `src/test/kotlin/exposed/examples/jpa`

| Directory          | Files                                                     | Description                         |
|--------------------|-----------------------------------------------------------|-------------------------------------|
| `ex01_simple`      | `Ex01_Simple_DSL.kt`, `Ex02_Simple_DAO.kt`                | DSL/DAO basic CRUD comparison        |
| `ex02_entities`    | `Ex01_Blog.kt`, `Ex02_Person.kt`, `Ex03_Task.kt`          | Compound Entity relationship examples |
| `ex03_customId`    | `Ex01_CustomId.kt`                                        | Custom ID type definition            |
| `ex04_compositeId` | `Ex01_CompositeId.kt`, `Ex02_IdClass.kt`                  | Composite PK (`@EmbeddedId`, `@IdClass`) |
| `ex05_relations`   | One-to-One, One-to-Many, Many-to-One, Many-to-Many examples | Relationship mapping migration     |

## JPA Entity Mapping Diagrams

### Blog (One-to-One / One-to-Many / Many-to-Many)

![Blog ERD](src/test/kotlin/exposed/examples/jpa/ex02_entities/BlogSchema_ERD.png)

Example code: [`ex02_entities/Ex01_Blog.kt`](src/test/kotlin/exposed/examples/jpa/ex02_entities/Ex01_Blog.kt), [
`ex02_entities/BlogSchema.kt`](src/test/kotlin/exposed/examples/jpa/ex02_entities/BlogSchema.kt)

### Person-Address (Many-to-One)

![Person ERD](src/test/kotlin/exposed/examples/jpa/ex02_entities/PersonSchema.png)

Example code: [`ex02_entities/Ex02_Person.kt`](src/test/kotlin/exposed/examples/jpa/ex02_entities/Ex02_Person.kt)

### One-to-One

![One-to-One](src/test/kotlin/exposed/examples/jpa/ex05_relations/ex01_one_to_one/one-to-one.png)

Example code: [
`ex05_relations/ex01_one_to_one/Ex01_OneToOne_Unidirectional.kt`](src/test/kotlin/exposed/examples/jpa/ex05_relations/ex01_one_to_one/Ex01_OneToOne_Unidirectional.kt)

### One-to-Many

![Family/Order/Batch/Restaurant](src/test/kotlin/exposed/examples/jpa/ex05_relations/ex02_one_to_many/schema/FamilySchema.png)

Example code: [
`ex05_relations/ex02_one_to_many/Ex01_OneToMany_Bidirectional_Batch.kt`](src/test/kotlin/exposed/examples/jpa/ex05_relations/ex02_one_to_many/Ex01_OneToMany_Bidirectional_Batch.kt)

### Many-to-One

![Many-to-One](src/test/kotlin/exposed/examples/jpa/ex05_relations/ex03_many_to_one/ManyToOneSchema.png)

Example code: [
`ex05_relations/ex03_many_to_one/Ex01_ManyToOne.kt`](src/test/kotlin/exposed/examples/jpa/ex05_relations/ex03_many_to_one/Ex01_ManyToOne.kt)

### Many-to-Many

![Bank Many-to-Many](src/test/kotlin/exposed/examples/jpa/ex05_relations/ex04_many_to_many/BankSchema.png)

Example code: [
`ex05_relations/ex04_many_to_many/Ex01_ManyToMany_Bank.kt`](src/test/kotlin/exposed/examples/jpa/ex05_relations/ex04_many_to_many/Ex01_ManyToMany_Bank.kt)

## Running Tests

```bash
./gradlew :07-jpa:01-convert-jpa-basic:test
```

## Practice Checklist

- Compare JPA and Exposed implementations with the same test fixtures.
- Verify that exception messages and failure codes are compatible with the existing contract.

## Performance and Stability Checkpoints

- Confirm there is no query count regression on basic queries.
- Verify that transaction boundaries are identical to the original.

## Complex Scenarios

### 5 Relationship Mapping Types

| JPA Annotation | Exposed Implementation File |
|---|---|
| `@OneToOne` (unidirectional) | [`ex05_relations/ex01_one_to_one/Ex01_OneToOne_Unidirectional.kt`](src/test/kotlin/exposed/examples/jpa/ex05_relations/ex01_one_to_one/Ex01_OneToOne_Unidirectional.kt) |
| `@OneToOne` (bidirectional) | [`ex05_relations/ex01_one_to_one/Ex02_OneToOne_Bidirectional.kt`](src/test/kotlin/exposed/examples/jpa/ex05_relations/ex01_one_to_one/Ex02_OneToOne_Bidirectional.kt) |
| `@OneToMany` (batch/unidirectional) | [`ex05_relations/ex02_one_to_many/Ex01_OneToMany_Bidirectional_Batch.kt`](src/test/kotlin/exposed/examples/jpa/ex05_relations/ex02_one_to_many/Ex01_OneToMany_Bidirectional_Batch.kt) |
| `@ManyToOne` | [`ex05_relations/ex03_many_to_one/Ex01_ManyToOne.kt`](src/test/kotlin/exposed/examples/jpa/ex05_relations/ex03_many_to_one/Ex01_ManyToOne.kt) |
| `@ManyToMany` | [`ex05_relations/ex04_many_to_many/Ex01_ManyToMany_Bank.kt`](src/test/kotlin/exposed/examples/jpa/ex05_relations/ex04_many_to_many/Ex01_ManyToMany_Bank.kt) |

### CompositeId (Composite Primary Key)

- JPA `@EmbeddedId`: [
  `ex04_compositeId/Ex01_CompositeId.kt`](src/test/kotlin/exposed/examples/jpa/ex04_compositeId/Ex01_CompositeId.kt)
- JPA `@IdClass`: [
  `ex04_compositeId/Ex02_IdClass.kt`](src/test/kotlin/exposed/examples/jpa/ex04_compositeId/Ex02_IdClass.kt)

### Solving the N+1 Problem

In Exposed, N+1 is resolved using `load()` / `with()` or DSL JOIN queries.

- Order domain: [`ex05_relations/ex02_one_to_many/Ex03_OneToMany_N_plus_1_Order.kt`](src/test/kotlin/exposed/examples/jpa/ex05_relations/ex02_one_to_many/Ex03_OneToMany_N_plus_1_Order.kt)
- Restaurant domain: [`ex05_relations/ex02_one_to_many/Ex04_OneToMany_N_plus_1_Restaurant.kt`](src/test/kotlin/exposed/examples/jpa/ex05_relations/ex02_one_to_many/Ex04_OneToMany_N_plus_1_Restaurant.kt)

## Next Module

- [`../02-convert-jpa-advanced/README.md`](../02-convert-jpa-advanced/README.md)
