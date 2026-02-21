# Exposed-Jackson: Jackson 라이브러리 기반 JSON/JSONB 지원

이 모듈은 인기 있는 **Jackson** 라이브러리를 활용하여 Exposed에서 `JSON`과 `JSONB` 컬럼 타입을 사용하는 방법을 보여줍니다. 이는 `kotlinx.serialization`을 사용하는
`exposed-json` 모듈의 대안을 제공하며, 이미 Jackson 생태계를 사용하고 있는 프로젝트에 이상적입니다.

## 학습 목표

- Jackson을 사용하여 Kotlin 데이터 클래스에 매핑되는 `json`과 `jsonb` 컬럼 정의
- `@Serializable` 어노테이션 없이 복잡하고 중첩된 객체 저장 및 조회
- `.extract<T>()`, `.contains()`, `.exists()`를 포함한 Exposed의 전체 JSON 쿼리 함수 범위를 Jackson 기반 컬럼에서 사용
- DSL과 DAO 프로그래밍 스타일 모두에 Jackson 기반 JSON 컬럼 적용

## 핵심 개념

이 모듈의 API는 `exposed-json`과 거의 동일하지만, 기본 구현은 Jackson의 `ObjectMapper`를 사용합니다.

### 컬럼 타입

- **`jackson<T>(name)`**: Jackson 호환 타입 `T`의 객체를 표준 `JSON` 텍스트 컬럼에 저장하는 컬럼을 정의합니다.
- **`jacksonb<T>(name)`**: Jackson 호환 타입 `T`의 객체를 최적화된
  `JSONB`(바이너리 JSON) 컬럼에 저장하는 컬럼을 정의합니다. 이는 PostgreSQL과 같이 지원하는 데이터베이스에 권장되는 선택입니다.

`exposed-json`과 달리 데이터 클래스에 `@Serializable` 표시가 **필요 없습니다**. 표준 Kotlin 데이터 클래스나 POJO일 수 있습니다.

### 조회 함수

동일한 강력한 조회 함수를 사용할 수 있습니다:

- **`.extract<T>(path, toScalar)`**: JSON 문서의 특정 경로에서 값을 추출합니다.
- **`.contains(value, path)`**: JSON 문서가 주어진 JSON 형식 문자열을 값으로 포함하고 있는지 확인합니다.
- **`.exists(path, optional)`**: 주어진 JSONPath 표현식에서 값의 존재 여부를 확인합니다.

## 예제 개요

### `JacksonSchema.kt`

이 파일은 데이터 클래스(`User`, `DataHolder`)와 Exposed `Table` 객체(`JacksonTable`, `JacksonBTable`)를 정의합니다. 또한 DAO `Entity` 클래스(
`JacksonEntity`, `JacksonBEntity`)와 테스트 헬퍼 함수도 포함합니다.

### `JacksonColumnTest.kt` (DSL & DAO with `json`)

이 파일은 `jackson`(텍스트 기반 JSON) 컬럼 타입의 사용법을 보여줍니다:

- `INSERT`, `UPDATE`, `UPSERT`, `SELECT` 연산
- `.extract()`, `.contains()`, `.exists()`를 사용한 쿼리
- DAO 엔티티 내에서 컬럼 사용
- 컬렉션 및 nullable JSON 컬럼 처리

### `JacksonBColumnTest.kt` (DSL & DAO with `jsonb`)

이 파일은 `JacksonColumnTest.kt`의 예제를 미러링하지만 더 성능이 좋은 `jacksonb` 컬럼 타입을 사용합니다. 코드는 거의 동일하며 API의 일관성을 보여줍니다.

## 코드 스니펫

### 1. `jacksonb` 컬럼이 있는 테이블 정의

```kotlin
import io.bluetape4k.exposed.core.jackson.jacksonb

// 표준 데이터 클래스 - @Serializable 불필요
data class User(val name: String, val team: String?)
data class UserData(val info: User, val logins: Int, val active: Boolean)

object UsersTable: IntIdTable("users") {
    // 컬럼은 Jackson을 사용하여 UserData 객체를 JSONB로 저장
    val data = jacksonb<UserData>("data")
}
```

### 2. Jackson으로 삽입 및 조회 (DSL)

```kotlin
val userData = UserData(info = User("test", "A"), logins = 5, active = true)

// 데이터 삽입
UsersTable.insert {
    it[data] = userData
}

// 중첩된 값 추출 후 WHERE 절에서 사용
// 참고: 경로 구문은 데이터베이스마다 다를 수 있음
val username = UsersTable.data.extract<String>(".info.name")
val userRecord = UsersTable.selectAll().where { username eq "test" }.single()

// 전체 객체는 읽을 때 자동으로 역직렬화됨
val retrievedData = userRecord[UsersTable.data]
retrievedData.logins shouldBeEqualTo 5
```

### 3. Entity에서 Jackson 컬럼 사용 (DAO)

```kotlin
class UserEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<UserEntity>(UsersTable)

    // 프로퍼티는 JSON으로/에서 자동 매핑됨
    var data by UsersTable.data
}

// 새 엔티티 생성
val entity = UserEntity.new {
    data = UserData(info = User("dao_user", "B"), logins = 1, active = true)
}

// 프로퍼티 접근
println(entity.data.info.name) // "dao_user" 출력
```

## 테스트 실행

**참고**: JSON/JSONB 기능은 데이터베이스에 따라 크게 달라집니다. 많은 테스트는 지원이 제한적인 데이터베이스(예: H2)에서는 건너뜁니다. 최상의 결과를 위해 PostgreSQL에서 실행하세요.

```bash
# 이 모듈의 모든 테스트 실행
./gradlew :06-advanced:08-exposed-jackson:test

# JSONB 컬럼 타입에 대한 테스트 실행
./gradlew :06-advanced:08-exposed-jackson:test --tests "exposed.examples.jackson.JacksonBColumnTest"
```

## 더 읽어보기

- [Exposed Jackson](https://debop.notion.site/Exposed-Jackson-1c32744526b0809599a7db2e629a597a)
