# Issue #139 Trino session option workshop 설계

## 배경

Issue #139는 13장 ecosystem integration을 위한 epic #137의 두 번째 child다. 이전 child #138은
credential-free BigQuery dry-run 예제 `13-ecosystem-integrations/01-bigquery-dry-run`을
추가했다.

다음 planned module은 `13-ecosystem-integrations/02-trino-session-options`다. 이 모듈은
default CI에서 live Trino cluster를 요구하지 않고 `bluetape4k-exposed` Trino option surface를
보여 줘야 한다.

## 현재 근거

- `13-ecosystem-integrations/README.md`는 이미 #139를 `02-trino-session-options` 및 planned
  Gradle task `:02-trino-session-options:build`로 나열한다.
- `bluetape4k-exposed` issue #229는 `TrinoConnectionOptions`를 제공하고
  connector-specific pushdown verification을 문서화했다.
- `TrinoConnectionOptions` supports `explicitPrepare`, `encoding`,
  `validateConnection`, `source`, `clientTags`, `sessionProperties`,
  `extraCredentials`, and `extraHeaders`.
- JetBrains Exposed 1.3.0은 non-parameterized SQL string 생성을 위한
  `Query.prepareSQL(prepared = false)`를 문서화한다.
- Trino documentation은 pushdown을 connector-specific으로 취급한다. Stable check는 brittle
  full-plan snapshot 대신 `EXPLAIN` signal을 사용해야 한다.

## 설계 결정

Trino analytical connection profile과 pushdown-friendly query plan request를 model하는
local-only workshop module을 만든다.

1. Public typed option API로 `TrinoConnectionOptions`를 사용한다.
2. Example code가 `TrinoConnectionOptions`를 만들기 전에 catalog, schema, source, tag,
   session property를 검증할 수 있도록 application-facing `TrinoWorkshopConnectionProfile`을
   둔다.
3. H2 SQL-generation database를 대상으로 Exposed analytical SQL을 생성한다.
4. Generated SQL을 감싸는 `EXPLAIN` request string을 만들어 test가 live Trino cluster 없이
   predicate/top-N/projection shape를 검증할 수 있게 한다.
5. 실제 pushdown support는 target Trino catalog 또는 connector를 대상으로 확인해야 함을
   문서화한다.

## 기각한 대안

- Default test path로 Live Trino Testcontainers 사용: #139는 local container path가 안정적이지
  않다면 default test가 live cluster를 피해야 하므로 기각한다. Workshop은 나중에 opt-in
  real-service lane을 추가할 수 있다.
- Full `EXPLAIN` output snapshot: Trino plan text는 connector와 version에 민감하므로
  기각한다. 모듈은 stable request shape만 assert해야 한다.
- User code에서 raw JDBC property string 생성: 가르치려는 기능은 typed
  `TrinoConnectionOptions`이므로 기각한다. Raw string fragment는 README/debugging을 위한 좁은
  preview 뒤에 머물러야 한다.

## 수용 기준

- `:02-trino-session-options:test`는 live Trino cluster 없이 통과한다.
- Test는 typed option construction, unsafe value rejection, expected SQL/`EXPLAIN` request
  shape를 검증한다.
- `README.md`와 `README.ko.md`는 local validation과 real Trino connector validation의 차이를
  설명한다.
- Chapter README pair는 #139를 Ready로 표시하고 새 모듈을 link한다.
- Root README pair는 새 child module을 link한다.
- Rendered PNG sequence diagram과 adjacent SVG source를 다음 위치에 추가한다.
  `docs/images/readme-diagrams/`.
- `.github/workflows/examples.yml`은 `:02-trino-session-options:build`를 포함한다.
- Catalog dependency wiring은 기존 BOM convention을 사용하고 ad hoc version pin을 피한다.

## 위험

- Workshop이 real pushdown을 과장할 수 있다. 완화: 모든 prose는 local test가 request shape만
  검증한다고 명시해야 한다.
- `TrinoConnectionOptions.toProperties`는 library module 내부 API다. 완화: public data class
  field를 검사하고 educational assertion을 위한 workshop preview map을 제공한다.
- Generated SQL은 dialect별로 달라질 수 있다. 완화: exact SQL text가 아니라 stable clause
  (`SELECT`, `FROM`, `WHERE`, `ORDER BY`, `LIMIT`)를 assert한다.
