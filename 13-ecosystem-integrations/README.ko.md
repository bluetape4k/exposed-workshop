# Chapter 13 - Ecosystem Integrations

[English](README.md) | 한국어

이 장은 epic
[#137](https://github.com/bluetape4k/exposed-workshop/issues/137) 아래에서
완료한 Exposed 1.11 및 인접 ecosystem 통합 예제를 담습니다. 운영형 서비스
패턴을 다루는 12장과 경계를 분리하며, 아래 모듈은 현재 소스 트리에서 실행할
수 있습니다. 향후 통합도 같은 경계에 추가합니다.

![Chapter 13 ecosystem integrations architecture](../docs/images/readme-diagrams/13-ecosystem-integrations-architecture-01.png)

## 학습 목표

- 데이터베이스 플랫폼 어댑터를 12장의 서비스 패턴과 분리합니다.
- Ktor, Spring Modulith, DDD, dialect 중심 예제를 위한 지속 가능한 장 경계를 둡니다.
- 장을 확장할 때 자식 모듈 순서와 검증 기대치를 계속 명확히 합니다.
- 모든 외부 서비스 예제는 기본값을 local, fake, 또는 명시적 opt-in으로 둡니다.

## 구현된 예제

| Issue | 상태 | 디렉터리 | Gradle task | README title | Lane |
|---|---|---|---|---|---|
| [#138](https://github.com/bluetape4k/exposed-workshop/issues/138) | 구현됨 | [`13-ecosystem-integrations/01-bigquery-dry-run`](01-bigquery-dry-run/README.ko.md) | `:01-bigquery-dry-run:build` | [BigQuery Dry-Run Query Validation](01-bigquery-dry-run/README.ko.md) | Database platform adapters |
| [#139](https://github.com/bluetape4k/exposed-workshop/issues/139) | 구현됨 | [`13-ecosystem-integrations/02-trino-session-options`](02-trino-session-options/README.ko.md) | `:02-trino-session-options:build` | [Trino Session Options and Pushdown Verification](02-trino-session-options/README.ko.md) | Database platform adapters |
| [#140](https://github.com/bluetape4k/exposed-workshop/issues/140) | 구현됨 | [`13-ecosystem-integrations/03-cockroachdb-retry`](03-cockroachdb-retry/README.ko.md) | `:03-cockroachdb-retry:build` | [CockroachDB Serializable Retry](03-cockroachdb-retry/README.ko.md) | Database platform adapters |
| [#141](https://github.com/bluetape4k/exposed-workshop/issues/141) | 구현됨 | [`13-ecosystem-integrations/04-starrocks-olap-local`](04-starrocks-olap-local/README.ko.md) | `:04-starrocks-olap-local:build` | [StarRocks Local-First OLAP](04-starrocks-olap-local/README.ko.md) | Database platform adapters |
| [#142](https://github.com/bluetape4k/exposed-workshop/issues/142) | 구현됨 | [`13-ecosystem-integrations/05-ktor-exposed-integration`](05-ktor-exposed-integration/README.ko.md) | `:05-ktor-exposed-integration:build` | [Explicit Ktor Exposed Integration](05-ktor-exposed-integration/README.ko.md) | Runtime and framework integration |
| [#143](https://github.com/bluetape4k/exposed-workshop/issues/143) | 구현됨 | [`13-ecosystem-integrations/06-spring-modulith-publications`](06-spring-modulith-publications/README.ko.md) | `:06-spring-modulith-publications:build` | [Spring Modulith Publication Store with Exposed](06-spring-modulith-publications/README.ko.md) | Runtime and framework integration |
| [#144](https://github.com/bluetape4k/exposed-workshop/issues/144) | 구현됨 | [`13-ecosystem-integrations/07-ddd-aggregate-repository`](07-ddd-aggregate-repository/README.ko.md) | `:07-ddd-aggregate-repository:build` | [DDD Aggregate Lifecycle with Exposed Repository](07-ddd-aggregate-repository/README.ko.md) | Domain architecture |
| [#145](https://github.com/bluetape4k/exposed-workshop/issues/145) | 구현됨 | [`13-ecosystem-integrations/08-ddd-modulith-boundaries`](08-ddd-modulith-boundaries/README.ko.md) | `:08-ddd-modulith-boundaries:build` | [DDD Bounded Context and Modulith Boundary Verification](08-ddd-modulith-boundaries/README.ko.md) | Domain architecture |
| — | 구현됨 | [`13-ecosystem-integrations/09-duckdb-embedded-analytics`](09-duckdb-embedded-analytics/README.ko.md) | `:09-duckdb-embedded-analytics:build` | [DuckDB Embedded Analytics with Exposed](09-duckdb-embedded-analytics/README.ko.md) | Database platform adapters |
| [#234](https://github.com/bluetape4k/exposed-workshop/issues/234) | 구현됨 | [`13-ecosystem-integrations/10-druid-query-only`](10-druid-query-only/README.ko.md) | `:10-druid-query-only:build` | [Apache Druid Query-Only Exposed](10-druid-query-only/README.ko.md) | Database platform adapters |
| [#236](https://github.com/bluetape4k/exposed-workshop/issues/236) | 구현됨 | [`13-ecosystem-integrations/11-checkpointable-batch`](11-checkpointable-batch/README.ko.md) | `:11-checkpointable-batch:build` | [재시작 가능한 Exposed JDBC Batch](11-checkpointable-batch/README.ko.md) | JDBC batch 실행 |
| [#239](https://github.com/bluetape4k/exposed-workshop/issues/239) | 구현됨 | [`13-ecosystem-integrations/12-javers-exposed-audit`](12-javers-exposed-audit/README.ko.md) | `:12-javers-exposed-audit:build` | [JaVers + Exposed 감사 이력](12-javers-exposed-audit/README.ko.md) | JDBC 감사 이력 |

## 외부 서비스와 credential 정책

현재 모듈은 다음 기본값을 사용하며, 향후 자식 모듈도 이를 유지해야 합니다.

- No checked-in credentials, tokens, service-account files, project IDs, or endpoint secrets.
- No default ADC or local credential file use.
- Fake, local, Testcontainers, or emulator-style defaults.
- Real-service execution is explicit opt-in and skipped by default in CI.
- README warnings for cost, network, and credentials before any real-service command.

## 자식 모듈 인계 기준

각 자식 이슈는 모듈 디렉터리와 `build.gradle.kts`를 만든 뒤 Gradle project
discovery를 검증하고, 실제 파일이 존재할 때 chapter/root README 링크를
추가했습니다. 향후 모듈도 같은 순서를 따르고, 자동 검증이 가능해지면 같은 PR에서
Examples 또는 Nightly workflow 실행 task를 함께 추가합니다.

자식 PR을 열기 전에 다음 lane 결정을 기록합니다.

| Field | Required decision |
|---|---|
| Default path | Local, fake, Testcontainers, emulator, or documentation-only |
| Real-service opt-in | Environment variable, Gradle property, test tag, or not applicable |
| Workflow lane | Weekly Examples, full Nightly, or manual opt-in |
