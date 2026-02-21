# Exposed-Jasypt: Jasypt를 이용한 결정적 암호화

이 모듈은 **Jasypt(Java Simplified Encryption)** 라이브러리를 Exposed와 통합하여 투명한 컬럼 암호화와 복호화를 제공하는 방법을 보여줍니다. 이 통합의 핵심 기능은 **결정적
** 특성으로, 동일한 평문 입력이 항상 동일한 암호문 출력을 생성한다는 것입니다. 이 속성은 암호화된 데이터를 `WHERE` 절에서 동등성 검사에 직접 사용할 수 있게 해주므로 매우 중요합니다.

이 모듈은 암호화된 데이터를 직접 쿼리할 수 없는 비결정적 암호화 방식(기본 `exposed-crypt` 모듈 등)의 한계를 해결합니다.

## 학습 목표

- Jasypt 암호화 컬럼(`jasyptVarChar`, `jasyptBinary`) 정의 방법 이해
- 문자열 및 바이너리 데이터의 투명한 암호화 및 복호화 수행
- 결정적 암호화를 활용하여 `WHERE` 절에서 암호화된 컬럼 직접 쿼리
- DSL과 DAO 프로그래밍 스타일 모두에 Jasypt 암호화 컬럼 적용

## 핵심 개념

### 결정적 암호화

동일한 평문에 대해 다른 암호문을 생성하기 위해 무작위성(솔트, IV)을 추가하는 많은 표준 암호화 접근 방식과 달리, Jasypt는 일관된 암호문을 생성하도록 구성할 수 있습니다. 이를 통해 SQL 동등성 비교(
`WHERE encrypted_column = 'encrypted_value'`)가 올바르게 작동합니다.

**트레이드오프
**: 검색 가능성을 가능하게 하지만, 결정적 암호화는 반복되는 데이터의 패턴을 악용하는 공격에 대해 더 낮은 암호화 강도를 제공합니다. 검색 가능성이 엄격한 요구 사항이고 데이터의 민감도가 이러한 트레이드오프를 허용하는 시나리오에 적합합니다.

### 컬럼 타입

- **`jasyptVarChar(name, length, encryptor)`**: Jasypt를 사용하여 `String` 값을 암호화하는 컬럼을 정의합니다.
- **`jasyptBinary(name, length, encryptor)`**: Jasypt를 사용하여 `ByteArray` 값을 암호화하는 컬럼을 정의합니다.

### `Encryptors`

`Encryptors` 열거형(예: `Encryptors.AES`, `Encryptors.RC4`)은 암호화 알고리즘을 지정하며 Jasypt에 대한 키 관리 설정을 암시적으로 처리합니다.

## 예제 개요

### `JasyptColumnTypeTest.kt` (DSL 스타일)

이 파일은 Exposed DSL 내에서 Jasypt 암호화 컬럼의 사용법을 보여줍니다.

- **CRUD 연산**: 암호화된 `String`과 `ByteArray` 필드를 `insert`하고 `update`하는 방법을 보여줍니다. 암호화/복호화는 투명합니다.
- **검색 가능성**: 암호화된 컬럼을 `WHERE` 절의 `eq` 비교에 사용할 수 있음을 명시적으로 강조하며, 이는 결정적 암호화의 주요 장점입니다.

### `JasyptColumnTypeDaoTest.kt` (DAO 스타일)

이 파일은 `JasyptColumnTypeTest.kt`를 미러링하지만 Exposed DAO API에 개념을 적용합니다.

- **Entity 매핑**: `jasyptVarChar`와 `jasyptBinary` 컬럼이 엔티티 프로퍼티에 매핑되는 방법을 보여줍니다.
- **원활한 DAO 사용**: 엔티티에 대한 CRUD 연산은 투명하며, 암호화된 프로퍼티로 쿼리도 예상대로 작동합니다.

## 코드 스니펫

### 1. Jasypt 암호화 컬럼이 있는 테이블 정의

```kotlin
import io.bluetape4k.exposed.core.jasypt.jasyptVarChar
import io.bluetape4k.exposed.core.jasypt.jasyptBinary
import io.bluetape4k.crypto.encrypt.Encryptors

object UserSecrets: IntIdTable("user_secrets") {
    val username = varchar("username", 255)

    // AES를 사용하는 API 키용 암호화 문자열 컬럼
    // 이 컬럼은 검색 가능합니다
    val apiKey = jasyptVarChar("api_key", 512, Encryptors.AES)

    // RC4를 사용하는 비밀 토큰용 암호화 바이너리 컬럼
    // 이 컬럼도 검색 가능합니다
    val secretToken = jasyptBinary("secret_token", 256, Encryptors.RC4)
}
```

### 2. 암호화된 데이터 삽입 및 조회 (DSL)

```kotlin
// 암호화된 레코드 삽입
val id = UserSecrets.insertAndGetId {
    it[username] = "john.doe"
    it[apiKey] = "my_super_secret_api_key_123"
    it[secretToken] = "binary_token_data".toByteArray()
}

// 조회 및 검증
val retrievedUser = UserSecrets.selectAll().where { UserSecrets.id eq id }.single()
retrievedUser[UserSecrets.username] shouldBeEqualTo "john.doe"
retrievedUser[UserSecrets.apiKey] shouldBeEqualTo "my_super_secret_api_key_123"
retrievedUser[UserSecrets.secretToken].toUtf8String() shouldBeEqualTo "binary_token_data"

// 암호화된 컬럼으로 쿼리 (암호화가 결정적이므로 작동)
val userByApiKey = UserSecrets.selectAll().where { UserSecrets.apiKey eq "my_super_secret_api_key_123" }.single()
userByApiKey[UserSecrets.username] shouldBeEqualTo "john.doe"
```

## 테스트 실행

```bash
# 이 모듈의 모든 테스트 실행
./gradlew :06-advanced:10-exposed-jasypt:test

# 특정 테스트 클래스 실행
./gradlew :06-advanced:10-exposed-jasypt:test --tests "exposed.examples.jasypt.JasyptColumnTypeTest"
```

## 더 읽어보기

- [Exposed Jasypt](https://debop.notion.site/Exposed-Jasypt-1c32744526b080f08ab2f3e21149e9d7)
- [Exposed Crypt](https://debop.notion.site/Exposed-Crypt-1c32744526b0802da419d5ce74d2c5f3)
