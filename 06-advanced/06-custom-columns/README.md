# 커스텀 컬럼 및 기본값 생성기

이 모듈은 사용자 정의 컬럼 타입과 클라이언트 측 기본값 생성기를 생성하여 Exposed의 기능을 확장하는 고급 예제 모음입니다. 이러한 기법을 통해 투명한 암호화, 압축, 바이너리 직렬화, 사용자 정의 ID 생성과 같은 기능을 테이블 정의에 직접 추가할 수 있습니다.

## 학습 목표

- 컬럼을 위한 사용자 정의 클라이언트 측 기본값 생성기(예: 고유 ID용) 생성 및 사용
- 압축 및 암호화와 같은 투명한 데이터 변환을 위한 사용자 정의 컬럼 타입 구현
- 검색 가능한(결정적) 암호화 컬럼 구축 방법 이해
- 직렬화를 사용하여 임의의 Kotlin 객체를 바이너리 컬럼에 저장하는 방법 학습
- 직렬화와 압축과 같은 여러 변환 결합

---

## 1. 사용자 정의 클라이언트 측 기본값 생성기

**(소스: `CustomClientDefaultFunctionsTest.kt`)**

Exposed의 `clientDefault` 메커니즘을 확장 함수로 감싸서 재사용 가능하고 설명적인 ID 생성기를 만들 수 있습니다. 이 함수들은 `INSERT` 문이 데이터베이스로 전송되기
*전에* 애플리케이션에서 호출됩니다.

### 핵심 개념

- **`clientDefault { ... }`**: 값이 제공되지 않은 경우 기본값을 생성하기 위해 람다를 실행하는 컬럼 정의의 함수입니다.
- **확장 함수**: `clientDefault`를 자신의 함수로 감싸면 깔끔하고 선언적인 API를 만들 수 있습니다.

### 예제

- **.timebasedGenerated()**: 시간 기반(버전 1) UUID를 생성합니다.
- **.snowflakeGenerated()**: Snowflake 알고리즘을 사용하여 k-ordered, 고유한 `Long` ID를 생성합니다.
- **.ksuidGenerated()**: 시간 순서이며 사전식으로 정렬 가능한 K-Sortable Unique Identifier를 생성합니다.

### 코드 스니펫

```kotlin
import io.bluetape4k.exposed.core.ksuidGenerated
import io.bluetape4k.exposed.core.snowflakeGenerated
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object ClientGenerated: IntIdTable() {
  // 이 컬럼들은 제공된 값이 없으면 자동으로 채워집니다
    val snowflake: Column<Long> = long("snowflake").snowflakeGenerated()
    val ksuid: Column<String> = varchar("ksuid", 27).ksuidGenerated()
}

// 사용법 (DSL)
ClientGenerated.insert {
  // snowflake나 ksuid에 값을 지정할 필요 없음
}

// 사용법 (DAO)
class ClientGeneratedEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<ClientGeneratedEntity>(ClientGenerated)
    // ...
}
ClientGeneratedEntity.new {
  // 프로퍼티가 자동으로 생성됨
}
```

---

## 2. 투명한 압축

**(소스: `compress/`)**

이 예제는 데이터를 데이터베이스에 쓰기 전에 자동으로 압축하고 읽을 때 압축을 해제하는 사용자 정의 컬럼 타입을 만드는 방법을 보여줍니다. 이는 큰 `TEXT`나
`BLOB` 필드의 저장 공간을 줄이는 데 이상적입니다.

### 핵심 개념

- **`compressedBinary(name, length, compressor)`**: `VARBINARY` 데이터베이스 컬럼에 매핑되는 사용자 정의 컬럼 타입입니다.
- **`compressedBlob(name, compressor)`**: `BLOB` 데이터베이스 컬럼에 매핑되는 사용자 정의 컬럼 타입입니다.
- **`Compressors`**: `LZ4`, `Snappy`, `Zstd`와 같은 다양한 압축 알고리즘을 제공하는 객체/열거형입니다.

### 코드 스니펫

```kotlin
import io.bluetape4k.exposed.core.compress.compressedBlob
import io.bluetape4k.io.compressor.Compressors

private object CompressedTable: IntIdTable() {
  // 이 컬럼은 BLOB 필드에 Zstd로 압축된 데이터를 저장합니다
    val compressedContent = compressedBlob("zstd_blob", Compressors.Zstd).nullable()
}

// 사용법
val largeData = "some very long string...".toByteArray()
CompressedTable.insert {
  // `largeData` ByteArray가 여기서 자동으로 압축됩니다
    it[compressedContent] = largeData
}

val row = CompressedTable.selectAll().single()
// 데이터는 읽을 때 자동으로 압축 해제됩니다
val originalData = row[CompressedTable.compressedContent]
```

