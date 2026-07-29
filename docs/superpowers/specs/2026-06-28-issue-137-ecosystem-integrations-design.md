# Issue #137 Ecosystem integrations chapter 설계

## 배경

Issue #137은 `bluetape4k-exposed 1.11.0` 및 인접 DDD/Modulith workshop example의 parent
epic이다. Child issue #138-#145는 database platform integration, explicit Ktor integration,
Spring Modulith publication storage, DDD aggregate/module-boundary example을 다룬다.

현재 repository에는 production-style Spring Boot 4/Ktor service pattern을 위한
`12-production-integration`이 이미 있다. 그 장은 HTTP entrypoint, service boundary,
outbox/auth/realtime/observability, 작은 production-facing application을 다룬다. BigQuery,
Trino, StarRocks, CockroachDB, DDD, Modulith 예제를 12장에 넣으면 runtime production concern과
ecosystem/dialect/framework integration concern이 한 장에 섞인다.

채택된 chapter name은 `13-ecosystem-integrations`다.

## 목표

- Ecosystem-level Exposed example을 위한 새 chapter boundary를 추가한다.
- Issue #137은 parent epic으로 유지하고 #138-#145는 implementable child example로 둔다.
- Child issue 구현 전에 future module order를 명시한다.
- 새 chapter를 root documentation 및 workflow path filter에 연결해 향후 child PR이 chapter를
  수정할 때 기존 Examples gate가 trigger되게 한다.
- Child example implementation이 build되기 전에 이미 존재하는 것처럼 보이지 않게 한다.

## 비목표

- 이 foundation PR에서 BigQuery, Trino, CockroachDB, StarRocks, Ktor, Modulith, DDD child
  example을 구현하지 않는다.
- Child issue chain이 완료되거나 사용자가 scaffold를 epic closeout으로 취급하겠다고 명시하기
  전까지 #137을 닫지 않는다.
- Ad hoc dependency version을 추가하지 않는다. Future child module은 기존 catalog/BOM
  convention을 계속 사용해야 한다.
- 현재 12장 모듈을 이동하지 않는다.

## 현재 근거

- `settings.gradle.kts`는 `includeModules("<chapter>", false, false)`를 통해 chapter
  directory를 포함한다.
- `12-production-integration/README.md`는 12장을 Spring/Ktor production service pattern
  coverage로 정의한다.
- `docs/lessons/2026-05-22-issue-63-chapter-12-wiring.md`는 chapter wiring이 root README pair,
  chapter README pair, workflow coverage, diagram asset을 갱신해야 한다고 기록한다.
- `docs/lessons/2026-06-06-issue-118-examples-weekly.md`는 downstream example이 weekly Examples
  workflow scope를 확장해야 한다고 기록한다.
- GitHub issue #137은 milestone `exposed-1.11.0` 아래 child example issue로 #138-#145를
  나열한다.

## 설계

`README.md`와 `README.ko.md`가 있는 chapter-level directory
`13-ecosystem-integrations/`를 만든다. Chapter README는 integration map을 설명하고, planned
child module을 implemented example이 아니라 issue-backed placeholder로 나열한다.

Child module이 존재하기 전까지 root README pair는 chapter overview만 link해야 한다. Planned
child example은 명시적 `Planned` status 및 GitHub issue link와 함께 chapter README pair에 둔다.

Planned child module order:

| Issue | Status | Planned directory | Gradle project/task | README title | Lane |
|---|---|---|---|---|---|
| #138 | Planned | `13-ecosystem-integrations/01-bigquery-dry-run` | `:01-bigquery-dry-run:build` | BigQuery Dry-Run Query Validation | Database platform adapters |
| #139 | Planned | `13-ecosystem-integrations/02-trino-session-options` | `:02-trino-session-options:build` | Trino Session Options and Pushdown Verification | Database platform adapters |
| #140 | Planned | `13-ecosystem-integrations/03-cockroachdb-retry` | `:03-cockroachdb-retry:build` | CockroachDB Serializable Retry | Database platform adapters |
| #141 | Planned | `13-ecosystem-integrations/04-starrocks-olap-local` | `:04-starrocks-olap-local:build` | StarRocks Local-First OLAP | Database platform adapters |
| #142 | Planned | `13-ecosystem-integrations/05-ktor-exposed-integration` | `:05-ktor-exposed-integration:build` | Explicit Ktor Exposed Integration | Runtime and framework integration |
| #143 | Planned | `13-ecosystem-integrations/06-spring-modulith-publications` | `:06-spring-modulith-publications:build` | Spring Modulith Publication Store with Exposed | Runtime and framework integration |
| #144 | Planned | `13-ecosystem-integrations/07-ddd-aggregate-repository` | `:07-ddd-aggregate-repository:build` | DDD Aggregate Lifecycle with Exposed Repository | Domain architecture |
| #145 | Planned | `13-ecosystem-integrations/08-ddd-modulith-boundaries` | `:08-ddd-modulith-boundaries:build` | DDD Bounded Context and Modulith Boundary Verification | Domain architecture |

Chapter를 다음 위치에 등록한다.

- `settings.gradle.kts` with `includeModules("13-ecosystem-integrations", false, false)`.
  이는 base-directory scan hook을 추가한다. `build.gradle.kts`가 있는 child module directory가
  생기기 전까지 Gradle project는 만들지 않는다.
