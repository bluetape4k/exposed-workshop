# 06 Advanced: Custom Entities (07)

기본 `Int/Long/UUID` 이외의 ID 전략을 사용하는 Entity 패턴을 다루는 모듈입니다. 도메인별 ID 생성 규칙을 Exposed 모델에 반영하는 방법을 학습합니다.

## 학습 목표

- 커스텀 ID 타입 기반 Entity 모델링을 익힌다.
- ID 생성 전략과 저장 타입 매핑을 이해한다.
- 정렬/검색/인덱싱에 미치는 영향을 검토한다.

## 선수 지식

- [`../05-exposed-dml/05-entities/README.md`](../05-exposed-dml/05-entities/README.md)

## 핵심 개념

- 커스텀 ID 생성기
- Entity/Table 매핑 확장
- 도메인 무결성과 고유성

## 실행 방법

```bash
./gradlew :exposed-06-advanced-07-custom-entities:test
```

## 실습 체크리스트

- 동시 생성 상황에서 ID 충돌 가능성을 검증한다.
- 정렬 가능한 ID 전략 여부를 비교한다.

## 성능·안정성 체크포인트

- ID 길이 증가에 따른 인덱스 비용을 검토
- 생성기 시계 의존성(시간 역행 등) 리스크 관리

## 다음 모듈

- [`../08-exposed-jackson/README.md`](../08-exposed-jackson/README.md)
