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

## 실행 방법

```bash
./gradlew :02-ddl:test
```

## 실습 체크리스트

- 인덱스 전/후 실행계획 차이를 확인한다.
- enum/sequence 지원 차이를 DB별로 비교한다.

## 성능·안정성 체크포인트

- DDL은 운영 배포 시 잠금 영향도 사전 점검
- `createMissingTablesAndColumns` 사용 시 예상치 못한 변경을 검토

## 다음 챕터

- [`../05-exposed-dml/README.md`](../05-exposed-dml/README.md)