---

## 3. 검색 가능한(결정적) 암호화

**(소스: `encrypt/`)**

이 예제는 투명하고 **결정적**인 암호화를 위한 사용자 정의 컬럼 타입을 구현합니다. "결정적"이라는 것은 동일한 입력이 항상 동일한 암호화된 출력을 생성한다는 의미입니다.

이는 일반적으로 무작위성(솔트, IV)을 추가하여 동일한 평문에 대해 다른 암호문을 생성하는 비결정적 암호화를 사용하는 `exposed-crypt` 모듈과 중요한 차이점입니다. 덜 안전하지만, 결정적 암호화는
`WHERE` 절에서 암호화된 데이터에 대한 직접 동등성 검사를 수행할 수 있습니다.

### 핵심 개념

- **`encryptedVarChar(name, length, encryptor)`**: 검색 가능한 암호화된 문자열을 저장하는 사용자 정의 컬럼입니다.
- **`encryptedBinary(name, length, encryptor)`**: 검색 가능한 암호화된 바이트 배열을 저장하는 사용자 정의 컬럼입니다.
- **`Encryptors`**: 결정적 출력으로 구성된 다양한 대칭 암호화 알고리즘(`AES`, `RC4` 등)을 제공합니다.

### 코드 스니펫

```kotlin
import io.bluetape4k.exposed.core.encrypt.encryptedVarChar
import io.bluetape4k.crypto.encrypt.Encryptors

private object EncryptedUsers: IntIdTable("EncryptedUsers") {
    val email = encryptedVarChar("email", 256, Encryptors.AES)
}

// 사용법
val userEmail = "test@example.com"
EncryptedUsers.insert {
    it[email] = userEmail
}

// 암호화가 결정적이므로 평문 값으로 검색할 수 있습니다
val user = EncryptedUsers.selectAll().where { EncryptedUsers.email eq userEmail }.single()

// 값은 조회 시 자동으로 복호화됩니다
user[EncryptedUsers.email] shouldBeEqualTo userEmail
```

---

## 4. 바이너리 직렬화

**(소스: `serialization/`)**

이 예제는 모든 `java.io.Serializable` Kotlin 객체를 바이너리 데이터베이스 컬럼(`VARBINARY` 또는
`BLOB`)에 저장하는 방법을 보여줍니다. 이는 구조화된 데이터를 저장하기 위한 JSON의 대안이며, 특히 압축과 결합할 때 더 공간 효율적일 수 있습니다.

### 핵심 개념

- **`binarySerializedBinary<T>(name, length, serializer)`**: `Serializable` 객체 타입 `T`를 `VARBINARY` 컬럼에 매핑합니다.
- **`binarySerializedBlob<T>(name, serializer)`**: `Serializable` 객체 타입 `T`를 `BLOB` 컬럼에 매핑합니다.
- **`BinarySerializers`**: 종종 압축 알고리즘과 결합된 다양한 바이너리 직렬화 라이브러리를 제공합니다(예: `LZ4Kryo`, `ZstdFory`).

### 코드 스니펫

```kotlin
import io.bluetape4k.exposed.core.serializable.binarySerializedBlob
import io.bluetape4k.io.serializer.BinarySerializers
import java.io.Serializable

// 데이터 클래스는 Serializable이어야 함
data class UserProfile(val username: String, val settings: Map<String, String>): Serializable

private object UserData: IntIdTable("UserData") {
  // UserProfile 객체를 BLOB에 저장, Kryo로 직렬화하고 LZ4로 압축
    val profile = binarySerializedBlob<UserProfile>("profile", BinarySerializers.LZ4Kryo)
}

// 사용법
val userProfile = UserProfile("john.doe", mapOf("theme" to "dark", "lang" to "en"))
UserData.insert {
    it[profile] = userProfile
}

// 객체는 읽을 때 자동으로 역직렬화 및 압축 해제됩니다
val retrievedProfile = UserData.selectAll().first()[UserData.profile]
retrievedProfile.settings["theme"] shouldBeEqualTo "dark"
```

## 테스트 실행

```bash
# 이 모듈의 모든 테스트 실행
./gradlew :06-advanced:06-custom-columns:test

# 특정 기능에 대한 테스트 실행, 예: 압축
./gradlew :06-advanced:06-custom-columns:test --tests "exposed.examples.custom.columns.compress.*"
```

## 더 읽어보기

- [Exposed Custom ColumnTypes](https://debop.notion.site/Custom-Columns-1c32744526b0802aa7a8e2e5f08042cb)
