# 07 JPA Migration: Advanced Migration (02-convert-jpa-advanced)

English | [한국어](./README.ko.md)

A module for migrating advanced JPA features to Exposed, including complex relationships, inheritance/auditing, and locking strategies. Covers the most common performance and consistency risks encountered during migration.

## Learning Objectives

- Learn Exposed replacement strategies for advanced mappings and queries.
- Understand the key considerations when migrating optimistic locking and audit fields.
- Establish regression testing and performance measurement baselines.

## Prerequisites

- [`../01-convert-jpa-basic/README.md`](../01-convert-jpa-basic/README.md)

## Inheritance Mapping Strategy Comparison

| Strategy        | JPA Configuration               | Tables | Exposed Implementation                                   | Advantages                              | Disadvantages                           |
|-----------------|---------------------------------|--------|----------------------------------------------------------|-----------------------------------------|-----------------------------------------|
| Single Table    | `@Inheritance(SINGLE_TABLE)`    | 1      | Single `IntIdTable` + `dtype` column + nullable columns  | Fast queries without joins              | Many nullable columns, table bloat      |
| Joined Table    | `@Inheritance(JOINED)`          | 1+n    | Parent `IntIdTable` + child `IdTable` (FK=PK)            | Normalized schema, no column bloat      | Requires joins, multiple table writes   |
| Table Per Class | `@Inheritance(TABLE_PER_CLASS)` | n      | Independent `IntIdTable` per subtype                     | Table independence, single-table queries | Difficult cross-type queries, schema duplication |

## Exposed Implementation Patterns by Inheritance Strategy

### Single Table Inheritance

```kotlin
// Single table with dtype column to distinguish subtypes
object BillingTable : IntIdTable("billing") {
    val owner    = varchar("owner", 64).index()
    val swift    = varchar("swift", 16)
    val dtype    = enumerationByName<BillingType>("dtype", 32).default(BillingType.UNKNOWN)

    // CreditCard-only columns (nullable)
    val cardNumber  = varchar("card_number", 24).nullable()
    val expMonth    = integer("exp_month").nullable()
    val expYear     = integer("exp_year").nullable()

    // BankAccount-only columns (nullable)
    val accountNumber = varchar("account_number", 255).nullable()
    val bankName      = varchar("bank_name", 255).nullable()
}

// Query for subtype
BillingTable.selectAll()
    .where { BillingTable.dtype eq BillingType.CREDIT_CARD }
```

### Joined Table Inheritance

```kotlin
// Parent table
object PersonTable : IntIdTable("joined_person") {
    val name = varchar("person_name", 128)
    val ssn  = varchar("ssn", 128)
    init { uniqueIndex(name, ssn) }
}

// Child table — PK = FK to PersonTable
object EmployeeTable : IdTable<Int>("joined_employee") {
    override val id: Column<EntityID<Int>> = reference("id", PersonTable, onDelete = CASCADE)
    val empNo   = varchar("emp_no", 128)
    val empTitle = varchar("emp_title", 128)
    val managerId = reference("manager_id", EmployeeTable).nullable()  // self-reference
}

// JOIN required when querying
(PersonTable innerJoin EmployeeTable)
    .selectAll()
    .where { PersonTable.name eq "John" }
```

### Table Per Class Inheritance

```kotlin
// Independent table per subtype (common columns repeated)
object CreditCardTable : IntIdTable("credit_card") {
    val owner      = varchar("owner", 64)
    val cardNumber = varchar("card_number", 24)
    val expMonth   = integer("exp_month")
}

object BankAccountTable : IntIdTable("bank_account") {
    val owner         = varchar("owner", 64)
    val accountNumber = varchar("account_number", 255)
}

// UNION to query all
CreditCardTable.selectAll()
    .union(BankAccountTable.selectAll())
```

## Inheritance Strategy classDiagram

```mermaid
%%{init: {'theme': 'base', 'backgroundColor': '#FAFAFA', 'themeVariables': {'background': '#FAFAFA', 'fontFamily': '"Comic Mono", "goorm sans code", "JetBrains Mono", "goorm sans"'}}}%%
classDiagram
    class Billing {
        <<abstract>>
        +Int id
        +String owner
        +String swift
    }
    class CreditCard {
        +String cardNumber
        +Int expMonth
        +Int expYear
        +LocalDate startDate
        +LocalDate endDate
    }
    class BankAccount {
        +String accountNumber
        +String bankName
    }

    Billing <|-- CreditCard : 3 inheritance strategies applied
    Billing <|-- BankAccount : 3 inheritance strategies applied

    note for Billing "SingleTable: 1 BillingTable / Joined: billing + credit_card / TablePerClass: independent tables"

    style Billing fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style CreditCard fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style BankAccount fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
```

