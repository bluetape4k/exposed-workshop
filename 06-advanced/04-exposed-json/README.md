# 06 Advanced: exposed-json (04)

JSON/JSONB 컬럼에 Kotlin 객체를 저장/조회하는 모듈입니다. 문서형 필드가 필요한 도메인에서 Exposed JSON 쿼리 패턴을 학습합니다.

## 학습 목표

- JSON/JSONB 컬럼 정의와 매핑을 익힌다.
- JSON 경로 추출/포함/존재 조건 쿼리를 작성한다.
- JSON vs JSONB 선택 기준을 이해한다.

## 선수 지식

- [`../05-exposed-dml/03-functions/README.md`](../05-exposed-dml/03-functions/README.md)

## 핵심 개념

- `json<T>()`, `jsonb<T>()`
- `extract`, `contains`, `exists`
- 직렬화/역직렬화 일관성

## 예제 구성

| 파일                    | 설명          |
|-----------------------|-------------|
| `JsonTestData.kt`     | 데이터 클래스/테이블 |
| `Ex01_JsonColumn.kt`  | JSON 컬럼     |
| `Ex02_JsonBColumn.kt` | JSONB 컬럼    |

## DB별 JSONB vs JSON 차이

| DB | JSON | JSONB |
|----|------|-------|
| PostgreSQL | 텍스트 저장, 삽입 순서 유지 | 바이너리 저장, 인덱싱 가능, 중복 키 제거 |
| MySQL V8 | 바이너리 저장(`JSON` 타입) | 별도 타입 없음 (JSON과 동일) |
| MariaDB | JSON 타입 | 미지원 |
| H2 | JSON 타입 | 미지원 |

검색 성능이 중요하면 PostgreSQL의 JSONB + GIN 인덱스를 고려합니다.

## 실행 방법

```bash
./gradlew :06-advanced:04-exposed-json:test
```

## 복잡한 시나리오

### JSON 경로 추출

`extract` 함수를 사용해 JSON 필드 내부 값을 직접 SELECT 조건이나 반환 값으로 활용합니다.

- 관련 파일: [`Ex01_JsonColumn.kt`](src/test/kotlin/exposed/examples/json/Ex01_JsonColumn.kt), [`Ex02_JsonBColumn.kt`](src/test/kotlin/exposed/examples/json/Ex02_JsonBColumn.kt)
- 테스트: `jsonExtract` — 중첩 필드 경로(`$.field`) 추출 검증

### JSON 존재 여부 체크

`exists` 함수로 JSON 문서 내 특정 경로나 값의 존재 여부를 조건으로 사용합니다.

- 관련 파일: [`Ex02_JsonBColumn.kt`](src/test/kotlin/exposed/examples/json/Ex02_JsonBColumn.kt)
- 테스트: `jsonbExists` — PostgreSQL `jsonb_path_exists` 기반 조건 검증

### JSON 포함 여부 체크

`contains` 함수로 JSON 문서가 특정 하위 문서를 포함하는지 확인합니다.

- 관련 파일: [`Ex02_JsonBColumn.kt`](src/test/kotlin/exposed/examples/json/Ex02_JsonBColumn.kt)
- 테스트: `jsonbContains` — JSONB `@>` 연산자 기반 포함 조건 검증

## 실습 체크리스트

- JSON/JSONB 컬럼에서 동일 쿼리 성능을 비교한다.
- 중첩 필드 조건 조회를 추가해본다.

## 성능·안정성 체크포인트

- 검색 중심이면 JSONB + 인덱스 전략 우선
- 스키마 유연성 때문에 발생하는 데이터 품질 리스크를 검증

## 다음 모듈

- [`../05-exposed-money/README.md`](../05-exposed-money/README.md)
