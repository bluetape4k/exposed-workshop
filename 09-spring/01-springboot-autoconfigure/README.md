# 09 Spring: AutoConfiguration (01)

Spring Boot 자동 설정으로 Exposed를 빠르게 통합하는 모듈입니다. 최소 설정으로 DataSource/트랜잭션을 연결하는 기본 패턴을 다룹니다.

## 학습 목표

- Spring Boot 자동 구성과 Exposed 연동 구조를 이해한다.
- `application.yml` 기반 최소 설정을 익힌다.
- 자동 구성 환경에서 트랜잭션 경계를 검증한다.

## 선수 지식

- Spring Boot 기본
- [`../04-exposed-ddl/01-connection/README.md`](../04-exposed-ddl/01-connection/README.md)

## 핵심 개념

- DataSource 자동 구성
- Exposed Database 초기화
- `@Transactional` 통합

## 실행 방법

```bash
./gradlew :exposed-09-spring-01-springboot-autoconfigure:test
```

## 실습 체크리스트

- 최소 설정으로 애플리케이션이 기동되는지 확인
- 트랜잭션 롤백 동작이 기대와 일치하는지 검증

## 성능·안정성 체크포인트

- 자동 구성 기본값 의존 대신 운영 설정을 명시
- 환경별 설정(dev/test/prod) 분리

## 다음 모듈

- [`../02-transactiontemplate/README.md`](../02-transactiontemplate/README.md)
