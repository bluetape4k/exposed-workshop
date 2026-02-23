# 09 Spring: Exposed Repository (04)

동기식 Repository 패턴으로 Exposed 데이터 접근을 캡슐화하는 모듈입니다. 서비스 레이어와 데이터 접근 레이어를 분리하는 구조를 학습합니다.

## 학습 목표

- Repository 인터페이스/구현체 패턴을 익힌다.
- 서비스 계층에서 Exposed 의존을 분리한다.
- 테스트 가능성이 높은 계층 구조를 만든다.

## 선수 지식

- [`../03-spring-transaction/README.md`](../03-spring-transaction/README.md)

## 핵심 개념

- Repository 추상화
- 도메인 모델/테이블 매핑
- 서비스 계층 분리

## 실행 방법

```bash
./gradlew :exposed-09-spring-04-exposed-repository:test
```

## 실습 체크리스트

- 서비스 테스트에서 저장소 계약을 검증한다.
- 쿼리 로직 변경이 서비스 코드에 누수되지 않는지 확인

## 성능·안정성 체크포인트

- 공통 조회 패턴의 중복을 저장소 레이어에서 통합
- 잘못된 추상화로 쿼리 유연성이 떨어지지 않게 균형 유지

## 다음 모듈

- [`../05-exposed-repository-coroutines/README.md`](../05-exposed-repository-coroutines/README.md)
