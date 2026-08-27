# 이슈 #238 Exposed measured JDBC 예제 lesson

## 배경

`06-advanced`에 `bluetape4k-exposed-measured` provider를 사용하는 JDBC
측정값 예제를 추가했다. 길이·질량·절대온도를 `ProductTable`과
`ProductEntity`로 노출하고, R2DBC 구현은 `exposed-r2dbc-workshop` 저장소의
별도 작업으로 남겼다.

## 재사용할 결정

1. provider가 `MeasureColumnType`과 온도 column type의 `DOUBLE` 직렬화,
   기준 단위 변환, DB 값 복원을 소유한다. workshop은 DSL/DAO 사용법만
   보여 주며 column type을 복제하지 않는다.
2. 저장 기준 단위는 provider 계약인 meter·kilogram·Kelvin으로 고정한다.
   README와 ERD에 표시 단위 metadata를 저장하지 않는다는 점과 기준 단위
   변경 시 migration 판단이 필요하다는 점을 함께 기록한다.
3. `DOUBLE`은 금융 또는 법정 계량 정확도 계약이 아니므로 raw equality 대신
   `shouldBeNear`와 값별 허용 오차를 사용한다. nullable 질량은 NULL 왕복을
   별도로 확인한다.
4. `Measure<Length>`와 `Measure<Mass>`, `Temperature`와
   `TemperatureDelta`의 계열 경계는 runtime cast가 아니라 Kotlin
   compile-time 예제로 설명한다. provider 책임인 비수치 `valueFromDB` 실패
   경계는 source ledger와 README에 기록하고 정상 JDBC 경로에 인위적인
   driver 출력을 주입하는 테스트는 중복하지 않는다.
5. Exposed 예제 문서의 코드 블록은 실제 테이블 컬럼과 non-null `name`을
   포함해야 한다. EN/KO README, SVG/PNG, semantic ledger를 한 변경으로
   갱신하고, ledger의 중첩 source도 실제 repo-relative path를 사용한다.

## 검증 교훈

- 첫 독립 아키텍처 검토에서 km/g/K round-trip과 계열 compile-time 예가
  빠진 사실을 발견했다. 표시 단위 표만 작성하는 것으로는 충분하지 않으며,
  각 대표 단위를 실제 dialect parameterized test와 문서 예제에 연결해야 한다.
- 파일명을 detekt 규칙에 맞춰 `Ex01MeasuredColumns.kt`로 정리한 뒤, PNG와
  semantic ledger의 오래된 `Ex01_MeasuredColumns.kt` 참조를 함께 갱신해야
  했다. source ledger는 top-level path뿐 아니라 node/edge의 source도
  실제 파일을 가리켜야 한다.
- README의 상대 이미지 경로와 DSL insert 예제를 독립 코드 리뷰에서 다시
  읽으면, 문서가 build에 직접 포함되지 않아도 경로·필수 컬럼 누락을 조기에
  잡을 수 있다.
- 승인 spec과 초기 계획에 provider 경계 테스트 문구가 남아 있었지만,
  승인된 설계의 “인위적인 assertFailsWith를 중복하지 않는다”는 범위가 더
  구체적이었다. 계획·spec·verification을 source contract 문서화로 일치시키고
  테스트 수를 default 12, fast H2 4로 다시 검증했다.
- 전체 repository fast test는 bounded local window 안에 결론을 반환하지
  않았다. 이를 성공으로 추정하지 않고 module/neighbor/detekt/assemble 증거와
  CI matrix gate로 분리해 기록한다.

## workflow 증적

단일 개발자 `issue-238-feature-r1` lane은 recovery와 main takeover 후에도
동일 worktree와 committed checkpoint를 유지했다. review lane lease 만료는
receipt diagnosis → recovery → bounded probe ACK 절차로 복구했으며, 구현
범위를 넓히거나 원래 변경을 되돌리지 않았다. 최종 구현 checkpoint는
`cd97a3e4`다.

최종 근거 명령은 다음과 같다.

```bash
./gradlew :13-exposed-measured:test --no-build-cache --no-daemon --no-configuration-cache
./gradlew :13-exposed-measured:test -PuseFastDB=true --no-build-cache --no-daemon --no-configuration-cache
./gradlew :13-exposed-measured:test :06-custom-columns:test :04-exposed-json:test -PuseFastDB=true --no-build-cache --no-daemon --no-configuration-cache
./gradlew :13-exposed-measured:detekt --no-build-cache --no-daemon --no-configuration-cache
./gradlew detekt --no-build-cache --no-daemon --no-configuration-cache
./gradlew assemble -PuseFastDB=true --no-build-cache --no-daemon --no-configuration-cache
git diff --check
```

모듈 테스트는 기본 12건, H2 fast 4건이 실패·오류·skip 0으로 통과했다.
인접 모듈 합계는 52건이며 실패·오류 0, 기존 JSON skip 18건이다. provider
`1.12.1` 해석, Gradle project discovery, module/root detekt, assemble과
semantic/asset/geometry/visual 문서 감사도 통과했다.

## 후속 지침

새 측정값 column 예제를 추가할 때는 provider source와 catalog alias를 먼저
확인하고, 기준 단위·정밀도·nullable 계약을 테스트와 한영 README에 동시에
반영한다. absolute temperature와 temperature delta를 같은 컬럼 타입으로
합치지 않으며, R2DBC 조건부 코드를 이 저장소에 추가하지 않는다.
