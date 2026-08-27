# 이슈 #237 Ktor 관측성 provider 전환 lesson

## 배경

`10-ktor-observability-readiness`가 로컬 `CallId`/`CallLogging` 설치와 UUID
정제를 중복 유지하고 있어, `bluetape4k-ktor-observability` 중앙 provider를
사용하도록 전환했다. 이번 issue의 구현 범위는 JDBC/H2 workshop 예제이며,
R2DBC 구현은 `exposed-r2dbc-workshop`에서 별도 관리한다.

## 재사용할 결정

1. 공통 관측성은 provider가 설치하고 애플리케이션은 `CorrelationIdSettings`,
   `ContentNegotiation`, `StatusPages`, 도메인 route만 소유한다. 공통 plugin을
   예제마다 복제하지 않는다.
2. request ID 계약은 provider의 실제 semantics를 테스트로 고정한다. 허용문자와
   길이 제한, 헤더 누락 시 생성 규칙, 응답 header 전파, call log 상관관계를
   각각 확인해야 한다.
3. `CancellationException`은 broad exception handler보다 먼저 rethrow한다.
   Ktor test engine에서는 cancellation trace가 500 HTML로 보일 수 있으므로,
   테스트는 구조화 `INTERNAL_ERROR`를 소비하지 않는다는 의미를 검증한다.
4. 전역 logger를 캡처하는 테스트는 `SAME_THREAD`와 명시적인 clear/close를
   사용한다. Ktor application logger가 root appender를 통과하면
   `LogCaptor.forRoot()`가 올바른 범위다.
5. README의 source-equivalent EN/KO 문서와 SVG/PNG semantic ledger를 함께
   갱신하고, SVG를 먼저 수정한 뒤 CairoSVG로 PNG를 재생성한다. geometry,
   endpoint, arrowhead, semantic, visual audit를 모두 실행한다.

## 검증 교훈

- RED 단계에서 기존 UUID 36자와 로컬 sanitizer 동작이 provider 계약과 다르다는
  사실을 먼저 드러내면, 구현이 테스트 기대를 맞추는 식으로 흐르지 않는다.
- version catalog alias는 전역적으로 재사용될 수 있으므로, 모듈의 직접 의존성과
  로컬 중복 구현만 정적 검색한다. 전역 alias를 삭제하면 unrelated 예제가 깨질
  수 있다.
- repository-wide detekt의 기존 finding과 이번 변경 finding을 분리해서 기록해야
  한다. 이번 모듈의 `DiagnosticsPersistence.kt:21` `MagicNumber`는 기존 baseline이라
  범위를 넓혀 수정하지 않았다.
- asset-pair helper의 `--require-all-referenced`는 저장소의 역사적 미노출
  다이어그램 때문에 실패할 수 있다. 새 README 참조와 SVG/PNG pair를 별도로
  검증하고, baseline 예외를 verifier에 남긴다.

## workflow 증적

단일 개발자 `issue-237-feature` lane은 `completed` 상태이며, `spec-plan`,
`module-test`, `static-check`, `docs-parity`, `workflow-registration`의 다섯
필수 check와 component coverage가 모두 통과했다. run은
`20260827T034929Z-4e6c565e`, 최종 receipt checksum은
`333256e7a9500b8b2c35cf502950c09f2b0bd957cd149149598376b259c45724`다.

최종 근거 명령은 다음과 같다.

```bash
USE_FAST_DB=true repo-test-summary -- ./gradlew :10-ktor-observability-readiness:test --no-build-cache --no-daemon --no-configuration-cache
./gradlew :10-ktor-observability-readiness:build --no-build-cache --no-daemon --no-configuration-cache
./gradlew :10-ktor-observability-readiness:detekt --no-build-cache --no-daemon --no-configuration-cache
./gradlew detekt --no-build-cache --no-daemon --no-configuration-cache
git diff --check
```

모듈 테스트는 12개 성공(실패 0, skip 0), build/Kover/detekt와 root detekt는
exit 0이다. 다이어그램은 semantic·connector·arrowhead·geometry·endpoint·
mixed-corner·sequence-style·opaque visual audit를 EN/KO 모두 통과했다.

## 후속 지침

새 Ktor workshop 예제가 추가될 때는 중앙 provider의 public API와 기본값을 먼저
확인하고, provider가 소유하는 책임을 로컬 plugin으로 되돌리지 않는다. R2DBC
예제가 필요하면 이 모듈에 조건부 코드를 넣지 말고 `exposed-r2dbc-workshop`의
동일한 문서·테스트·workflow 경계로 등록한다.
