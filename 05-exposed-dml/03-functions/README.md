# 05 Exposed DML: SQL Functions (03-functions)

English | [한국어](./README.ko.md)

A module for writing analytical queries by combining SQL functions in the Exposed DSL. Focuses on string, math, statistical, and window functions with practical patterns.

## Learning Objectives

- Write expression-based queries using the Exposed function API.
- Implement analytical queries with aggregation and window functions.
- Manage per-DB function support differences with tests.

## Prerequisites

- [`../01-dml/README.md`](../01-dml/README.md)
- [`../02-types/README.md`](../02-types/README.md)

## SQL Function Classification Diagram

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
flowchart TD
    F["Exposed SQL Functions"] --> STR["String Functions\ntrim / lowerCase / upperCase\nsubstring / concat / like / ilike"]
    F --> MATH["Math Functions\nround / abs / floor / ceiling\nsqrt / power"]
    F --> AGG["Aggregate Functions\ncount / sum / avg / min / max\ngroupBy + having"]
    F --> STAT["Statistical Functions\nstdDevPop / stdDevSamp\nvarPop / varSamp"]
    F --> TRIG["Trigonometric Functions\nsin / cos / tan\natan / atan2"]
    F --> WIN["Window Functions\nrowNumber / rank / denseRank\nlead / lag / firstValue / lastValue"]
    F --> COND["Conditional Functions\ncase / coalesce / nullIf"]
    F --> BIT["Bitwise Functions\nbitwiseAnd / bitwiseOr / bitwiseXor"]

    WIN --> OVER["over()\n.partitionBy(col)\n.orderBy(col, SortOrder)"]

    classDef purple fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    classDef blue fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef green fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef orange fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    classDef pink fill:#FCE4EC,stroke:#F48FB1,color:#AD1457
    classDef teal fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    classDef yellow fill:#FFFDE7,stroke:#FFF176,color:#F57F17
    classDef red fill:#FFEBEE,stroke:#EF9A9A,color:#C62828

    class F purple
    class STR blue
    class MATH green
    class AGG orange
    class STAT pink
    class TRIG teal
    class WIN yellow
    class COND red
    class BIT teal
    class OVER yellow
```

## Key Concepts

### String Functions

```kotlin
// trim, lowerCase, upperCase, substring, concat
Users.select(
    Users.name.trim(),
    Users.name.lowerCase(),
    concat(Users.firstName, stringLiteral(" "), Users.lastName)
)

// coalesce — null fallback value
Users.select(coalesce(Users.nickname, Users.name))
```

### Math Functions

```kotlin
// round, abs, floor, ceiling
Products.select(
    Products.price.round(2),
    Products.price.abs()
)
```

### Aggregate Functions

```kotlin
// count, sum, avg, min, max + groupBy + having
Orders
    .select(Orders.customerId, Orders.amount.sum())
    .groupBy(Orders.customerId)
    .having { Orders.amount.sum() greater 1000.toBigDecimal() }
```

### Window Functions

```kotlin
// rowNumber, rank, denseRank, lead, lag
val rowNum = rowNumber().over().partitionBy(Sales.region).orderBy(Sales.amount, SortOrder.DESC)
val rankVal = rank().over().orderBy(Sales.amount, SortOrder.DESC)

Sales.select(Sales.region, Sales.amount, rowNum, rankVal)
```

## Function Reference Table

| Category    | Functions                                                                    | Notes                        |
|-------------|------------------------------------------------------------------------------|------------------------------|
| String      | `trim`, `lowerCase`, `upperCase`, `substring`, `concat`, `like`, `ilike`    | `ilike`: PostgreSQL only     |
| Math        | `round`, `abs`, `floor`, `ceiling`, `sqrt`, `power`                         |                              |
| Aggregate   | `count`, `sum`, `avg`, `min`, `max`                                         | Combined with `groupBy` / `having` |
| Statistical | `stdDevPop`, `stdDevSamp`, `varPop`, `varSamp`                              | Per-DB support differences   |
| Trig        | `sin`, `cos`, `tan`, `atan`, `atan2`                                        | Per-DB support differences   |
| Window      | `rowNumber`, `rank`, `denseRank`, `lead`, `lag`, `firstValue`, `lastValue`  | Combined with `OVER()` clause |
| Conditional | `case`, `coalesce`, `nullIf`                                                |                              |
| Bitwise     | `bitwiseAnd`, `bitwiseOr`, `bitwiseXor`                                     |                              |

## Window Function Structure

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
flowchart LR
    A["Window Function\n(rowNumber / rank / lead / lag)"] --> B["over()"]
    B --> C["partitionBy(column)"]
    B --> D["orderBy(column, SortOrder)"]
    C --> E["PARTITION BY clause"]
    D --> F["ORDER BY clause"]
    E --> G["SQL: OVER(PARTITION BY ... ORDER BY ...)"]
    F --> G

    classDef purple fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    classDef blue fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef green fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef orange fill:#FFF3E0,stroke:#FFCC80,color:#E65100

    class A purple
    class B blue
    class C,D green
    class E,F green
    class G orange
```

## Example Map

Source location: `src/test/kotlin/exposed/examples/functions`

| File                                | Description                        |
|-------------------------------------|------------------------------------|
| `Ex00_FunctionBase.kt`              | Common table/data setup            |
| `Ex01_Functions.kt`                 | String and basic functions         |
| `Ex02_MathFunction.kt`              | Math functions                     |
| `Ex03_StatisticsFunction.kt`        | Aggregate and statistical functions |
| `Ex04_TrigonometricalFunction.kt`   | Trigonometric functions            |
| `Ex05_WindowFunction.kt`            | Window functions                   |

## Running Tests

```bash
./gradlew :05-exposed-dml:03-functions:test
```

## Practice Checklist

- Rewrite the same aggregation using `groupBy + having` combinations.
- Compare window function results (rank, lead/lag values) across different sort orders.
- Verify the result type when chaining functions (e.g., string normalization → aggregation).

## Per-DB Notes

- Function names/signatures may have subtle differences per DB; Dialect-specific tests are necessary
- Check DB support for case-insensitive search functions like `ilike`

## Performance and Stability Checkpoints

- Index presence on sort/partition columns has a significant performance impact on aggregate/window functions
- Separate expressions into smaller pieces as query complexity grows to maintain readability

## Complex Scenarios

### Window Function OVER Clause Combinations

Combine `rowNumber`, `rank`, `denseRank`, `lead`, `lag`, `sum`, `avg`, etc. with `PARTITION BY` / `ORDER BY` to write rank and cumulative sum queries.

- Source: [`Ex05_WindowFunction.kt`](src/test/kotlin/exposed/examples/functions/Ex05_WindowFunction.kt)

### String, Bitwise, and Conditional Function Chains

Shows patterns for building expression-based queries by chaining `concat`, `substring`, `lowerCase`, `coalesce`, `case`, `bitwiseAnd`, etc. in DSL.

- Source: [`Ex01_Functions.kt`](src/test/kotlin/exposed/examples/functions/Ex01_Functions.kt)

### Aggregate and Statistical Functions with groupBy/having

Write analytical queries by combining `count`, `sum`, `avg`, `min`, `max` with `groupBy` + `having`.

- Source: [`Ex03_StatisticsFunction.kt`](src/test/kotlin/exposed/examples/functions/Ex03_StatisticsFunction.kt)

## Next Module

- [`../04-transactions/README.md`](../04-transactions/README.md)
