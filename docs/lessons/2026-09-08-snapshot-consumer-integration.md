# SNAPSHOT 소비선 전환 시 검사 기준도 함께 갱신한다

## 놓친 검증과 결정

[PR #274](https://github.com/bluetape4k/exposed-workshop/pull/274)는 BOM을
`2.1.0-SNAPSHOT`으로 전환했지만 `gradle/dependency-governance.sh`의 허용
버전은 `2.0.0`으로 남아 있었다. Gradle 컴파일과 기존 CI 성공만으로 소비선
전환이 끝났다고 판단한 것이 누락의 원인이었다. 독립 통합 리뷰 후 스크립트를
실행하자 `expected=2.0.0 actual=2.1.0-SNAPSHOT` 실패가 재현됐다.

BOM 허용 버전만 요청된 개발선으로 변경하고 나머지 25개 의존성 검사 기준은
유지했다. tenant의 안정 버전 설명과 JaVers의 과거 `0.3.0` 표기도 현재
소비 좌표와 일치시켰다. 새로운 Kotlin 또는 라이브러리 업그레이드는 하지 않았다.

## 상류 변경 보존과 결과

검증 대기 중 [PR #275](https://github.com/bluetape4k/exposed-workshop/pull/275)가
머지되어 카탈로그 충돌이 발생했다. 상류 tenant JDBC 공용 API와
`2.1.0-20260907.153611-1` 고정 버전 및 alias를 보존하고 BOM 전환을 통합했다.
원격 이력을 강제 재작성하지 않았다.

- `bash gradle/dependency-governance.sh`: 보정 전 BOM 검사 실패, 보정 후 26항목 통과.
- `bash -n gradle/dependency-governance.sh`, `git diff --check`: 통과.
- `:tenant-jdbc-support:test`: 4개 테스트 통과.
- 두 Spring tenant 예제의 `compileTestKotlin`: 통과.
- 독립 통합 및 후속 수정 리뷰: CLEAR. 최종 head의 hosted CI와 새 머지 승인은 별도 증거다.

## 다음 전환의 필수 확인

1. 카탈로그 버전을 바꾸면 README에 명시된 governance 명령도 실제로 실행한다.
2. 현재 사용 안내의 좌표와 안정·SNAPSHOT 표현을 EN/KO 모두 대조한다. 과거 완료 기록은 보존한다.
3. base 변경을 통합할 때 상류 구현과 의도된 고정 버전을 양쪽 부모와 대조한다.
4. 통합 또는 문서 보정으로 head가 바뀌면 최종 SHA의 CI를 확인하고 새 머지 승인을 받는다.
