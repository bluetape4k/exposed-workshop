package exposed.examples.ktor.cache.model

import kotlinx.serialization.Serializable
import java.io.Serializable as JavaSerializable

@Serializable
data class IndexResponse(
    val name: String = "cache-strategies-ktor",
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
data class UserResponse(
    val id: String,
    val displayName: String,
    val version: Int,
    val source: String,
) : JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
data class UpsertUserRequest(
    val displayName: String,
) : JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
data class CacheStatsResponse(
    val databaseReads: Int,
    val cacheHits: Int,
    val cacheMisses: Int,
    val cacheSize: Int,
) : JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
