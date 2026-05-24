# 07 Routing DataSource Ktor

English | [한국어](./README.ko.md)

This module demonstrates explicit read/write datasource routing with Ktor request handling and Exposed JDBC. `RoutingDataSourcePlugin` selects `READ` for `GET` requests and `WRITE` for mutating requests, while `X-Data-Source` can override representative requests for tests and diagnostics.

## Architecture Diagram

![Ktor routing datasource architecture](../../docs/images/readme-diagrams/11-high-performance-07-routing-datasource-ktor-architecture-01.png)

## Routes

| Route | Default datasource | Behavior |
|---|---|---|
| `GET /inventory/{sku}` | `READ` | Reads from the replica-like datasource. |
| `PUT /inventory/{sku}` | `WRITE` | Writes through the primary-like datasource. |
| `GET /routing/stats` | Public | Exposes read/write selection counters. |

Use `X-Data-Source: write` to force a read route onto the write datasource when validating routing behavior.

## Verification

```bash
./gradlew :07-routing-datasource-ktor:test
```

Use this example when a Ktor service needs datasource selection without Spring transaction routing.
