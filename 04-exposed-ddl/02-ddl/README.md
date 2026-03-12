# 04 Exposed DDL: 스키마 정의 (02-ddl)

Exposed DDL API로 테이블, 컬럼, 인덱스, 시퀀스를 정의하는 모듈입니다. DB 스키마 변경을 테스트로 검증하는 기본 패턴을 제공합니다.

## 학습 목표

- 테이블/컬럼/제약조건 정의를 익힌다.
- 인덱스와 시퀀스 사용법을 익힌다.
- 스키마 변경 시 회귀를 테스트로 관리한다.

## 선수 지식

- [`../01-connection/README.md`](../01-connection/README.md)

## 핵심 개념

- `SchemaUtils.create`, `createMissingTablesAndColumns`
- 컬럼 제약조건(`nullable`, `default`, `uniqueIndex`)
- 인덱스/시퀀스/커스텀 enum

## 예제 구성

| 파일                                     | 설명            |
|----------------------------------------|---------------|
| `Ex01_CreateDatabase.kt`               | DB 생성 (지원 환경) |
| `Ex02_CreateTable.kt`                  | 테이블 생성        |
| `Ex03_CreateMissingTableAndColumns.kt` | 누락 테이블/컬럼 보완  |
| `Ex04_ColumnDefinition.kt`             | 컬럼/제약조건       |
| `Ex05_CreateIndex.kt`                  | 인덱스           |
| `Ex06_Sequence.kt`                     | 시퀀스           |
| `Ex07_CustomEnumeration.kt`            | 커스텀 enum      |
| `Ex10_DDL_Examples.kt`                 | DDL 종합 예제    |

## 실행 방법

```bash
# 전체 모듈 테스트
./gradlew :04-exposed-ddl:02-ddl:test

# 특정 테스트 클래스만 실행
./gradlew :04-exposed-ddl:02-ddl:test --tests "exposed.examples.ddl.Ex02_CreateTable"
./gradlew :04-exposed-ddl:02-ddl:test --tests "exposed.examples.ddl.Ex10_DDL_Examples"
```

## 복잡한 시나리오

### 복합 PK / 복합 FK (`Ex02_CreateTable.kt`)

2컬럼 복합 PK 정의 후, `foreignKey(idA, idB, target = parent.primaryKey)` 방식과
`foreignKey(idA to parent.pidA, idB to parent.pidB)` 방식 두 가지로 복합 FK를 생성합니다.
`ON DELETE CASCADE` / `ON UPDATE CASCADE` 옵션을 함께 적용합니다.

### 스키마 마이그레이션 (`Ex03_CreateMissingTableAndColumns.kt`)

`createMissingTablesAndColumns`를 이용해 이미 존재하는 테이블에 누락된 uniqueIndex를 추가하거나,
`autoIncrement` 속성을 제거하는 마이그레이션 시나리오를 검증합니다.
두 Exposed Table 객체가 동일한 물리 테이블을 가리킬 때의 동작을 확인합니다.

### 커스텀 컬럼 타입 — Enum (`Ex07_CustomEnumeration.kt`)

- `enumerationByName`: Enum 이름을 VARCHAR로 저장 (이식성 높음)
- `customEnumeration`: DB 네이티브 ENUM 타입 사용 (PostgreSQL `CREATE TYPE … AS ENUM`, MySQL `ENUM(…)`)
- Enum 컬럼을 `reference()`로 참조하는 FK 시나리오

### Functional / Partial 인덱스 (`Ex05_CreateIndex.kt`)

- **Partial index**: `WHERE flag = TRUE` 조건부 인덱스 (PostgreSQL 전용)
- **Functional index**: `LOWER(item)`, `amount * price` 등 함수식 기반 인덱스 (PostgreSQL · MySQL 8)
- `getIndices()`로 생성된 인덱스 수 검증

### DDL 종합 예제 (`Ex10_DDL_Examples.kt`)

체크 제약조건, 스키마 간 FK, 복합 FK의 unique index 참조, inner join 다중 FK,
`DROP TABLE` 시 캐시 플러시 동작, UUID/불리언/텍스트 컬럼 타입 등 38개 시나리오를 포함합니다.

## 실습 체크리스트

- 인덱스 전/후 실행계획 차이를 확인한다.
- enum/sequence 지원 차이를 DB별로 비교한다.

## 성능·안정성 체크포인트

- DDL은 운영 배포 시 잠금 영향도 사전 점검
- `createMissingTablesAndColumns` 사용 시 예상치 못한 변경을 검토

## 다음 챕터

- [`../05-exposed-dml/README.md`](../05-exposed-dml/README.md)
