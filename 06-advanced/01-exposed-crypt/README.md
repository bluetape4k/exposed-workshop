# Exposed-Crypt: 투명한 컬럼 암호화

이 모듈은
`exposed-crypt` 확장을 사용하여 데이터베이스 컬럼의 데이터를 투명하게 암호화하고 복호화하는 방법을 단계별로 학습합니다. 개인정보, 비밀, 금융 데이터와 같은 민감한 정보를 저장 시 보호하는 데 활용합니다.

## 학습 목표

- 암호화된 컬럼을 정의하고 사용하는 방법 이해
- 다양한 암호화 알고리즘(`AES`, `Blowfish`, `Triple DES`) 사용법 학습
- DSL과 DAO 스타일 모두에서 암호화된 컬럼 적용
- 암호화된 컬럼 검색의 제한 사항과 대안 파악

## 예제 구성

모든 예제는 `src/test/kotlin/exposed/examples/crypt` 아래에 있습니다.

| 파일                                  | 설명             | 핵심 기능                                 |
|-------------------------------------|----------------|---------------------------------------|
| `Ex01_EncryptedColumn.kt`           | DSL 스타일 암호화 컬럼 | `encryptedVarchar`, `encryptedBinary` |
| `Ex02_EncryptedColumnWithEntity.kt` | DAO 스타일 암호화 컬럼 | Entity 프로퍼티로 암호화 필드 사용                |

## 핵심 개념

### 암호화 vs 결정적 암호화

| 구분       | 일반 암호화 (exposed-crypt) | 결정적 암호화 (exposed-jasypt) |
|----------|------------------------|--------------------------|
| 동일 평문 결과 | 매번 다른 암호문              | 항상 동일한 암호문               |
| 검색 가능    | 불가능                    | 가능 (`WHERE` 절 사용)        |
| 보안 강도    | 높음 (IV 사용)             | 낮음 (패턴 분석 가능)            |
| 사용 사례    | 비밀번호, 개인정보             | 주민번호, 계좌번호 (검색 필요)       |

## 지원 암호화 알고리즘

| 알고리즘              | 키 길이    | 특징                |
|-------------------|---------|-------------------|
| `AES_256_PBE_CBC` | 256-bit | AES-CBC 모드, 높은 보안 |
| `AES_256_PBE_GCM` | 256-bit | AES-GCM 모드, 인증 포함 |
| `BLOW_FISH`       | 가변      | 빠른 속도, 적당한 보안     |
| `TRIPLE_DES`      | 168-bit | 레거시 호환, 느린 속도     |

## 코드 스니펫

### 1. DSL 스타일 암호화 컬럼

```kotlin
import org.jetbrains.exposed.v1.crypt.encryptedVarchar
import org.jetbrains.exposed.v1.crypt.Algorithms

// 암호화기 정의
val nameEncryptor = Algorithms.AES_256_PBE_CBC(
    password = "my-secret-password",
    salt = "5c0744940b5c369b"
)

// 암호화된 컬럼이 있는 테이블
object SecureUsers : IntIdTable("secure_users") {
    val name = encryptedVarchar("name", 80, nameEncryptor)
    val email = encryptedVarchar(
        "email", 
        80, 
        Algorithms.AES_256_PBE_GCM("password", "salt123")
    )
    val secretData = encryptedBinary(
        "secret_data",
        256,
        Algorithms.BLOW_FISH("encryption-key")
    )
}

// 사용법
transaction {
    // 암호화하여 저장 (자동)
    SecureUsers.insert {
        it[name] = "John Doe"
        it[email] = "john@example.com"
        it[secretData] = "secret".toByteArray()
    }
    
    // 복호화하여 조회 (자동)
    val user = SecureUsers.selectAll().single()
    println(user[SecureUsers.name])  // "John Doe"
    
    // 주의: 암호화된 컬럼으로 검색 불가
    // SecureUsers.select { SecureUsers.name eq "John Doe" }  // 작동 안함!
}
```

### 2. DAO 스타일 암호화 컬럼

```kotlin
object Accounts : IntIdTable("accounts") {
    private val encryptor = Algorithms.AES_256_PBE_GCM(
        password = "secure-password",
        salt = "unique-salt-value"
    )
    
    val username = varchar("username", 50)
    val password = encryptedVarchar("password", 256, encryptor)
    val apiKey = encryptedBinary("api_key", 512, encryptor)
}

class Account(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Account>(Accounts)
    
    var username by Accounts.username
    var password by Accounts.password
    var apiKey by Accounts.apiKey
}

// 사용법
transaction {
    // 생성
    val account = Account.new {
        username = "john"
        password = "my-secret-password"  // 자동 암호화
        apiKey = "api-key-12345".toByteArray()
    }
    
    // 조회
    val found = Account.findById(account.id)
    println(found?.password)  // "my-secret-password" (자동 복호화)
    
    // 수정
    account.password = "new-password"  // 자동 암호화
}
```

