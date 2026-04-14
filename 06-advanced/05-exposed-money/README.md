# 06 Advanced: exposed-money (05)

English | [한국어](./README.ko.md)

A module for handling JavaMoney-based currency values as Exposed columns. Provides patterns for improving financial domain consistency by storing amounts and currencies together.

## Learning Objectives

- Understand the `compositeMoney` mapping structure.
- Learn patterns for storing and querying currency/amount simultaneously.
- Understand why precision types should be used instead of floating-point errors.

## Prerequisites

- [`../05-exposed-dml/02-types/README.md`](../05-exposed-dml/02-types/README.md)

## AccountTable ERD

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
erDiagram
    Accounts {
        SERIAL id PK
        DECIMAL composite_money "amount (precision=8, scale=5), nullable"
        VARCHAR composite_money_C "currency code (3 chars), nullable"
    }
```

## MonetaryAmount Type Mapping

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
classDiagram
    class AccountTable {
        +id: Column~EntityID~Int~~
        +composite_money: CompositeMoneyColumn~MonetaryAmount?~
    }
    class CompositeMoneyColumn {
        +amount: Column~BigDecimal?~
        +currency: Column~CurrencyUnit?~
    }
    class MonetaryAmount {
        <<javax.money>>
        +number: NumberValue
        +currency: CurrencyUnit
    }
    class BigDecimal {
        <<java.math>>
    }
    class CurrencyUnit {
        <<javax.money>>
        +currencyCode: String
    }
    class AccountEntity {
        +money: MonetaryAmount?
        +amount: BigDecimal?
        +currency: CurrencyUnit?
    }

    AccountTable --> CompositeMoneyColumn : compositeMoney()
    CompositeMoneyColumn --> BigDecimal : amount column
    CompositeMoneyColumn --> CurrencyUnit : currency column
    MonetaryAmount --> BigDecimal : number
    MonetaryAmount --> CurrencyUnit : currency
    AccountEntity --> AccountTable : maps to
    AccountEntity --> MonetaryAmount : reads/writes

    style AccountTable fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style CompositeMoneyColumn fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style MonetaryAmount fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style BigDecimal fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    style CurrencyUnit fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    style AccountEntity fill:#E0F2F1,stroke:#80CBC4,color:#00695C
```

## Key Concepts

- `MonetaryAmount` <-> composite column mapping
- Currency code-based filtering
- Default values / client defaults

## Example Files

| File                    | Description            |
|-------------------------|------------------------|
| `MoneyData.kt`          | Table/domain definitions |
| `Ex01_MoneyDefaults.kt` | Default value configuration |
| `Ex02_Money.kt`         | CRUD/queries           |

## How to Run

```bash
./gradlew :06-advanced:05-exposed-money:test
```

## Advanced Scenarios

### Currency Code Filtering

`compositeMoney` consists of two columns: amount (`amount`) and currency code (`currency`).
You can use the currency code column directly as a WHERE condition to query specific currencies.

- Related file: [`Ex02_Money.kt`](src/test/kotlin/exposed/examples/money/Ex02_Money.kt)
- Test: `filterByCurrencyCode` — Validates currency code column-based conditional query

### Digit Overflow Exception Handling

A DB exception occurs when inserting an amount exceeding the `precision`/`scale` range of a `BigDecimal` column.
This scenario is verified with `assertFailsWith`.

- Related file: [`Ex02_Money.kt`](src/test/kotlin/exposed/examples/money/Ex02_Money.kt)
- Test: `insertMoneyWithOverflow`

### compositeMoney Null Handling

The `compositeMoney` column supports the `nullable()` option.
Validates behavior when only one of amount or currency is null, and full null handling.

- Related file: [`Ex01_MoneyDefaults.kt`](src/test/kotlin/exposed/examples/money/Ex01_MoneyDefaults.kt)
- Test: `nullableCompositeMoney` — Validates null storage/retrieval consistency

## Practice Checklist

- Verify behavior when entering the same amount in different currencies.
- Confirm type precision during amount sorting/aggregation.

## Performance and Stability Checkpoints

- Use Decimal-based types instead of `Double/Float` for amounts
- Clearly separate exchange rate conversion responsibility (application/external service)

## Next Module

- [`../06-custom-columns/README.md`](../06-custom-columns/README.md)
