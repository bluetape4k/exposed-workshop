package exposed.examples.cache.lettuce

import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig

/** 상품 캐시의 namespace와 provider 기본 설정을 한 곳에서 고정합니다. */
object ProductLettuceCacheConfig {
    const val KEY_PREFIX = "workshop:products"
    const val PRODUCT_PATTERN = "product-*"

    /** near-cache 없이 Redis remote cache만 사용하는 기본 설정입니다. */
    fun default(): LettuceCacheConfig =
        LettuceCacheConfig.READ_WRITE_THROUGH.copy(
            nearCacheEnabled = false,
            keyPrefix = KEY_PREFIX
        )

    internal fun requireAllowed(config: LettuceCacheConfig) {
        require(config.keyPrefix == KEY_PREFIX || config.keyPrefix.startsWith("$KEY_PREFIX:")) {
            "상품 캐시 keyPrefix는 $KEY_PREFIX namespace 또는 테스트 suffix여야 합니다."
        }
        require(!config.nearCacheEnabled) {
            "상품 예제는 remote-only Lettuce cache만 사용합니다."
        }
    }
}
