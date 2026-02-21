# Exposed-Money: 통화 값 처리

이 모듈은 `exposed-money` 확장을 사용하여 JavaMoney(`javax.money`) API와 통합하여 통화 값을 안전하고 구조화된 방식으로 다루는 방법을 단계별로 학습합니다. 금융 계산에
`Double`이나 `Float`를 사용할 때 발생하는 문제를 해결하고, 통화 정보가 항상 금액과 함께 제공되도록 보장합니다.

## 학습 목표

- `compositeMoney`를 사용하여 통화 컬럼 정의 방법 이해
- 단일 `MonetaryAmount` 객체가 금액과 통화 컬럼에 매핑되는 방식 학습
- DSL과 DAO 스타일 모두에서 통화 데이터 CRUD 수행
- 통화 컬럼의 구성 요소(금액, 통화)로 쿼리하는 방법 습득
- 통화 컬럼의 기본값 설정 방법 익히기

## 예제 구성

모든 예제는 `src/test/kotlin/exposed/examples/money` 아래에 있습니다.

| 파일                      | 설명              | 핵심 기능                          |
|-------------------------|-----------------|--------------------------------|
| `MoneyData.kt`          | 테이블 및 Entity 정의 | `compositeMoney`, DAO 매핑       |
| `Ex01_MoneyDefaults.kt` | 기본값 설정          | `default()`, `clientDefault()` |
| `Ex02_Money.kt`         | CRUD 및 쿼리       | 삽입, 조회, 필터링, 복합 컬럼             |

## 핵심 개념

### compositeMoney 구조

```
Kotlin 코드                    데이터베이스
┌─────────────────────┐      ┌─────────────────────┐
│ MonetaryAmount      │      │ amount (DECIMAL)    │
│ - amount: BigDecimal│ ───► │ currency_C (VARCHAR)│
│ - currency: Currency │      └─────────────────────┘
└─────────────────────┘
```

- `compositeMoney`는 하나의 논리적 프로퍼티지만, 데이터베이스에는 두 개의 컬럼으로 저장됩니다
- 컬럼 이름: `columnName` (금액), `columnName_C` (통화)

## 코드 스니펫

### 1. 통화 컬럼 정의

```kotlin
import org.jetbrains.exposed.v1.money.compositeMoney
import javax.money.MonetaryAmount

object Accounts : IntIdTable("accounts") {
    val name = varchar("name", 255)
    
    // 통화 컬럼 (8자리, 소수점 2자리)
    // DB: balance (DECIMAL), balance_C (VARCHAR)
    val balance = compositeMoney(8, 2, "balance").nullable()
    
    // 기본값 있는 통화 컬럼
    val creditLimit = compositeMoney(10, 2, "credit_limit")
        .default(Money.of(10000, "USD"))
}
```

### 2. DAO Entity 정의

```kotlin
import org.javamoney.moneta.Money
import javax.money.MonetaryAmount
import javax.money.CurrencyUnit

class Account(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Account>(Accounts)
    
    var name by Accounts.name
    
    // 전체 MonetaryAmount
    var balance by Accounts.balance
    
    // 개별 접근 (읽기 전용)
    val balanceAmount by Accounts.balance.amount
    val balanceCurrency by Accounts.balance.currency
}
```

### 3. DSL 스타일 CRUD

```kotlin
import org.javamoney.moneta.Money

transaction {
    // 삽입 - 전체 객체
    val accountId = Accounts.insertAndGetId {
        it[name] = "John's Account"
        it[balance] = Money.of(1500.50, "USD")
    }
    
    // 삽입 - 개별 필드
    Accounts.insert {
        it[name] = "Jane's Account"
        it[Accounts.balance.amount] = BigDecimal("2500.00")
        it[Accounts.balance.currency] = Monetary.getCurrency("EUR")
    }
    
    // 조회
    val account = Accounts.selectAll().where { Accounts.id eq accountId }.single()
    val balance = account[Accounts.balance]  // MonetaryAmount
    
    // 수정
    Accounts.update({ Accounts.id eq accountId }) {
        it[balance] = balance.add(Money.of(500, "USD"))
    }
}
```

### 4. DAO 스타일 CRUD