- 새 module-list section이 있는 root `README.md`와 `README.ko.md`.
- Repo-local `AGENTS.md` layout table.
- `.github/workflows/examples.yml` path filter. 이를 통해 향후 chapter 13 module PR이 selected
  Examples gate를 trigger한다. 이 foundation PR은 hard-coded Examples build list에 존재하지 않는
  Gradle task를 추가하면 안 된다. 각 child module PR은 runnable module을 만들 때 해당 selected
  build list에 자체 Gradle task를 추가해야 한다.

Chapter-level architecture diagram 하나를 추가한다.

- `docs/images/readme-diagrams/13-ecosystem-integrations-architecture-01.svg`
- `docs/images/readme-diagrams/13-ecosystem-integrations-architecture-01.png`

Diagram은 세 lane을 보여 줘야 한다.

1. Database platform adapters: BigQuery, Trino, CockroachDB, StarRocks.
2. Runtime/framework integration: explicit Ktor and Spring Modulith.
3. Domain architecture: DDD aggregate lifecycle 및 Modulith boundary verification.

## External service 및 credential 정책

Future child module은 default safe 상태여야 한다.

- Checked-in credential, token, service-account file, project ID, endpoint secret 없음.
- Default test에서 ADC나 local credential file 같은 real cloud credential을 자동으로 사용하지
  않는다.
- Default test는 fake/local/Testcontainers/emulator-style path 또는 deterministic
  documentation-only check를 사용해야 한다.
- Real external-service execution은 explicit environment variable, Gradle property, test tag를
  통한 opt-in이어야 하며 CI default에서는 skip해야 한다.
- Child module PR은 selected workflow coverage가 weekly Examples gate, full Nightly matrix,
  explicit opt-in/manual lane 중 어디에 속하는지 문서화해야 한다. Default path는 local 및
  deterministic 상태로 남아야 한다.
- Cloud 또는 external-service example의 README file은 real-service command 전에 cost, network,
  credential warning을 포함해야 한다.

## 기각한 접근

### Extend `12-production-integration`

12장은 이미 명확한 production-service purpose를 가지므로 기각한다. Cloud warehouse, OLAP,
dialect, DDD, Modulith 예제를 거기에 추가하면 장이 너무 넓어지고 새로운 Exposed 1.11 ecosystem
theme가 숨겨진다.

### Name the chapter `13-exposed-1-11`

Version-named chapter는 빠르게 낡으므로 기각한다. Milestone과 issue metadata가 release context를
이미 보존한다. Chapter name은 durable learning theme를 설명해야 한다.

### Create empty child modules for #138-#145 now

Empty Gradle project는 runnable example이 존재한다고 암시하므로 기각한다. Child issue는 각
example을 구현할 때 test와 README pair가 있는 module을 만들어야 한다.

## 위험 및 완화

- Risk: documentation이 future example이 이미 구현됐다고 주장할 수 있다. 완화: 이를
  planned issue-backed example로 표시하고 issue number를 link한다.
- Risk: future chapter change가 Examples workflow를 실행하지 않을 수 있다. 완화:
  `13-ecosystem-integrations/**`를 Examples workflow path filter에 추가하고, child PR은 runnable
  module이 존재할 때만 hard-coded Gradle task entry를 추가하도록 요구한다.
- Risk: future cloud example이 real credential이나 billable service에 우발적으로 의존할 수 있다.
  완화: local/fake execution을 default로 만들고 real-service test는 opt-in 및 CI default
  skip을 요구한다.
- Risk: Gradle discovery가 증명되지 않을 수 있다. 완화: `settings.gradle.kts` registration
  추가 후 `./gradlew projects`를 실행하고, child module directory가 없으므로 foundation PR이 아직
  새 Gradle project를 만들지 않는다는 점을 기록한다.
- Risk: diagram asset이 README quality expectation을 만족하지 못할 수 있다. 완화: SVG를
  CairoSVG로 PNG rendering하고, SVG를 XML로 validate하며, PNG를 inspect하고, README reference가
  committed SVG/PNG sibling을 가리키는지 검증한다.

## 수용 기준

- `13-ecosystem-integrations/README.md`와 `README.ko.md`가 존재하고 chapter purpose를
  설명한다.
- Root `README.md`와 `README.ko.md`가 새 chapter를 나열한다.
- Root README entry는 child module이 존재하기 전까지 chapter overview만 link한다.
- Chapter README pair는 두 locale 모두에서 같은 planned issue table, language switch, diagram
  reference를 포함한다.
- `AGENTS.md`가 repo layout table에 chapter를 나열한다.
- `settings.gradle.kts`가 chapter base-directory scan hook을 추가한다.
- `.github/workflows/examples.yml`이 chapter 13 path filter를 포함한다.
- `.github/workflows/examples.yml`은 child module이 존재하기 전 missing chapter 13 Gradle task를
  참조하지 않는다.
- Future child module은 default credential-free 및 opt-in real-service execution only로
  문서화된다.
- Chapter diagram SVG/PNG가 commit되고 두 README locale에서 참조된다.
- Chapter diagram의 SVG XML validation이 통과한다.
- Chapter diagram에 대한 README reference가 committed file로 resolve된다.
- Chapter diagram dimension/viewBox는 README rendering에 적절하고 PNG는 시각적으로 검사된다.
- `./gradlew projects`가 성공한다. Expected foundation result는 child module이 생기기 전까지
  새 chapter 13 Gradle project가 없다는 것이다.
- `git diff --check`와 `actionlint .github/workflows/examples.yml`가 통과한다.
- README link/reference check는 새 root/chapter link를 다룬다.
- PR은 #137을 reference하지만 child issue도 완료되지 않는 한 close하지 않는다.
