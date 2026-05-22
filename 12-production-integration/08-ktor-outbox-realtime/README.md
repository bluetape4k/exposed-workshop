# Ktor WebSocket Realtime Outbox

[English](README.md) | [한국어](README.ko.md)

This example shows a database-backed realtime notification flow with Ktor
WebSockets and Exposed. A notification request writes the domain row and the
outbox row first; a separate publish step emits JSON frames through a WebSocket
hub.

## Architecture Diagram

![Ktor realtime outbox architecture](../../docs/images/readme-diagrams/12-production-integration-08-ktor-outbox-realtime-architecture-01.png)

## Flow

1. `POST /notifications` validates the request and inserts both `notifications`
   and `realtime_outbox` rows in one Exposed transaction.
2. `POST /outbox/publish` dispatches pending events through `RealtimeHub`.
3. Successful delivery marks rows `PUBLISHED`; failed delivery marks rows
   `FAILED` without deleting the outbox event.
4. `WS /events?after={eventId}` replays published events after the client
   boundary and then streams live JSON frames.

## Ktor Tradeoffs

Ktor WebSockets are a better fit than SSE when the client may later need
bidirectional messages, subscription commands, or application-level heartbeat
handling. They require a WebSocket-capable client and more explicit reconnect
handling than HTTP SSE.

The example uses an in-process `SharedFlow` hub to keep the outbox pattern small.
Production deployments should use a broker or a multi-instance outbox worker for
cross-node delivery.

`FAILED` rows are intentionally left for operator intervention in this compact
example. Add a retry policy, claim lease, and attempt limit before using the
pattern as an automated at-least-once delivery worker.

## Run

```bash
./gradlew :08-ktor-outbox-realtime:run
```

## Verification

```bash
./gradlew :08-ktor-outbox-realtime:test
```

The tests cover event persistence before delivery, WebSocket replay plus live
streaming after a reconnect boundary, and delivery failure recording.
