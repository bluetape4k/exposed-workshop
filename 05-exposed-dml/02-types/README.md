# 05 Exposed DML: Column Types (02-types)

Exposed에서 제공하는 다양한 컬럼 타입을 실제로 생성, 저장, 조회하는 방법을 다루는 모듈입니다. 기본 타입부터 배열/다차원 배열, BLOB, UUID, unsigned 수형까지 폭넓은 타입을 예제로 확인할 수 있습니다. 각 예제는 JUnit 테스트로 구성되어 있어, 다양한 DB Dialect별 동작 차이를 함께 검증합니다.

## 예제 구성

모든 예제는 `src/test/kotlin/exposed/examples/types` 아래에 있습니다.

### 1. Boolean / Char 타입

- `Ex01_BooleanColumnType.kt`
    - `bool` 컬럼의 기본 insert/select와 `booleanParam` 사용.
    - `CHAR(1)` 컬럼을 `Boolean`으로 매핑하는 `CharBooleanColumnType` 커스텀 타입 예제.
    - default/nullable 처리 및 조건식(`eq`, `isNull`) 조합.
- `Ex02_CharColumnType.kt`
    - `char` 컬럼의 기본 읽기/쓰기.
    - `collate` 옵션 사용 (Postgres/MySQL/MariaDB 전용)과 정렬 결과 비교.

### 2. Numeric 타입

- `Ex03_NumericColumnType.kt`
    - `byte/short/int` 범위 검증과 Out-of-range 오류 확인.
    - `byteParam`, `shortParam`, `intParam`, `floatParam`, `doubleParam`, `decimalParam` 등 파라미터 바인딩.
    - MySQL의 `float` 비교 특성(근사치) 처리.
    - unsigned 타입 체크 제약(`checkConstraintName`) 명시 예제.
- `Ex04_DoubleColumnType.kt`
    - `double` 컬럼 insert/select 기본 동작.
    - DDL의 `DOUBLE PRECISION`을 `REAL`로 변경했을 때의 호환성 확인.

### 3. Array / Multi-dimensional Array 타입

- `Ex05_ArrayColumnType.kt`
    - 배열 컬럼 생성/삭제 및 default 값 적용.
    - 배열 인덱스 접근(`column[2]`)과 슬라이스(`slice`) 사용.
    - `arrayParam`/리터럴 비교, `update`/`upsert` 적용.
    - DAO(Entity)에서 배열 컬럼 사용.
    - `anyFrom`/`allFrom`과의 비교, `ByteArray` 배열 처리, alias 사용.
    - 지원 DB: Postgres, H2 전용.
- `Ex06_MultiArrayColumnType.kt`
    - 2/3/5차원 배열 저장 및 조회.
    - `maximumCardinality` 제한, nullable 배열, literal/param 바인딩.
    - 배열 업데이트/업서트 및 인덱스 접근/슬라이스 예제.
    - DAO(Entity) 기반 다차원 배열 처리.
    - 지원 DB: Postgres 전용.

### 4. Unsigned 타입

- `Ex07_UnsignedColumnType.kt`
    - `ubyte/ushort/uint/ulong` 컬럼 생성 및 값 범위 검증.
    - DB별 DDL 차이 (MySQL의 `UNSIGNED`, Postgres의 CHECK 제약).
    - 이전 타입(TINYINT/SMALLINT/INT)에서 변경되는 마이그레이션 시나리오.

### 5. BLOB 타입

- `Ex08_BlobColumnType.kt`
    - `ExposedBlob` 저장/조회 및 alias 사용.
    - `blobParam` 사용과 `inputStream`/`bytes` 읽기.
    - 기본값 설정 (MySQL은 미지원).
    - Postgres `oid` 기반 `useObjectIdentifier` 옵션 동작.

### 6. UUID 타입 (Java/Kotlin)

- `Ex09_JavaUUIDColumnType.kt`
    - `javaUUID` 컬럼 insert/select, 조건 조회.
    - MariaDB의 native UUID 컬럼 테스트.
- `Ex10_KotlinUUIDColumnType.kt`
    - Kotlin `Uuid` 컬럼 (`uuid`) insert/select, 조건 조회.
    - MariaDB native UUID 컬럼 테스트.

## 실행 방법

IDE에서 각 테스트를 실행하거나, 아래 Gradle 명령으로 모듈 단위 테스트를 실행할 수 있습니다.

```bash
./gradlew :exposed-05-exposed-dml-02-types:test
```

## 참고 사항

- 배열 타입은 Postgres/H2에서만 지원됩니다.
- 다차원 배열은 Postgres 전용입니다.
- `blob` 기본값은 MySQL에서 지원되지 않습니다.
- `useObjectIdentifier`는 Postgres 전용입니다.

## Further Reading

- [7.2 Column Types](https://debop.notion.site/1c32744526b080f098f8f9727dc3615c?v=1c32744526b0817db4c7000c586f5ae0)
