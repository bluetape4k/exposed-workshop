# 06 Advanced: exposed-crypt (01)

`exposed-crypt`를 사용해 컬럼 데이터를 투명하게 암복호화하는 모듈입니다. 민감 정보 저장 시 애플리케이션 코드 변경을 최소화하는 패턴을 다룹니다.

## 학습 목표

- 암호화 컬럼 정의와 CRUD 패턴을 익힌다.
- DSL/DAO 경로에서 암호화 컬럼 사용법을 이해한다.
- 검색 제약(결정적 암호화 아님)과 대안을 정리한다.

## 선수 지식

- [`../05-exposed-dml/02-types/README.md`](../05-exposed-dml/02-types/README.md)

## 핵심 개념

- `encryptedVarchar`, `encryptedBinary`
- 알고리즘 선택(AES/GCM 등)
- 암호화 키 관리

## 예제 구성

| 파일                                  | 설명         |
|-------------------------------------|------------|
| `Ex01_EncryptedColumn.kt`           | DSL 암호화 컬럼 |
| `Ex02_EncryptedColumnWithEntity.kt` | DAO 암호화 컬럼 |

## 실행 방법

```bash
./gradlew :01-exposed-crypt:test
```

## 실습 체크리스트

- 평문/암호문 저장 결과를 비교한다.
- 키 변경 시 복호화 실패 시나리오를 점검한다.

## 성능·안정성 체크포인트

- 키/시크릿은 코드가 아닌 외부 설정으로 관리
- 검색 필요 필드는 결정적 암호화 모듈과 분리 검토

## 다음 모듈

- [`../02-exposed-javatime/README.md`](../02-exposed-javatime/README.md)
