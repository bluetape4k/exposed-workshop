# 05 Exposed DML: Column Types (02-types)

Exposed 컬럼 타입을 DB Dialect별로 검증하는 모듈입니다. 기본 타입뿐 아니라 배열, 다차원 배열, BLOB, UUID, unsigned 타입까지 실습합니다.

## 학습 목표

- Exposed 컬럼 타입 정의와 바인딩 방식을 익힌다.
- DB마다 다른 타입 지원 범위를 테스트로 확인한다.
- 커스텀/특수 타입 사용 시 제약 조건과 이식성 포인트를 이해한다.

## 선수 지식

- [`../01-dml/README.md`](../01-dml/README.md)

## 핵심 개념

- 파라미터 바인딩: `*_Param` 기반 안전한 바인딩
- 배열 타입: 인덱스 접근, 슬라이스, 리터럴/파라미터 비교
- UUID/BLOB: DB별 저장 방식과 기본값/스트림 처리 차이

## 예제 지도

소스 위치: `src/test/kotlin/exposed/examples/types`

| 범주    | 파일                                                                                                                   |
|-------|----------------------------------------------------------------------------------------------------------------------|
| 기본 타입 | `Ex01_BooleanColumnType.kt`, `Ex02_CharColumnType.kt`, `Ex03_NumericColumnType.kt`, `Ex04_DoubleColumnType.kt`       |
| 배열 타입 | `Ex05_ArrayColumnType.kt`, `Ex06_MultiArrayColumnType.kt`                                                            |
| 확장 타입 | `Ex07_UnsignedColumnType.kt`, `Ex08_BlobColumnType.kt`, `Ex09_JavaUUIDColumnType.kt`, `Ex10_KotlinUUIDColumnType.kt` |

## 실행 방법

```bash
./gradlew :02-types:test
```

## 실습 체크리스트

- 배열/다차원 배열 지원 여부를 DB별로 표로 정리한다.
- UUID(Java/Kotlin) 타입 간 변환 경계에서 직렬화 이슈가 없는지 확인한다.
- unsigned 타입 범위 초과 입력 시 실패 동작을 검증한다.

## DB별 주의사항

- 배열 타입: PostgreSQL/H2 중심
- 다차원 배열: PostgreSQL 전용
- `blob` 기본값: MySQL 미지원
- `useObjectIdentifier`: PostgreSQL 전용

## 성능·안정성 체크포인트

- 대형 BLOB 조회 시 전체 적재보다 스트림 접근을 우선 고려
- 배열 컬럼은 인덱싱/검색 전략을 별도로 설계
- 타입 변환 실패를 테스트로 고정해 런타임 오류를 사전 차단

## 다음 모듈

- [`../03-functions/README.md`](../03-functions/README.md)
