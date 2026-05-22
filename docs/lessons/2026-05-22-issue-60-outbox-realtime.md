# Issue 60 - Realtime Outbox Examples

## Context

Issue #60 adds paired Spring Boot 4 and Ktor examples for database-backed realtime
delivery in chapter 12. The user also required every example README to include a
PNG Architecture Diagram.

## Decision

Use separate `07-spring-outbox-realtime` and `08-ktor-outbox-realtime` modules to
avoid collisions with existing #59 worktree module numbers. Keep the examples
small: persist a notification row and an outbox row in one Exposed transaction,
then publish pending rows through an in-process SSE/WebSocket hub.

## Outcome

The Spring example uses WebFlux Server-Sent Events for one-way notification
delivery. The Ktor example uses WebSockets for reconnect/replay and future
bidirectional command room. Both examples record delivery failure as outbox
state instead of dropping the event.

## Verification

- `./gradlew :07-spring-outbox-realtime:test :08-ktor-outbox-realtime:test --stacktrace --continue`
- README PNG links validated by script.
- PNG diagrams rendered from SVG and visually checked.
- Claude advisor review initially found `P0=0 P1=4`; Spring SSE dedupe,
  Spring/Ktor live endpoint tests, and FAILED-row README semantics were fixed.
- Claude advisor rerun with stdin and a 6-minute timeout returned `P0=0 P1=0`.

## Future Guard

When multiple issue branches add chapter 12 modules independently, avoid reusing
the same numeric module prefix. Reconcile final ordering only during integration.

For Claude Code CLI advisor gates, use stdin-compatible invocation and allow at
least five minutes for review prompts; shorter RPC/tool timeouts can kill valid
reviews before Claude responds.