## Domain ERDs

### Single Table Inheritance ERD

```mermaid
%%{init: {'theme': 'base', 'backgroundColor': '#FAFAFA', 'themeVariables': {'background': '#FAFAFA', 'fontFamily': '"Comic Mono", "goorm sans code", "JetBrains Mono", "goorm sans"'}}}%%
erDiagram
    billing {
        SERIAL id PK
        VARCHAR_64 owner "NOT NULL"
        VARCHAR_16 swift "NOT NULL"
        VARCHAR_32 dtype "DEFAULT UNKNOWN"
        VARCHAR_24 card_number "NULL (CreditCard only)"
        INT exp_month "NULL (CreditCard only)"
        INT exp_year "NULL (CreditCard only)"
        DATE start_date "NULL (CreditCard only)"
        DATE end_date "NULL (CreditCard only)"
        VARCHAR_255 account_number "NULL (BankAccount only)"
        VARCHAR_255 bank_name "NULL (BankAccount only)"
    }
```

### Joined Table Inheritance ERD

```mermaid
%%{init: {'theme': 'base', 'backgroundColor': '#FAFAFA', 'themeVariables': {'background': '#FAFAFA', 'fontFamily': '"Comic Mono", "goorm sans code", "JetBrains Mono", "goorm sans"'}}}%%
erDiagram
    joined_person {
        SERIAL id PK
        VARCHAR_128 person_name "NOT NULL"
        VARCHAR_128 ssn "NOT NULL"
    }
    joined_employee {
        INT id PK "joined_person FK"
        VARCHAR_128 emp_no "NOT NULL"
        VARCHAR_128 emp_title "NOT NULL"
        INT manager_id FK "NULL (self-reference)"
    }

    joined_person ||--o| joined_employee : "1:1 Joined (id=FK)"
    joined_employee }o--o| joined_employee : "self-reference (manager_id)"
```

### Table Per Class Inheritance ERD

```mermaid
%%{init: {'theme': 'base', 'backgroundColor': '#FAFAFA', 'themeVariables': {'background': '#FAFAFA', 'fontFamily': '"Comic Mono", "goorm sans code", "JetBrains Mono", "goorm sans"'}}}%%
erDiagram
    credit_card {
        SERIAL id PK
        VARCHAR_64 owner "NOT NULL"
        VARCHAR_24 card_number "NOT NULL"
        INT exp_month "NOT NULL"
        INT exp_year "NOT NULL"
    }
    bank_account {
        SERIAL id PK
        VARCHAR_64 owner "NOT NULL"
        VARCHAR_255 account_number "NOT NULL"
        VARCHAR_255 bank_name "NOT NULL"
    }
```

### TreeNode ERD (Self-reference Tree Structure)

```mermaid
%%{init: {'theme': 'base', 'backgroundColor': '#FAFAFA', 'themeVariables': {'background': '#FAFAFA', 'fontFamily': '"Comic Mono", "goorm sans code", "JetBrains Mono", "goorm sans"'}}}%%
erDiagram
    tree_nodes {
        BIGINT id PK "autoIncrement"
        VARCHAR_255 title "NOT NULL"
        TEXT description "NULL"
        INT depth "DEFAULT 0"
        BIGINT parent_id FK "NULL (self-reference)"
    }

    tree_nodes }o--o| tree_nodes : "self-reference (parent_id)"
```

### Tree Structure Hierarchy Example

```mermaid
%%{init: {'theme': 'base', 'backgroundColor': '#FAFAFA', 'themeVariables': {'background': '#FAFAFA', 'fontFamily': '"Comic Mono", "goorm sans code", "JetBrains Mono", "goorm sans"'}}}%%
flowchart TD
    Root["root (depth=1)"]
    Child1["child1 (depth=2)"]
    Child2["child2 (depth=2)"]
    GrandChild1["grandChild1 (depth=3)"]
    GrandChild2["grandChild2 (depth=3)"]

    Root --> Child1
    Root --> Child2
    Child1 --> GrandChild1
    Child1 --> GrandChild2

    classDef blue fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef green fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef purple fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A

    class Root blue
    class Child1,Child2 green
    class GrandChild1,GrandChild2 purple
```

