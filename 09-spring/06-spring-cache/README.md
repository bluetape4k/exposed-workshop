# 09 Spring: Spring Cache (06)

Spring Cache와 Exposed를 통합하는 모듈입니다.
`@Cacheable`, `@CachePut`, `@CacheEvict`를 사용한 기본 캐시 패턴을 다룹니다.

## 학습 목표

- 선언적 캐시 적용 패턴을 익힌다.
- 캐시 히트/미스/무효화 흐름을 이해한다.
- 캐시로 인한 정합성 리스크를 관리한다.

## 선수 지식

- [`../04-exposed-repository/README.md`](../04-exposed-repository/README.md)

## 핵심 개념

- 캐시 키 설계
- 읽기 캐시 최적화
- 변경 시 무효화

## 실행 방법

```bash
./gradlew :exposed-09-spring-06-spring-cache:test
```

## 실습 체크리스트

- 동일 요청 반복 시 DB 조회 감소를 확인
- 변경/삭제 시 캐시 무효화 누락 여부를 검증

## 성능·안정성 체크포인트

- 캐시 TTL과 데이터 신선도 요구를 일치
- 캐시 장애 시 DB 폴백 경로를 확인

## 다음 모듈

- [`../07-spring-suspended-cache/README.md`](../07-spring-suspended-cache/README.md)
