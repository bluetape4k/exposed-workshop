# 이슈 #239 JaVers + Exposed 감사 이력 예제 작업 교훈

## 배경

새 JDBC 예제에서 Exposed DAO 변경을 JaVers 변경 기준 데이터·diff·history로 노출하고,
actor/requestId 메타데이터·롤백 원자성·민감정보 제외를 함께 보여줘야 했다.
R2DBC는 이 저장소의 범위가 아니므로 `exposed-r2dbc-workshop#235`에 남겼다.

## 결정과 효과

- `IntEntity`의 `EntityHook`에 JaVers provider를 연결하고, 감사 주체와 요청 식별자를
  명시적인 `ThreadLocal` 컨텍스트로 전달했다. 컨텍스트가 없으면 조용히 익명 기록을
  만들지 않고 실패하도록 해 예제의 안전한 기본값을 고정했다.
- 감사 대상은 `AuditedCustomer`라는 별도 read model로 만들고 `secret`을 타입 수준에서
  제외했다. 저장소 raw `state`와 `changedProperties`를 직접 확인하는 테스트를 추가해
  직렬화 경로에서 민감정보가 다시 들어오지 않음을 증명했다.
- 각 테스트에 고유 H2 데이터베이스와 새 Javers/repository 인스턴스를 사용했다. provider가
  내부적으로 head를 캐시하는 특성 때문에 테스트 간 공유 인스턴스는 순서 의존성을 만들 수
  있다는 점을 설계 단계에서 차단했다.
- 스키마 준비는 교육용 `ensureSchema()`로 한정하고, 운영 마이그레이션 소유권은 애플리케이션에
  남겼다. README에 이 경계를 명시해 예제를 운영용 자동 마이그레이션으로 오해하지 않게 했다.
- provider의 전역 hook은 entity class만으로 이벤트를 매칭하므로, 구독 시 database identity를
  함께 고정하고 다른 `Database` 이벤트를 fail-closed 처리했다. 교차 database 부정 테스트로
  업무·감사 행이 모두 남지 않는 것을 확인했다.

## 검증에서 발견한 놀라움

- 한국어 SVG를 기본 글꼴로 렌더링하면 글자가 네모로 표시됐다. EN/KO 자산 모두에
  `Apple SD Gothic Neo`, `Noto Sans KR` 글꼴 대체 목록을 지정하고 PNG를 다시 렌더링한
  뒤 시각 검사를 통과시켰다.
- 공유 다이어그램 디렉터리 전체에 `--require-all-referenced`를 적용하면 기존 예제의
  미참조 자산 때문에 실패한다. 따라서 모듈 README 링크·SVG/PNG 쌍을 기준으로 검증하고,
  전역 미참조 자산 실패를 새 모듈의 결함으로 오판하지 않았다.
- 최초 Gradle 테스트 컴파일은 색인 단계에서 시간 제한에 걸렸지만, 동일 명령을 재실행해
  전체 테스트 성공과 `BUILD SUCCESSFUL`을 확인했다. 일시적 실행 중단과 실제 회귀를
  구분하려면 재실행 결과를 최종 증거로 남겨야 한다.
- 독립 보안 검토에서 raw `javers.commit`, actor 위조, 이력 조회 인가 부재, 중복 전역 hook의
  운영 위험을 확인했다. 이 교육 예제에서는 직접 commit·production endpoint 사용을 금지하고,
  actor 인증·customer/tenant 권한·database별 단일 subscription 소유권을 호출자 계약으로
  문서화했다. 다중 테넌트 registry 자체는 별도 설계 과제로 남긴다.

## 다음 작업을 위한 방어선

- JaVers provider 버전은 중앙 BOM과 `dependencyInsight` 결과를 함께 확인한다.
- 감사 컨텍스트 테스트에는 정상 종료·중첩·예외·누락의 네 경로를 모두 유지한다.
- 민감 필드는 도메인 테이블에 추가될 때마다 감사 read model과 raw 상태 데이터 검사를 함께
  갱신한다.
- 예제 README는 EN/KO 문서와 SVG/PNG 자산을 동시에 갱신하고, 선택기·CI 경로 포함 여부를
  `select-changed-examples.sh`와 `actionlint`로 확인한다.
- 원격 데이터베이스·성능·R2DBC 요구를 이 JDBC 교육 예제에 섞지 않고 별도 이슈/저장소로
  분리한다.
