# MyBatis Dynamic SQL 2 catalog 동기화

## 배경

`bluetape4k-dependencies`가 MyBatis Dynamic SQL 2.0.0과 Fory Kotlin 0.17.0을
shared catalog version으로 승격했다.

## 결정

Shared catalog 변경을 Exposed workshop repository에 반영하고 build가 계속 compile되는지
검증한다.

## 결과

`gradle/libs.versions.toml`은 이제 MyBatis Dynamic SQL 2.0.0과 Fory Kotlin 0.17.0을
담고 있다.

## 검증

- `./gradlew build -x test --no-daemon`

Build는 기존 unrelated warning과 함께 완료됐다.