### 3. 다양한 알고리즘 사용

```kotlin
object MultiEncryptedTable : IntIdTable("multi_encrypted") {
    // AES-256 CBC
    val aesField = encryptedVarchar(
        "aes_field", 
        256, 
        Algorithms.AES_256_PBE_CBC("pass1", "salt1")
    )
    
    // AES-256 GCM (인증 포함)
    val gcmField = encryptedVarchar(
        "gcm_field", 
        256, 
        Algorithms.AES_256_PBE_GCM("pass2", "salt2")
    )
    
    // Blowfish (빠른 속도)
    val blowfishField = encryptedVarchar(
        "blowfish_field", 
        256, 
        Algorithms.BLOW_FISH("blowfish-key")
    )
    
    // Triple DES (레거시)
    val tripleDesField = encryptedBinary(
        "tripledes_field", 
        256, 
        Algorithms.TRIPLE_DES("tripledes-key")
    )
}
```

### 4. 커스텀 암호화기

```kotlin
import org.jetbrains.exposed.v1.crypt.Encryptor

class CustomEncryptor : Encryptor {
    override fun encrypt(data: ByteArray): ByteArray {
        // 커스텀 암호화 로직
        return customEncryptFunction(data)
    }
    
    override fun decrypt(data: ByteArray): ByteArray {
        // 커스텀 복호화 로직
        return customDecryptFunction(data)
    }
}

// 사용
object CustomTable : IntIdTable("custom_encrypted") {
    val data = encryptedVarchar("data", 256, CustomEncryptor())
}
```

## 보안 모범 사례

### 1. 키 관리

```kotlin
// 좋지 않은 예 (하드코딩)
val encryptor = Algorithms.AES_256_PBE_GCM("hardcoded-password", "salt")

// 권장 예 (환경 변수)
val encryptor = Algorithms.AES_256_PBE_GCM(
    password = System.getenv("ENCRYPTION_PASSWORD"),
    salt = System.getenv("ENCRYPTION_SALT")
)

// 권장 예 (Vault 등 시크릿 매니저)
val encryptor = Algorithms.AES_256_PBE_GCM(
    password = secretManager.getSecret("db-encryption-password"),
    salt = secretManager.getSecret("db-encryption-salt")
)
```

### 2. 알고리즘 선택 가이드

| 사용 사례      | 권장 알고리즘         | 이유           |
|------------|-----------------|--------------|
| 개인정보 (PII) | AES_256_PBE_GCM | 높은 보안, 인증 포함 |
| 비밀번호       | AES_256_PBE_CBC | 높은 보안        |
| 대용량 데이터    | BLOW_FISH       | 빠른 속도        |
| 레거시 시스템 호환 | TRIPLE_DES      | 호환성          |

### 3. Salt 관리

```kotlin
// 각 환경별로 다른 Salt 사용
val salt = when (environment) {
    "prod" -> "production-salt-value"
    "staging" -> "staging-salt-value"
    else -> "development-salt-value"
}
```

## 제한 사항

1. **검색 불가**: 암호화된 컬럼은 `WHERE` 절에서 직접 검색할 수 없음
2. **정렬 불가**: 암호화된 값으로 정렬하면 의미 없는 결과
3. **인덱스 무효**: 암호화된 컬럼에 인덱스를 생성해도 검색 성능 향상 없음

### 검색이 필요한 경우

```kotlin
// 검색 가능한 암호화가 필요하면 Jasypt 사용 (10-exposed-jasypt 모듈)
// 또는 별도의 검색용 해시 컬럼 추가
object SecureData : IntIdTable("secure_data") {
    val encryptedValue = encryptedVarchar("encrypted_value", 256, encryptor)
    val searchHash = varchar("search_hash", 64)  // SHA-256 해시
}

// 검색 시
val searchHash = sha256("search-value")
SecureData.select { SecureData.searchHash eq searchHash }
```

## 테스트 실행

```bash
# 전체 테스트 실행
./gradlew :06-advanced:01-exposed-crypt:test

# 특정 테스트만 실행
./gradlew :06-advanced:01-exposed-crypt:test --tests "exposed.examples.crypt.Ex01_EncryptedColumn"
```

## 더 읽어보기

- [Exposed Crypt 모듈](https://debop.notion.site/Exposed-Crypt-1c32744526b0802da419d5ce74d2c5f3)
- [검색 가능한 암호화 (Jasypt)](../10-exposed-jasypt/README.md)
