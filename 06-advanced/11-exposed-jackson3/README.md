# Exposed-Jackson3: Jackson 3 기반 JSON/JSONB 지원

이 모듈은 **[Jackson 3.x](https://github.com/fasterxml/jackson)**에서 작동하도록 특별히 설계된
`Exposed-Jackson` 통합의 업데이트된 버전입니다. Jackson 3 라이브러리의 최신 기능과 개선 사항을 활용하여 Exposed에서 `JSON`과 `JSONB` 컬럼 타입을 제공합니다.

## 학습 목표

- Jackson 3를 사용하여 Kotlin 데이터 클래스에 매핑되는 `json`과 `jsonb` 컬럼 정의
- Jackson 3를 사용하여 복잡하고 중첩된 객체를 효율적으로 저장 및 조회
- Jackson 3 기반 컬럼에서 Exposed의 JSON 쿼리 함수(`.extract<T>()`, `.contains()`, `.exists()`) 활용
- DSL과 DAO 프로그래밍 스타일 모두에 Jackson 3 기반 JSON 컬럼 적용

## 핵심 개념

이 모듈의 API와 기능은 `exposed-json`과
`exposed-jackson`(Jackson 2)과 같은 다른 JSON 통합 모듈과 일관되어 친숙한 개발 경험을 보장하면서 최신 Jackson 버전을 사용합니다.

### 컬럼 타입

- **`jackson<T>(name)`**: Jackson 3 호환 타입 `T`의 객체를 표준 `JSON` 텍스트 컬럼에 저장하는 컬럼을 정의합니다.
- **`jacksonb<T>(name)`**: Jackson 3 호환 타입 `T`의 객체를 최적화된
  `JSONB`(바이너리 JSON) 컬럼에 저장하는 컬럼을 정의합니다. 이는 PostgreSQL과 같이 지원하는 데이터베이스에 권장되는 선택입니다.

데이터 클래스에 `@Serializable` 표시가 필요 없습니다. Jackson은 일반적으로 객체 매핑에 리플렉션을 사용하므로 표준 Kotlin 데이터 클래스나 POJO일 수 있습니다.

### 조회 함수

Exposed가 제공하는 전체 JSON 조회 함수 제품군을 사용할 수 있습니다:

- **`.extract<T>(path, toScalar)`**: JSON 문서의 특정 경로에서 값을 추출합니다.
- **`.contains(value, path)`**: JSON 문서가 주어진 JSON 값을 포함하고 있는지 확인합니다.
- **`.exists(path, optional)`**: 주어진 JSONPath 표현식에서 키나 값의 존재 여부를 확인합니다.

## 예제 개요

### `JacksonSchema.kt`

이 파일은 데이터 클래스(`User`, `DataHolder`)와 Exposed `Table` 객체(`JacksonTable`, `JacksonBTable`)를 정의합니다. 또한 DAO `Entity` 클래스(
`JacksonEntity`, `JacksonBEntity`)와 테스트 헬퍼 함수도 포함합니다.

### `JacksonColumnTest.kt` (DSL & DAO with `json`)

이 파일은 Jackson 3와 함께 `jackson`(텍스트 기반 JSON) 컬럼 타입의 사용법을 보여줍니다. `.extract()`, `.contains()`,
`.exists()`를 사용한 표준 CRUD 연산과 JSON 특화 쿼리를 다룹니다. 또한 DAO 패턴과의 통합도 보여줍니다.

### `JacksonBColumnTest.kt` (DSL & DAO with `jsonb`)

이 파일은 `JacksonColumnTest.kt`의 예제를 미러링하지만 Jackson 3와 함께 `jacksonb` 컬럼 타입에 초점을 맞춥니다. 코드는 유사하게 구성되어 `json`과
`jsonb` 타입 전반의 일관된 API를 강조합니다.

## 코드 스니펫

### 1. `jacksonb` 컬럼이 있는 테이블 정의 (Jackson 3)

```kotlin
import io.bluetape4k.exposed.core.jackson3.jacksonb
import com.fasterxml.jackson.annotation.JsonCreator // Jackson 3 어노테이션 예시

// 표준 데이터 클래스
data class User(val name: String, val team: String?)
data class UserData(val info: User, val logins: Int, val active: Boolean)

object UsersTable: IntIdTable("users") {
    // 컬럼은 Jackson 3를 사용하여 UserData 객체를 JSONB로 저장
    val data = jacksonb<UserData>("data")
}
```

### 2. Jackson 3로 삽입 및 조회 (DSL)

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

### 3. Entity에서 Jackson 3 컬럼 사용 (DAO)

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
./gradlew :06-advanced:11-exposed-jackson3:test

# JSONB 컬럼 타입에 대한 테스트 실행
./gradlew :06-advanced:11-exposed-jackson3:test --tests "exposed.examples.jackson3.JacksonBColumnTest"
```

## 더 읽어보기

- [Exposed Jackson](https://debop.notion.site/Exposed-Jackson-1c32744526b0809599a7db2e629a597a)
