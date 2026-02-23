# 00 Shared: Exposed Shared Tests

공통 스키마, 유틸, 테스트 베이스 등을 모아 Exposed 예제 전반에서 공유하는 기반을 제공합니다.

## 챕터 목표

- 테스트 패턴과 DB 설정을 중앙화해 중복을 줄인다.
- 다양한 DB Dialect에서 통일된 스키마/데이터 생성 흐름을 확보한다.
- Testcontainers, 트랜잭션 헬퍼, SchemaUtils의 재사용 궤적을 마련한다.

## 선수 지식

- Kotlin/Java 기초 문법
- 관계형 데이터베이스와 JDBC 개념

## 포함 모듈

| 모듈                     | 설명                                                 |
|------------------------|----------------------------------------------------|
| `exposed-shared-tests` | 공통 테스트 베이스 클래스와 DB 설정, `WithTables` 헬퍼 및 ERD 문서 집합 |

## 권장 학습 순서

1. `exposed-shared-tests`

## 실행 방법

```bash
./gradlew :exposed-00-shared-exposed-shared-tests:test
```

## 테스트 포인트

- 각 Dialect에서 공통 스키마/데이터가 동일하게 생성되는지 확인한다.
- Testcontainers 기반 DB 격리 환경에서 연결/롤백이 안정적인지 검증한다.

## 성능·안정성 체크포인트

- Dialect별 스키마 생성/삭제가 테스트 간 독립적으로 작동하는지 점검한다.
- Testcontainers 환경에서 커넥션 재활용과 롤백이 일정하게 수행되는지 확인한다.

## 참고

- `AbstractExposedTest`, `WithTables` 등의 헬퍼 상속 구조를 참고해 실제 예제에서 재사용 가능하다.
- 다양한 ERD 문서와 Faker 기반 샘플 데이터가 포함되어 있다.

## 다음 챕터

- [01-spring-boot](../01-spring-boot/README.md): Spring Boot 기반 MVC/WebFlux 예제에서 Exposed 사용 패턴을 학습합니다.
