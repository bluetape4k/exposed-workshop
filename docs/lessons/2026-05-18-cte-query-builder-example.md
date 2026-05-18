# CTE Query Builder Example

## Context

`bluetape4k-exposed` published the `CteTable` and JDBC `withCte` APIs as
`1.8.1-SNAPSHOT`, so the workshop can demonstrate CTEs without raw SQL.

## Lesson

Keep workshop snapshot adoption narrow. `exposed-workshop` still depends on the
main `bluetape4k` line for general utilities, while Exposed artifacts can move
through a dedicated `bluetape4k-exposed` version key.

## Evidence

- Added `Ex51_CteQueryBuilder` beside the existing raw SQL `Ex50_RecursiveCTE`.
- Verified `CteTable` and `withCte` with H2 through the `:01-dml:test` fast path.

## Future Guard

When adding examples for a freshly published snapshot API, separate library-line
versions if only one artifact family needs the snapshot.
