# 06 Advanced: exposed-money (05)

JavaMoney 기반 통화 값을 Exposed 컬럼으로 다루는 모듈입니다. 금액과 통화를 함께 저장해 금융 도메인 정합성을 높이는 패턴을 제공합니다.

## 학습 목표

- `compositeMoney` 매핑 구조를 이해한다.
- 통화/금액 동시 저장과 조회 패턴을 익힌다.
- 부동소수점 오차 대신 정밀 타입을 사용하는 이유를 이해한다.

## 선수 지식

- [`../05-exposed-dml/02-types/README.md`](../05-exposed-dml/02-types/README.md)

## 핵심 개념

- `MonetaryAmount` <-> 복합 컬럼 매핑
- 통화 코드 기반 필터링
- 기본값/클라이언트 기본값

## 예제 구성

| 파일                      | 설명         |
|-------------------------|------------|
| `MoneyData.kt`          | 테이블/도메인 정의 |
| `Ex01_MoneyDefaults.kt` | 기본값 설정     |
| `Ex02_Money.kt`         | CRUD/조회    |

## 실행 방법

```bash
./gradlew :05-exposed-money:test
```

## 실습 체크리스트

- 동일 금액의 서로 다른 통화 입력 시 동작을 검증한다.
- 금액 정렬/집계 시 타입 정밀도를 확인한다.

## 성능·안정성 체크포인트

- 금액은 `Double/Float` 대신 Decimal 기반 타입 사용
- 환율 변환 책임(애플리케이션/외부 서비스)을 명확히 분리

## 다음 모듈

- [`../06-custom-columns/README.md`](../06-custom-columns/README.md)
