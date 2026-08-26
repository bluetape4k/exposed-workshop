# Issue #236 checkpointable JDBC batch 예제 lesson

## 배경

JDBC batch의 chunk 경계와 재시작 checkpoint를 학습자가 직접 확인할 수 있는
Exposed workshop을 추가했다. R2DBC는 별도 저장소의
`exposed-r2dbc-workshop#205`로 분리해 두 저장소의 실행 모델과 의존성 경계를
섞지 않았다.

## 결정

- `ExposedJdbcBatchReader`의 `Long` keyset과
  `ExposedJdbcBatchJobRepository`의 metadata table을 provider DSL에서 직접
  조합한다.
- target `sourceId`를 primary key로 두어 재시작 시 중복 write 가능성을
  관찰할 수 있게 하고, exactly-once를 구현한 것처럼 표현하지 않는다.
- H2에서 정상 완료, skip/retry/timeout, FAILED 경계, cancellation 후
  `STOPPED` 재시작을 결정적으로 검증한다. provider의 FAILED checkpoint 삭제
  동작은 #745 후속 이슈로 남기고 workshop에서 우회하지 않는다.
- 테스트는 `runSuspendIO`로 실행한다. provider가 virtual-thread dispatcher를
  사용하므로 `runTest`의 가상 시간과 실제 timeout을 섞지 않는 것이 안정적이다.
- module README만 추가하지 않고 central catalog, chapter/root index,
  changed-examples selector, paired diagrams, 표준 test resources를 한 번에
  닫는다.

## 검증

- H2 module test `8 passing`.
- module `build`, Kover XML report, repository detekt, project discovery,
  module-only path selection 성공.
- English/Korean README terminology audit와 SVG/PNG semantic·geometry·visual
  검사가 성공.

## 다음 적용 원칙

새 batch 저장소 예제는 provider의 실제 commit/checkpoint 순서를 먼저 읽고,
재시작 경계가 깨지는 상태를 workaround로 감추지 않는다. 실행 모델이 다른
R2DBC 예제는 JDBC 모듈의 공통 facade로 합치지 않고 sibling repository의
독립 이슈와 README 경계로 관리한다. 짧은 timeout이나 cancellation 테스트는
dispatcher 특성을 확인한 뒤 테스트 harness를 선택한다.

이번 구현에서 확인된 provider 결함 후속 작업은
[bluetape4k-exposed#745](https://github.com/bluetape4k/bluetape4k-exposed/issues/745)에
등록되어 있다.
