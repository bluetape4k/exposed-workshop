# 04 Exposed DDL

[English](./README.md) | 한국어

Exposed에서 데이터베이스 연결과 스키마 정의(DDL)를 다루는 챕터입니다. 연결 메타데이터와 재시도 동작부터 테이블, 제약조건, 인덱스, 시퀀스, enum 작성까지 테스트로 확인합니다.

## 개요

이 챕터는 Exposed 애플리케이션의 기반이 되는 두 가지 주제를 다룹니다. **연결 관리**(`01-connection`)에서는 `Database.connect`, 연결 메타데이터, 트랜잭션 재시도 횟수, HikariCP 커넥션 재사용, H2 다중 DB 트랜잭션을 실습합니다. **스키마 정의**(`02-ddl`)에서는 `Table` 선언, 인덱스, 시퀀스, 커스텀 enum, `SchemaUtils`와 `MigrationUtils`를 활용한 DDL 실행을 다룹니다.

## 학습 목표

- URL/DataSource 입력 방식의 `Database.connect`와 메타데이터 조회를 이해한다.
- 테이블, 기본 키, 외래 키, 인덱스, 시퀀스, enum 컬럼을 선언하는 방법을 익힌다.
- 파라미터화 테스트로 DB dialect 차이와 마이그레이션 구문을 검증한다.

## 포함 모듈

| 모듈              | 설명                                                         |
|-----------------|------------------------------------------------------------|
| `01-connection` | `Database.connect`, 메타데이터 조회, 재시도 횟수, HikariCP 재사용, H2 다중 DB 트랜잭션 예제 |
| `02-ddl`        | 테이블/인덱스/제약조건/시퀀스/enum 선언과 `SchemaUtils`/`MigrationUtils` 기반 DDL 실행 |

## 아키텍처 흐름

![04 exposed ddl Architecture diagram](../docs/images/readme-diagrams/04-exposed-ddl-architecture-01.png)

## 선수 지식

- `03-exposed-basic`에서 DSL/DAO 흐름을 이해한 상태
- JDBC DataSource 및 트랜잭션 기본 개념

## 권장 학습 순서

1. `01-connection` — 연결 초기화, 예외 처리, 커넥션 풀
2. `02-ddl` — 테이블/인덱스/시퀀스/enum 선언

## 테스트 실행 방법

```bash
# 연결 관리 모듈 테스트
./gradlew :01-connection:test

# DDL 모듈 테스트
./gradlew :02-ddl:test

# H2만 대상으로 빠른 테스트
./gradlew :01-connection:test -PuseFastDB=true
./gradlew :02-ddl:test -PuseFastDB=true
```

## 테스트 포인트

- 연결 메타데이터, 트랜잭션 재시도 횟수, H2 다중 DB 격리를 확인한다.
- dialect별 스키마 생성/삭제, 누락 테이블/컬럼 마이그레이션, 중복 컬럼 실패를 검증한다.
- 인덱스 변형, 시퀀스 지원, enum 매핑, dialect별 실행 조건을 확인한다.
- DB별 DDL 차이로 인한 이식성 이슈를 실행 가능한 테스트로 문서화한다.

## 다음 챕터

- [05-exposed-dml](../05-exposed-dml/README.ko.md): DML/트랜잭션/Entity API 중심 학습으로 넘어갑니다.
