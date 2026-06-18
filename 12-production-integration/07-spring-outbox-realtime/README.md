# Spring Boot 4 Realtime Outbox

English | [한국어](README.ko.md)

This example shows a database-backed realtime notification flow with Spring Boot 4,
Spring WebFlux, and Exposed. A notification request writes the domain row and the
outbox row first; a separate publish step reads pending outbox rows and emits
Server-Sent Events.

## Architecture Diagram

![Spring Boot realtime outbox architecture](../../docs/images/readme-diagrams/12-production-integration-07-spring-outbox-realtime-architecture-01.png)

## Flow

1. `POST /notifications` validates the request and inserts both `notifications`
   and `realtime_outbox` rows in one Exposed transaction.
2. `POST /outbox/publish` dispatches pending events through `RealtimeHub`.
3. Successful delivery marks rows `PUBLISHED`; failed delivery marks rows
   `FAILED` with the error boundary preserved in the outbox.
4. `GET /events?after={eventId}` returns published replay events and then keeps
   an SSE stream open for live events.

## Spring Boot 4 Tradeoffs

Spring WebFlux SSE is simple for browser and HTTP clients because it keeps the
delivery protocol as HTTP. It is a good fit for one-way notification streams.
Bidirectional workflows should use WebSocket or a broker-backed protocol instead.

The example keeps delivery in-process to make the Exposed outbox boundary easy
to inspect. Production deployments should run a scheduler or worker that claims
outbox rows safely across instances.

`FAILED` rows are intentionally left for operator intervention in this compact
example. Add a retry policy, claim lease, and attempt limit before using the
pattern as an automated at-least-once delivery worker.

## Run

```bash
./gradlew :07-spring-outbox-realtime:bootRun
```

## Verification

```bash
./gradlew :07-spring-outbox-realtime:test
```

The tests cover event persistence before delivery, publish success, SSE replay
plus live streaming after a reconnect boundary, and delivery failure recording.
