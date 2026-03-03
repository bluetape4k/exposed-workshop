# 06 Advanced: exposed-kotlin-datetime (03)

`kotlinx.datetime` 타입을 Exposed와 연동하는 모듈입니다. KMP 친화적인 날짜/시간 처리가 필요한 경우의 표준 패턴을 제공합니다.

## 학습 목표

- `kotlinx.datetime` 타입 매핑을 익힌다.
- `java.time` 대비 차이를 이해하고 선택 기준을 세운다.
- 리터럴/기본값 처리 시 호환성을 검증한다.

## 선수 지식

- [`../02-exposed-javatime/README.md`](../02-exposed-javatime/README.md)

## 핵심 개념

- `kotlinx.datetime.LocalDate/Instant`
- 멀티플랫폼 시간 처리
- DB 저장 타입 매핑

## 예제 구성

| 파일                        | 설명       |
|---------------------------|----------|
| `Ex01_KotlinDateTime.kt`  | 기본 타입/함수 |
| `Ex02_Defaults.kt`        | 기본값 처리   |
| `Ex03_DateTimeLiteral.kt` | 리터럴 조회   |

## 실행 방법

```bash
./gradlew :03-exposed-kotlin-datetime:test
```

## 실습 체크리스트

- 같은 시나리오를 `java.time` 모듈과 비교한다.
- 시간대 변환 경계 케이스를 추가 테스트한다.

## 성능·안정성 체크포인트

- 플랫폼별 시간 처리 차이를 공통 테스트로 고정
- 직렬화 포맷(ISO-8601 등)을 일관되게 유지

## 다음 모듈

- [`../04-exposed-json/README.md`](../04-exposed-json/README.md)
