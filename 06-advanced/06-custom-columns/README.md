# 06 Advanced: Custom Columns (06)

도메인 요구에 맞는 커스텀 컬럼 타입을 구현하는 모듈입니다. 직렬화/압축/암복호화 변환을 컬럼 계층에 캡슐화하는 패턴을 다룹니다.

## 학습 목표

- 커스텀 ColumnType 구현 구조를 이해한다.
- 읽기/쓰기 변환 로직을 안전하게 분리한다.
- 테스트로 변환 손실/호환성 문제를 검증한다.

## 선수 지식

- [`../05-exposed-dml/02-types/README.md`](../05-exposed-dml/02-types/README.md)

## 핵심 개념

- `ColumnType` 확장
- `valueFromDB`, `notNullValueToDB`
- 도메인 타입 직렬화

## 실행 방법

```bash
./gradlew :exposed-06-advanced-06-custom-columns:test
```

## 실습 체크리스트

- 경계값(null/빈값/최대길이) 변환 테스트를 추가한다.
- 이전 포맷 데이터와 호환 여부를 검증한다.

## 성능·안정성 체크포인트

- 변환 비용이 큰 경우 캐시/배치 전략을 검토
- 역직렬화 실패 시 오류 복구 경로를 준비

## 다음 모듈

- [`../07-custom-entities/README.md`](../07-custom-entities/README.md)
