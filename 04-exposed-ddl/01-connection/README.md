# 04 Exposed DDL: 연결 관리 (01-connection)

Exposed 데이터베이스 연결 설정과 연결 안정성 검증을 다루는 모듈입니다. 연결 예외, 타임아웃, H2 연결 풀 및 다중 DB 연결 시나리오를 실습합니다.

## 학습 목표

- `Database.connect` 구성 방식을 이해한다.
- 연결 예외/타임아웃 처리 패턴을 익힌다.
- 다중 DB 연결 시 테스트 격리 전략을 익힌다.

## 선수 지식

- JDBC DataSource 기본
- [`../README.md`](../README.md)

## 핵심 개념

- 연결 초기화와 재시도
- timeout 설정
- 연결 풀/다중 DB 분리

## 예제 구성

| 파일                             | 설명              |
|--------------------------------|-----------------|
| `Ex01_Connection.kt`           | 기본 연결           |
| `Ex02_ConnectionException.kt`  | 연결 예외 처리        |
| `Ex03_ConnectionTimeout.kt`    | 연결 타임아웃         |
| `DataSourceStub.kt`            | 테스트용 DataSource |
| `h2/Ex01_H2_ConnectionPool.kt` | H2 풀 설정         |
| `h2/Ex02_H2_MultiDatabase.kt`  | 다중 DB 연결        |

## 실행 방법

```bash
./gradlew :01-connection:test
```

## 실습 체크리스트

- 잘못된 URL/계정으로 실패 시나리오를 재현한다.
- 타임아웃 값을 조정하며 실패 시간을 비교한다.

## 성능·안정성 체크포인트

- 과도한 재시도 루프를 방지
- 테스트 간 DB 상태가 공유되지 않도록 분리

## 다음 모듈

- [`../02-ddl/README.md`](../02-ddl/README.md)
