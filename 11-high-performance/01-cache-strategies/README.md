# 캐시 전략 (Caching Strategies)

다양한 캐시 전략에 대해 Redisson + Exposed 로 구현한 예제를 제공합니다. 가장 많이 사용하는 Cache Aside 전략은 09-Spring/06-spring-cache 에 있습니다. 여기에는 Read Through, Write Through, Write Behind 전략에 대해 설명하도록 하겠습니다.

### Read Through

Client <- Cache <- DB 로 Client 가 정보를 얻을 때, Cache 에서 정보를 가져오고 Cache 에 없을 경우 DB 에서 가져와 Cache 에 저장하는 전략입니다.

### Write Through

Client -> Cache -> DB 로 Client 가 정보를 저장할 때, Cache 에 저장하고 DB 에도 저장하는 전략입니다.

### Write Behind

Client -> Cache ---> DB 로 Client 가 정보를 저장할 때, Cache 에 저장하고 DB 에는 비동기적으로 debounce 나 배치로 저장하는 전략입니다.

## 참고

- [캐시 전략들 by Perplexity](https://www.perplexity.ai/search/kaesi-jeonryagdeulyi-teugjinge-JAF35te5SnWTUBsQg5JGSg)
- [Caching patterns](https://docs.aws.amazon.com/whitepapers/latest/database-caching-strategies-using-redis/caching-patterns.html)
- [A Hitchhiker's Guide to Caching](https://hazelcast.com/blog/a-hitchhikers-guide-to-caching-patterns/)
- [Understanding Cache Strategies](https://www.linkedin.com/pulse/decoding-cache-chronicles-understanding-strategies-aside-gopal-kb9kf/)
