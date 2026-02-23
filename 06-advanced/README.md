# 06 Advanced

실무에서 Exposed를 확장 적용할 때 필요한 커스텀 컬럼, 날짜/시간, JSON, 암복호화, 직렬화 연동 주제를 정리하는 챕터입니다.

## 챕터 목표

- 커스텀 컬럼과 확장 포인트를 이해하고, JSON/시간 타입 처리에서 DB 간 차이를 제어한다.
- 직렬화/암복호화, 외부 라이브러리 연동 시 안정적인 데이터 흐름을 설계한다.
- 테스트를 통해 커스텀 타입/엔티티 확장 모듈을 검증한다.

## 선수 지식

- `05-exposed-dml` 내용
- JSON/시간 처리에 대한 기본 이해

## 포함 모듈

| 모듈                           | 설명                    |
|------------------------------|-----------------------|
| `01-exposed-crypt`           | 암복호화/보안 컬럼 예제         |
| `02-exposed-javatime`        | Java Time 타입 매핑       |
| `03-exposed-kotlin-datetime` | Kotlin datetime 타입 매핑 |
| `04-exposed-json`            | JSON 컬럼 매핑            |
| `05-exposed-money`           | 금액 타입 모델링             |
| `06-custom-columns`          | 커스텀 컬럼 타입             |
| `07-custom-entities`         | Entity 확장 모델          |
| `08-exposed-jackson`         | Jackson 연동            |
| `09-exposed-fastjson2`       | Fastjson2 연동          |
| `10-exposed-jasypt`          | Jasypt 연동             |
| `11-exposed-jackson3`        | Jackson3 연동           |

## 권장 학습 순서

1. `06-custom-columns`
2. `04-exposed-json`
3. `02-exposed-javatime`
4. `03-exposed-kotlin-datetime`
5. 나머지 모듈

## 실행 방법

```bash
./gradlew :exposed-06-advanced:test
```

## 테스트 포인트

- 직렬화/역직렬화 왕복 시 데이터 손실이 없는지를 확인한다.
- 커스텀 컬럼 null/기본값 처리 로직을 검증한다.

## 성능·안정성 체크포인트

- JSON 직렬화 비용과 컬럼 크기 증가 영향까지 측정한다.
- 암복호화 컬럼 사용 시 인덱스 전략과 검색 제약을 검토한다.

## 다음 챕터

- [07-jpa](../07-jpa/README.md): 기존 JPA 프로젝트를 Exposed로 전환하는 실전 패턴을 다룹니다.
