# Issue 139 Trino Session Options Lesson

## Context

Issue #139 adds a credential-free Trino workshop example under Chapter 13.

## Decision

Keep the workshop on public APIs: validate an application-facing profile, convert it to `TrinoConnectionOptions`, and expose only a local JDBC-property preview for README and tests. Do not call internal property conversion helpers from bluetape4k-exposed.

## Outcome

The example verifies typed options and EXPLAIN request shape locally. It does not require a Trino endpoint or credentials, and it does not assert connector-specific pushdown behavior.

## Future Guidance

For a real Trino lane, use explicit opt-in tests and compare stable EXPLAIN fragments for a known connector. Avoid full-plan snapshots because Trino plans vary by connector, version, and catalog settings.