```kotlin
transaction {
    // 생성
    val account = Account.new {
        name = "Savings Account"
        balance = Money.of(10000, "KRW")
    }
    
    // 조회
    val found = Account.findById(account.id)
    println("Balance: ${found?.balance}")  // "KRW 10000"
    println("Amount: ${found?.balanceAmount}")  // 10000
    println("Currency: ${found?.balanceCurrency}")  // KRW
    
    // 수정
    account.balance = account.balance.subtract(Money.of(1000, "KRW"))
}
```

### 5. 쿼리 - 금액으로 필터링

```kotlin
transaction {
    // 금액으로 조회
    Accounts.selectAll().where { 
        Accounts.balance.amount greater BigDecimal("1000") 
    }
    
    // 전체 객체로 조회
    val targetAmount = Money.of(1500.50, "USD")
    Accounts.selectAll().where { 
        Accounts.balance eq targetAmount 
    }
    
    // 통화로 조회
    Accounts.selectAll().where { 
        Accounts.balance.currency eq Monetary.getCurrency("USD") 
    }
    
    // 복합 조건
    Accounts.selectAll().where {
        (Accounts.balance.amount greater BigDecimal("1000")) and
        (Accounts.balance.currency eq Monetary.getCurrency("USD"))
    }
}
```

### 6. 기본값 설정

```kotlin
object Products : IntIdTable("products") {
    val name = varchar("name", 255)
    
    // 상수 기본값
    val price = compositeMoney(10, 2, "price")
        .default(Money.of(0, "USD"))
    
    // 클라이언트 기본값
    val listPrice = compositeMoney(10, 2, "list_price")
        .clientDefault { Money.of(99.99, "USD") }
}
```

### 7. 수동 복합 컬럼 생성

```kotlin
// 기존 컬럼으로부터 복합 컬럼 생성
object Orders : IntIdTable("orders") {
    val amount = decimal("amount", 10, 2)
    val currencyCode = varchar("currency_code", 3)
    
    // 복합 컬럼으로 래핑
    val totalAmount = compositeMoneyFrom(amount, currencyCode)
}
```

## 통화 연산

```kotlin
import javax.money.Monetary

transaction {
    val account = Account.findById(1) ?: return@transaction
    
    // 덧셈
    account.balance = account.balance.add(Money.of(100, "USD"))
    
    // 뺄셈
    account.balance = account.balance.subtract(Money.of(50, "USD"))
    
    // 곱셈
    account.balance = account.balance.multiply(1.1)  // 10% 증가
    
    // 나눗셈
    account.balance = account.balance.divide(2)
    
    // 통화 변환
    val eurAmount = Monetary.getDefaultRounding()
        .adjust(account.balance.with(Monetary.getCurrency("EUR")))
    
    // 비교
    val isGreaterThanZero = account.balance.isPositive
    val isZero = account.balance.isZero
}
```

## 지원 통화

```kotlin
// 주요 통화
val usd = Monetary.getCurrency("USD")  // 미국 달러
val eur = Monetary.getCurrency("EUR")  // 유로
val krw = Monetary.getCurrency("KRW")  // 한국 원
val jpy = Monetary.getCurrency("JPY")  // 일본 엔
val gbp = Monetary.getCurrency("GBP")  // 영국 파운드

// 통화 확인
Monetary.isCurrencyAvailable("BTC")  // false (기본적으로)
```

## 모범 사례

### 1. 정밀도 선택

| 사용 사례  | 권장 정밀도  |
|--------|---------|
| 일반 금액  | (10, 2) |
| 고정밀 금액 | (15, 4) |
| 암호화폐   | (20, 8) |
| 환율     | (12, 6) |

### 2. 통화 일관성

```kotlin
// 같은 통화끼리만 연산
val usd100 = Money.of(100, "USD")
val eur50 = Money.of(50, "EUR")

// usd100.add(eur50)  // 오류 발생!
// 변환 후 연산
val eurInUsd = eur50.with(Monetary.getCurrency("USD"))
val total = usd100.add(eurInUsd)
```

## 테스트 실행

```bash
# 전체 테스트 실행
./gradlew :06-advanced:05-exposed-money:test

# 특정 테스트만 실행
./gradlew :06-advanced:05-exposed-money:test --tests "exposed.examples.money.Ex02_Money"
```

## 더 읽어보기

- [Exposed Money](https://debop.notion.site/Exposed-Money-1c32744526b08051a216d87ca750d73f)
- [JavaMoney 문서](https://javamoney.github.io/)
