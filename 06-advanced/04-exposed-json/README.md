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

## 실행 방법

```bash
./gradlew :04-exposed-json:test
```

## 실습 체크리스트

- JSON/JSONB 컬럼에서 동일 쿼리 성능을 비교한다.
- 중첩 필드 조건 조회를 추가해본다.

## 성능·안정성 체크포인트

- 검색 중심이면 JSONB + 인덱스 전략 우선
- 스키마 유연성 때문에 발생하는 데이터 품질 리스크를 검증

## 다음 모듈

- [`../05-exposed-money/README.md`](../05-exposed-money/README.md)
