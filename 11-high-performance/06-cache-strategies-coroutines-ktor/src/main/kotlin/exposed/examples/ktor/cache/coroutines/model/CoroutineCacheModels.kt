package exposed.examples.ktor.cache.coroutines.model

import kotlinx.serialization.Serializable
import java.io.Serializable as JavaSerializable

@Serializable
data class IndexResponse(
    val name: String = "cache-strategies-coroutines-ktor",
) : JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
data class HealthResponse(
    val status: String,
) : JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
data class ProductResponse(
    val sku: String,
    val name: String,
    val version: Int,
    val source: String,
) : JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
data class UpdateProductRequest(
    val name: String,
) : JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
data class CoroutineCacheStatsResponse(
    val databaseReads: Int,
    val cacheHits: Int,
    val cacheMisses: Int,
    val inFlightLoads: Int,
    val cacheSize: Int,
) : JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
