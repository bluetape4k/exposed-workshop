# Exposed-Fastjson2: Alibaba Fastjson2 기반 JSON/JSONB 지원

이 모듈은 Alibaba의 **Fastjson2** 라이브러리를 Exposed와 통합하여 `JSON`과
`JSONB` 컬럼 타입을 처리하는 방법을 보여줍니다. Fastjson2는 JSON 직렬화 및 역직렬화에서 뛰어난 성능으로 알려져 있어, JSON 처리 속도가 중요한 애플리케이션에 적합합니다.

## 학습 목표

- Fastjson2를 사용하여 Kotlin 데이터 클래스에 매핑되는 `json`과 `jsonb` 컬럼 정의
- Fastjson2로 복잡하고 중첩된 객체를 효율적으로 저장 및 조회
- Fastjson2 기반 컬럼에서 Exposed의 JSON 쿼리 함수(`.extract<T>()`, `.contains()`, `.exists()`) 활용
- DSL과 DAO 프로그래밍 스타일 모두에 Fastjson2 기반 JSON 컬럼 적용

## 핵심 개념

이 모듈의 API와 기능은 `exposed-json`(`kotlinx.serialization` 사용) 및
`exposed-jackson`(Jackson 사용)과 매우 유사하지만, 기본 직렬화 엔진으로 Fastjson2를 사용합니다.

### 컬럼 타입

- **`fastjson<T>(name)`**: Fastjson2 호환 타입 `T`의 객체를 표준 `JSON` 텍스트 컬럼에 저장하는 컬럼을 정의합니다.
- **`fastjsonb<T>(name)`**: Fastjson2 호환 타입 `T`의 객체를 최적화된
  `JSONB`(바이너리 JSON) 컬럼에 저장하는 컬럼을 정의합니다. 이는 일반적으로 지원하는 데이터베이스(예: PostgreSQL)에 권장됩니다.

Jackson 통합과 마찬가지로 데이터 클래스에 특별한 어노테이션이 **필요하지 않습니다**. Fastjson2는 일반적으로 리플렉션을 사용하여 표준 Kotlin 데이터 클래스나 POJO로 작업할 수 있습니다.

### 조회 함수

Exposed가 제공하는 동일한 강력한 JSON 조회 함수 세트를 사용할 수 있습니다:

- **`.extract<T>(path, toScalar)`**: JSON 문서의 특정 경로에서 값을 추출합니다.
- **`.contains(value, path)`**: JSON 문서가 주어진 JSON 값을 포함하고 있는지 확인합니다.
- **`.exists(path, optional)`**: 주어진 JSONPath 표현식에서 키나 값의 존재 여부를 확인합니다.

## 예제 개요

### `FastjsonSchema.kt`

이 파일은 데이터 클래스(`User`, `DataHolder`)와 Exposed `Table` 객체(`FastjsonTable`, `FastjsonBTable`)를 정의합니다. 또한 DAO `Entity` 클래스(
`FastjsonEntity`, `FastjsonBEntity`)와 예제를 촉진하는 테스트 헬퍼 함수도 포함합니다.

### `FastjsonColumnTest.kt` (DSL & DAO with `json`)

이 파일은 `fastjson`(텍스트 기반 JSON) 컬럼 타입의 사용법을 보여줍니다. `.extract()`, `.contains()`,
`.exists()`를 사용한 표준 CRUD 연산과 JSON 특화 쿼리를 다룹니다. 또한 DAO 패턴과의 통합도 보여줍니다.

### `FastjsonBColumnTest.kt` (DSL & DAO with `jsonb`)

이 파일은 `FastjsonColumnTest.kt`의 예제를 미러링하지만 `fastjsonb` 컬럼 타입에 초점을 맞춥니다. 코드는 유사하게 구성되어 `json`과
`jsonb` 타입 전반의 일관된 API를 강조합니다.

## 코드 스니펫

### 1. `fastjsonb` 컬럼이 있는 테이블 정의

```kotlin
import io.bluetape4k.exposed.core.fastjson2.fastjsonb
import com.alibaba.fastjson.annotation.JSONField // 선택 사항, 사용자 정의를 위해 사용 가능

// 표준 데이터 클래스
data class User(val name: String, val team: String?)
data class UserData(val info: User, val logins: Int, val active: Boolean)

object UsersTable : IntIdTable("users") {
    // 컬럼은 Fastjson2를 사용하여 UserData 객체를 JSONB로 저장
    val data = fastjsonb<UserData>("data")
}
```

### 2. Fastjson2로 삽입 및 조회 (DSL)

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

### 3. Entity에서 Fastjson2 컬럼 사용 (DAO)

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
./gradlew :06-advanced:09-exposed-fastjson2:test

# FastjsonB 컬럼 타입에 대한 테스트 실행
./gradlew :06-advanced:09-exposed-fastjson2:test --tests "exposed.examples.fastjson2.FastjsonBColumnTest"
```

## 더 읽어보기

- [Exposed Fastjson2](https://debop.notion.site/Exposed-Fastjson2-1c32744526b08050a9d4de947c3b3f0d)
