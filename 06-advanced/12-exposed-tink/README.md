# 12. Exposed Tink (Google Tink 기반 컬럼 암호화)

Google Tink 라이브러리를 활용하여 Exposed 컬럼의 데이터를 투명하게 암호화/복호화하는 방법을 학습합니다. AEAD(비결정적 암호화)와 DAEAD(결정적 암호화) 두 가지 암호화 방식을 지원하며, 암호화된 상태로 저장된 데이터에 대해 WHERE 절 검색까지 가능합니다.

## 학습 목표

- Google Tink의 AEAD와 DAEAD 차이를 이해한다.
- `bluetape4k-exposed-tink`가 제공하는 커스텀 컬럼 타입 확장 함수를 사용한다.
- DSL 방식과 DAO 방식 모두에서 암호화 컬럼을 활용한다.
- DAEAD(결정적 암호화)를 이용하여 암호문으로 WHERE 절 검색을 수행한다.

## Google Tink 암호화 방식 비교

| 방식        | 특징                    | WHERE 검색 | 권장 알고리즘                                         | 주요 용도                |
|-----------|-----------------------|----------|-------------------------------------------------|----------------------|
| **AEAD**  | 같은 평문이라도 매번 다른 암호문 생성 | 불가능      | `AES256_GCM`, `AES128_GCM`, `CHACHA20_POLY1305` | 패스워드, 개인정보           |
| **DAEAD** | 같은 평문은 항상 같은 암호문 생성   | 가능       | `AES256_SIV`                                    | 이메일, 주민번호 등 검색 필요 필드 |

### AEAD (Authenticated Encryption with Associated Data)

비결정적 암호화 방식으로, 동일한 평문을 암호화해도 매번 다른 암호문이 생성됩니다. 무결성 검증(인증) 기능이 포함되어 있어 데이터 변조를 감지할 수 있습니다.

**지원 알고리즘:**

- `TinkAeads.AES256_GCM` — 256비트 AES-GCM (기본 권장)
- `TinkAeads.AES128_GCM` — 128비트 AES-GCM
- `TinkAeads.CHACHA20_POLY1305` — ChaCha20-Poly1305

### DAEAD (Deterministic AEAD)

결정적 암호화 방식으로, 동일한 평문은 항상 동일한 암호문을 생성합니다. 암호화된 상태 그대로 WHERE 절 조건 검색이 가능합니다.

**지원 알고리즘:**

- `TinkDaeads.AES256_SIV` — AES-SIV (Synthetic IV) 방식

## 제공 컬럼 확장 함수

`io.bluetape4k.exposed.core.tink` 패키지의 확장 함수를 사용합니다.

### AEAD 컬럼

| 함수                                            | 컬럼 타입     | 설명                 |
|-----------------------------------------------|-----------|--------------------|
| `tinkAeadVarChar(name, length, keyTemplate?)` | `VARCHAR` | 문자열 AEAD 암호화 컬럼    |
| `tinkAeadBinary(name, length, keyTemplate?)`  | `BINARY`  | 바이트 배열 AEAD 암호화 컬럼 |
| `tinkAeadBlob(name, keyTemplate?)`            | `BLOB`    | BLOB AEAD 암호화 컬럼   |

### DAEAD 컬럼

| 함수                                             | 컬럼 타입     | 설명                          |
|------------------------------------------------|-----------|-----------------------------|
| `tinkDaeadVarChar(name, length, keyTemplate?)` | `VARCHAR` | 문자열 DAEAD 암호화 컬럼 (검색 가능)    |
| `tinkDaeadBinary(name, length, keyTemplate?)`  | `BINARY`  | 바이트 배열 DAEAD 암호화 컬럼 (검색 가능) |
| `tinkDaeadBlob(name, keyTemplate?)`            | `BLOB`    | BLOB DAEAD 암호화 컬럼 (검색 가능)   |

> `keyTemplate` 파라미터를 생략하면 각 방식의 기본 알고리즘이 사용됩니다.

## 주요 예제

### 1. AEAD 컬럼 정의 및 사용 (DSL 방식)

```kotlin
val secretTable = object: IntIdTable("tink_aead_table") {
    val secret = tinkAeadVarChar("secret", 512, TinkAeads.AES256_GCM).nullable()
    val data = tinkAeadBinary("data", 512, TinkAeads.AES256_GCM).nullable()
    val blob = tinkAeadBlob("blob", TinkAeads.AES256_GCM).nullable()
}

withTables(testDB, secretTable) {
    val id = secretTable.insertAndGetId {
        it[secret] = "민감한 문자열"
        it[data] = "바이트 데이터".toUtf8Bytes()
        it[blob] = "BLOB 데이터".toUtf8Bytes()
    }

    // 조회 시 자동으로 복호화됩니다.
    val row = secretTable.selectAll().where { secretTable.id eq id }.single()
    println(row[secretTable.secret])  // "민감한 문자열"
}
```