## Inheritance Strategy Comparison classDiagram

```mermaid
%%{init: {'theme': 'base', 'backgroundColor': '#FAFAFA', 'themeVariables': {'background': '#FAFAFA', 'fontFamily': '"Comic Mono", "goorm sans code", "JetBrains Mono", "goorm sans"'}}}%%
classDiagram
    direction TD
    class Billing {
        <<abstract common>>
        +Int id
        +String owner
        +String swift
    }
    class CreditCard_SingleTable {
        <<Single Table billing>>
        +String dtype
        +String cardNumber
        +Int expMonth
        +Int expYear
    }
    class BankAccount_SingleTable {
        <<Single Table billing>>
        +String dtype
        +String accountNumber
        +String bankName
    }
    class Person_Joined {
        <<Joined joined_person>>
        +Int id
        +String name
        +String ssn
    }
    class Employee_Joined {
        <<Joined joined_employee>>
        +Int id
        +String empNo
        +String empTitle
        +Employee manager
    }
    class CreditCard_TablePerClass {
        <<Table Per Class credit_card>>
        +Int id
        +String owner
        +String cardNumber
        +Int expMonth
        +Int expYear
    }
    class BankAccount_TablePerClass {
        <<Table Per Class: bank_account>>
        +Int id PK
        +String owner
        +String accountNumber
        +String bankName
    }

    Billing <|-- CreditCard_SingleTable
    Billing <|-- BankAccount_SingleTable
    Person_Joined <|-- Employee_Joined
    Employee_Joined --> Employee_Joined : manager self-reference
    Billing <|-- CreditCard_TablePerClass
    Billing <|-- BankAccount_TablePerClass

    style Billing fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style CreditCard_SingleTable fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style BankAccount_SingleTable fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style Person_Joined fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style Employee_Joined fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style CreditCard_TablePerClass fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    style BankAccount_TablePerClass fill:#FFF3E0,stroke:#FFCC80,color:#E65100
```

## Advanced Feature JPA ↔ Exposed Conversion Reference

| Feature              | JPA Implementation                                     | Exposed Implementation                                        |
|----------------------|--------------------------------------------------------|---------------------------------------------------------------|
| Auditable created-at | `@CreatedDate` + `@EntityListeners`                    | `EntityHook.subscribe` or `by Delegates.observable`           |
| Auditable updated-at | `@LastModifiedDate` + `@EntityListeners`               | Subscribe to `EntityHook` `EntityChangeType.Updated`          |
| Optimistic locking   | `@Version val version: Int`                            | Manual version column + `update where version = N`            |
| Subquery             | JPQL `SELECT x FROM X x WHERE x.id IN (...)`           | `inSubQuery` / `exists`                                       |
| Self-join (tree)     | `@ManyToOne self` + CTE                                | `alias()` + recursive CTE (`WITH RECURSIVE`)                  |
| Full JOIN            | `JOIN FETCH` (INNER/LEFT only)                         | `fullJoin` / `crossJoin`                                      |
| Covering index       | `@Index(columnList="...")` hint                        | `addIndex(customIndexName, col1, col2)`                       |

## Example Map

Source location: `src/test/kotlin/exposed/examples/jpa`

| Directory          | Files                                                                                                      | Description                     |
|--------------------|------------------------------------------------------------------------------------------------------------|---------------------------------|
| `ex01_joins`       | `Ex01_Simple_Join.kt` ~ `Ex07_Misc_Join.kt`                                                                | INNER/FULL/LEFT/RIGHT/SELF JOIN |
| `ex02_subquery`    | `Ex01_SubQuery.kt`                                                                                         | Correlated subqueries, EXISTS   |
| `ex03_inheritance` | `Ex01_SingleTable_Inheritance.kt`, `Ex02_Joined_Table_Inheritance.kt`, `Ex03_TablePerClass_Inheritance.kt` | 3 inheritance strategies        |
| `ex04_tree`        | `Ex01_TreeNode.kt`, `TreeNodeSchema.kt`                                                                    | Self-Reference + CTE            |
| `ex05_auditable`   | `Ex01_AuditableEntity.kt`, `AuditableEntity.kt`                                                            | Auto-managed created/updated timestamps |
| `ex06_cte`         | `Ex01_CTE.kt`                                                                                              | CTE (Common Table Expression)   |
| `ex07_version`     | `Ex01_Version.kt`                                                                                          | Optimistic locking (@Version)   |

