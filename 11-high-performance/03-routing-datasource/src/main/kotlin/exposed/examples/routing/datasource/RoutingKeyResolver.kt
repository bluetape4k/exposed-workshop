package exposed.examples.routing.datasource

/**
 * 현재 실행 컨텍스트에서 사용할 라우팅 키를 계산합니다.
 */
fun interface RoutingKeyResolver {

    /**
     * 현재 컨텍스트에 대한 라우팅 키를 반환합니다.
     */
    fun currentLookupKey(): String
}

