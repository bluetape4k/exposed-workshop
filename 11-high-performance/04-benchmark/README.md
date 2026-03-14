# 벤치마크 (04-benchmark)

11장의 캐시/라우팅 예제를 대상으로 `kotlinx-benchmark` 기반 마이크로벤치마크를 수행하는 모듈입니다. 빠른 smoke 프로파일과 기본 main 프로파일을 제공하며, 실행 결과를 Markdown 표로 저장할 수 있습니다.

## 측정 대상

- `ContextAwareRoutingKeyResolver.currentLookupKey()` 라우팅 키 계산 비용
- Caffeine 기반 read-through 캐시의 DB 직접 조회 / cache hit / cache miss 경로

## 실행 방법

```bash
# 빠른 smoke 실행
./gradlew :04-benchmark:smokeBenchmark

# 기본 프로파일 실행
./gradlew :04-benchmark:benchmark

# Markdown 리포트 생성 (기본 main)
./gradlew :04-benchmark:benchmarkMarkdown

# smoke 결과를 Markdown 으로 저장
./gradlew :04-benchmark:benchmarkMarkdown -PbenchmarkProfile=smoke
```

## 결과 파일

- JSON: `11-high-performance/04-benchmark/build/reports/benchmarks/<profile>/.../jvm.json`
- Markdown: `11-high-performance/04-benchmark/build/reports/benchmarks/<profile>/benchmark-report.md`

## 해석 포인트

- `RoutingKeyResolverBenchmark`는 테넌트 유무와 read-only 여부가 라우팅 키 계산에 미치는 오버헤드를 비교합니다.
- `ReadThroughCacheBenchmark`는 동일 payload 크기에서 cache hit와 cache miss의 차이를 확인하는 용도입니다.
- 마이크로벤치마크 결과는 절대값보다 상대 비교와 추세 확인에 활용하는 것이 좋습니다.
