# 05 Exposed DML: Entity API (05-entities)

Exposed DAO(Entity) 모델을 학습하는 모듈입니다. 기본 CRUD부터 관계 매핑, 라이프사이클 훅, 캐시, 복합키까지 다룹니다.

## 학습 목표

- Entity/EntityClass 모델링 패턴을 익힌다.
- 다양한 PK 전략(Long/UUID/Composite)을 이해한다.
- 관계 매핑과 캐시/훅 사용 시 주의점을 학습한다.

## 선수 지식

- [`../01-dml/README.md`](../01-dml/README.md)
- [`../04-transactions/README.md`](../04-transactions/README.md)

## 핵심 개념

- Entity 생명주기: 생성/수정/삭제와 훅
- 관계 매핑: `referencedOn`, `referrersOn`, `via`, self-reference
- 키 전략: 자동 증가/수동 ID/UUID/복합키

## 예제 지도

소스 위치: `src/test/kotlin/exposed/examples/entities`

| 범주        | 파일                                                                                                                     |
|-----------|------------------------------------------------------------------------------------------------------------------------|
| 기본/라이프사이클 | `Ex01_Entity.kt`, `Ex02_EntityHook.kt`, `Ex02_EntityHook_Auditable.kt`, `Ex03_EntityCache.kt`                          |
| 키 전략      | `Ex04_LongIdTableEntity.kt`, `Ex05_UUIDTableEntity.kt`, `Ex06_NonAutoIncEntities.kt`, `Ex10_CompositeIdTableEntity.kt` |
| 확장        | `Ex07_EntityWithBlob.kt`, `Ex08_EntityFieldWithTransform.kt`, `Ex09_ImmutableEntity.kt`                                |
| 관계 매핑     | `Ex11_ForeignIdEntity.kt`, `Ex12_Via.kt`, `Ex13_OrderedReference.kt`, `Ex31_SelfReference.kt`                          |

## 실행 방법

```bash
./gradlew :exposed-05-exposed-dml-05-entities:test
```

## 실습 체크리스트

- 같은 도메인을 DSL 버전과 Entity 버전으로 각각 구현해 차이를 비교한다.
- 다대다(`via`)와 self-reference에서 조회 패턴/N+1 가능성을 점검한다.
- 훅(`beforeInsert`, `beforeUpdate`) 사용 시 부수효과를 최소화한다.

## DB별 주의사항

- ID 생성 전략은 DB/드라이버에 따라 성능과 동작 차이가 있음
- 복합키 모델은 쿼리 단순성보다 명시적 무결성에 초점을 두고 선택

## 성능·안정성 체크포인트

- 대량 조회/갱신에서는 DAO 남용보다 DSL 배치 접근을 고려
- Entity 캐시 사용 시 트랜잭션 경계 밖 참조를 피한다.
- 관계 로딩 전략을 명확히 하지 않으면 N+1 회귀 위험이 높다.

## 다음 챕터

- [`../06-advanced/README.md`](../06-advanced/README.md)