### 2. DAEAD 컬럼으로 WHERE 절 검색

```kotlin
val searchableTable = object: IntIdTable("tink_daead_table") {
    val email = tinkDaeadVarChar("email", 512, TinkDaeads.AES256_SIV).nullable().index()
}

withTables(testDB, searchableTable) {
    searchableTable.insertAndGetId {
        it[email] = "user@example.com"
    }

    // DAEAD는 결정적 암호화이므로 암호문으로 WHERE 검색이 가능합니다.
    // 내부적으로 평문을 암호화하여 비교합니다.
    searchableTable.selectAll()
        .where { searchableTable.email eq "user@example.com" }
        .count()  // 1L
}
```

### 3. AEAD Update

```kotlin
secretTable.update({ secretTable.id eq id }) {
    it[secret] = "변경된 문자열"
    it[data] = "변경된 데이터".toUtf8Bytes()
}
```

### 4. Nullable 컬럼에 null 저장

```kotlin
val nullableTable = object: IntIdTable("tink_nullable_table") {
    val aeadSecret = tinkAeadVarChar("aead_secret", 512).nullable()
    val daeadSecret = tinkDaeadVarChar("daead_secret", 512).nullable()
}

val id = nullableTable.insertAndGetId {
    it[aeadSecret] = null
    it[daeadSecret] = null
}
// null 값도 정상적으로 저장/조회됩니다.
```

### 5. 다양한 알고리즘 사용

```kotlin
val multiAlgoTable = object: IntIdTable("tink_multi_algo_table") {
    val aes256 = tinkAeadVarChar("aes256", 512, TinkAeads.AES256_GCM)
    val aes128 = tinkAeadVarChar("aes128", 512, TinkAeads.AES128_GCM)
    val chacha20 = tinkAeadVarChar("chacha20", 512, TinkAeads.CHACHA20_POLY1305)
}
```

### 6. DAO 방식

```kotlin
object T1: IntIdTable() {
    val secret = tinkDaeadVarChar("secret", 255, TinkDaeads.AES256_SIV).index()
    val data = tinkDaeadBinary("data", 512, TinkDaeads.AES256_SIV)
}

class E1(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<E1>(T1)

    var secret by T1.secret
    var data by T1.data
}

// DAO로 저장하면 자동으로 암호화됩니다.
val entity = E1.new {
    secret = "홍길동"
    data = "서울시 강남구".toUtf8Bytes()
}

// 조회 시 자동으로 복호화됩니다.
val saved = E1.findById(entity.id)!!
println(saved.secret)  // "홍길동"

// DAEAD 컬럼으로 DAO 검색도 가능합니다.
E1.find { T1.secret eq "홍길동" }.single()
```

## 알려진 제약 사항

- **컬럼 길이는 반드시 0보다 커야 합니다.** `tinkAeadVarChar("col", 0)` 등은 `IllegalArgumentException`을 발생시킵니다.
- **AEAD 컬럼은 WHERE 절 검색이 불가능합니다.** 같은 평문도 암호화할 때마다 다른 값이 생성되기 때문입니다.
- **DAO 이중 복호화 버그 (Exposed 1.1.0, 1.1.1):
  ** DAO 방식으로 Binary 컬럼을 WHERE 조건으로 조회할 때 복호화가 이중으로 호출되는 버그가 있습니다. DSL 방식으로 우회하세요.

## 테스트 파일

| 파일                         | 설명                                 |
|----------------------------|------------------------------------|
| `TinkColumnTypeTest.kt`    | DSL 방식 AEAD/DAEAD 컬럼 CRUD 및 검색 테스트 |
| `TinkColumnTypeDaoTest.kt` | DAO 방식 DAEAD 컬럼 저장/조회/검색 테스트       |

## 실행 방법

```bash
# 전체 테스트 실행
./gradlew :06-advanced:12-exposed-tink:test

# H2만 대상으로 빠른 테스트
./gradlew :06-advanced:12-exposed-tink:test -PuseFastDB=true
```

## Jasypt 연동 모듈과 비교

| 항목       | Jasypt (`10-exposed-jasypt`) | Google Tink (`12-exposed-tink`) |
|----------|------------------------------|---------------------------------|
| 암호화 방식   | 대칭 키 (PBE 기반)                | AEAD / DAEAD                    |
| 결정적 암호화  | 기본 동작 (같은 암호문)               | DAEAD 방식으로 선택 가능                |
| WHERE 검색 | 가능                           | DAEAD 컬럼만 가능                    |
| 무결성 검증   | 없음                           | 있음 (GCM 인증 태그)                  |
| 표준화      | Java 생태계                     | Google 오픈소스 암호화 표준              |

## 다음 단계

- [07-jpa](../../07-jpa/README.md): JPA 코드를 Exposed로 마이그레이션하는 실전 패턴을 학습합니다.