## JPA Entity Mapping Diagrams

### Single Table Inheritance

![Single Table Inheritance](src/test/kotlin/exposed/examples/jpa/ex03_inheritance/Ex01_SingleTable_Inheritance_ERD.png)

Example code: [
`ex03_inheritance/Ex01_SingleTable_Inheritance.kt`](src/test/kotlin/exposed/examples/jpa/ex03_inheritance/Ex01_SingleTable_Inheritance.kt)

### Joined Table Inheritance

![Joined Table Inheritance](src/test/kotlin/exposed/examples/jpa/ex03_inheritance/Ex02_Joined_Table_Inheritance_ERD.png)

Example code: [
`ex03_inheritance/Ex02_Joined_Table_Inheritance.kt`](src/test/kotlin/exposed/examples/jpa/ex03_inheritance/Ex02_Joined_Table_Inheritance.kt)

### Table Per Class Inheritance

![Table Per Class Inheritance](src/test/kotlin/exposed/examples/jpa/ex03_inheritance/Ex03_TablePerClass_Inheritance_ERD.png)

Example code: [
`ex03_inheritance/Ex03_TablePerClass_Inheritance.kt`](src/test/kotlin/exposed/examples/jpa/ex03_inheritance/Ex03_TablePerClass_Inheritance.kt)

### Tree (Self-Reference)

![Tree Node Schema](src/test/kotlin/exposed/examples/jpa/ex04_tree/TreeNodeSchema.png)

Example code: [`ex04_tree/Ex01_TreeNode.kt`](src/test/kotlin/exposed/examples/jpa/ex04_tree/Ex01_TreeNode.kt)

## Running Tests

```bash
./gradlew :07-jpa:02-convert-jpa-advanced:test
```

## Practice Checklist

- Verify equivalence of complex query/sort/pagination results.
- Validate exception and retry policy on lock conflicts.

## Performance and Stability Checkpoints

- Remove lazy-loading-dependent code to prevent runtime errors.
- Track index/query plan regressions as CI metrics.

## Complex Scenarios

### 3 Inheritance Strategies

| JPA Strategy | Exposed Implementation File |
|---|---|
| `@Inheritance(SINGLE_TABLE)` | [`ex03_inheritance/Ex01_SingleTable_Inheritance.kt`](src/test/kotlin/exposed/examples/jpa/ex03_inheritance/Ex01_SingleTable_Inheritance.kt) |
| `@Inheritance(JOINED)` | [`ex03_inheritance/Ex02_Joined_Table_Inheritance.kt`](src/test/kotlin/exposed/examples/jpa/ex03_inheritance/Ex02_Joined_Table_Inheritance.kt) |
| `@Inheritance(TABLE_PER_CLASS)` | [`ex03_inheritance/Ex03_TablePerClass_Inheritance.kt`](src/test/kotlin/exposed/examples/jpa/ex03_inheritance/Ex03_TablePerClass_Inheritance.kt) |

### Subquery Patterns

- Correlated subquery / EXISTS subquery: [`ex02_subquery/Ex01_SubQuery.kt`](src/test/kotlin/exposed/examples/jpa/ex02_subquery/Ex01_SubQuery.kt)

### CTE (Common Table Expression)

- Exposed CTE API migration: [`ex04_tree/Ex01_TreeNode.kt`](src/test/kotlin/exposed/examples/jpa/ex04_tree/Ex01_TreeNode.kt)

### Auditing and Optimistic Locking

- `@CreatedDate/@LastModifiedDate`: [`ex05_auditable/Ex01_AuditableEntity.kt`](src/test/kotlin/exposed/examples/jpa/ex05_auditable/Ex01_AuditableEntity.kt)
- `@Version` optimistic locking: [`ex07_version/Ex01_Version.kt`](src/test/kotlin/exposed/examples/jpa/ex07_version/Ex01_Version.kt)

## Next Chapter

- [`../../08-coroutines/README.md`](../../08-coroutines/README.md)
