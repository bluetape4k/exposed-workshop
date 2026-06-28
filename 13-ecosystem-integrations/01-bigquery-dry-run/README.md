# BigQuery Dry-Run Query Validation

English | [한국어](README.ko.md)

This example shows how to validate an Exposed-generated analytical query with
BigQuery dry-run semantics before any billable query execution.

![BigQuery dry-run flow with mocked BigQuery REST response](../../docs/images/readme-diagrams/01-bigquery-dry-run-flow-01.png)

The diagram shows the local-only path: Exposed builds a query, H2 is used only
as the SQL-generation dialect, `BigQueryContext.validateQuery` creates a dry-run
request, and a mocked BigQuery REST response drives the workshop assertions.

## Purpose

The module focuses on the boundary between Exposed SQL generation and the
BigQuery `jobs.query` request. It is useful when an application wants to check a
warehouse read model for parser errors, option mapping, and estimated safety
limits before the application ever runs a real BigQuery job.

## Dry Run vs Execution

A BigQuery dry run validates a query request without producing result rows or
charging for query execution. The workshop calls
`BigQueryContext.validateQuery`, which converts the Exposed `Query` to SQL and
sets `dryRun=true` on the outgoing `QueryRequest`.

This module does not execute a real BigQuery query.

## Credential-Free Command

Run the example tests:

```bash
./gradlew :01-bigquery-dry-run:test
```

Expected result: the command uses only an H2 SQL-generation database plus mocked
BigQuery REST calls, and it passes without `GOOGLE_APPLICATION_CREDENTIALS`.

## No Cloud Credential Guarantee

The default path does not read Application Default Credentials, service-account
files, project secrets, endpoint overrides, tokens, API keys, environment
variables, or system properties. Tests construct a mocked `Bigquery` service and
capture the `QueryRequest` sent through `Bigquery.Jobs.query`.

Placeholder project and dataset IDs are test constants used only to verify
request mapping.

## Tested Behavior

The tests verify that:

- Exposed generates grouped analytical SQL for the `events` table.
- `dryRun=true` and `useLegacySql=false` are applied.
- default dataset project and dataset IDs are mapped.
- maximum billed bytes, labels, priority, location, and timeout are mapped.
- a successful dry run returns the mocked response.
- BigQuery validation errors are surfaced as `BigQueryQueryException`.

## Real BigQuery Out of Scope

Real BigQuery execution, ADC setup, service-account files, endpoint overrides,
and manual opt-in cloud tests are intentionally out of scope for issue #138.
Open a separate issue if a future example needs an explicit real-service lane.
