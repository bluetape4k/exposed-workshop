# 09 Spring: Suspended Cache (07)

코루틴 기반 서비스에서 캐시를 적용하는 모듈입니다.
`suspend` 경로에서 캐시 일관성과 논블로킹 처리의 균형을 다룹니다.

## 학습 목표

- suspend 메서드에서 캐시 적용 전략을 익힌다.
- 코루틴 환경의 캐시 무효화 패턴을 이해한다.
- 비동기 경로의 일관성/성능 검증 기준을 수립한다.

## 선수 지식

- [`../05-exposed-repository-coroutines/README.md`](../05-exposed-repository-coroutines/README.md)
- [`../06-spring-cache/README.md`](../06-spring-cache/README.md)

## 핵심 개념

- suspend + cache 조합
- 비동기 캐시 갱신/무효화
- 컨텍스트 전파

## 실행 방법

```bash
./gradlew :07-spring-suspended-cache:test
```

## 실습 체크리스트

- 캐시된 suspend 경로의 응답 시간 변화를 측정
- 갱신/삭제 후 stale 데이터 노출 여부를 검증

## 성능·안정성 체크포인트

- 캐시 레이어의 블로킹 호출 여부 점검
- 코루틴 취소 시 캐시 상태 불일치가 없는지 확인

## 다음 챕터

- [`../10-multi-tenant/README.md`](../10-multi-tenant/README.md)
