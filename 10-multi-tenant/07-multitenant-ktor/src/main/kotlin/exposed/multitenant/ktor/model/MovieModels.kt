package exposed.multitenant.ktor.model

import kotlinx.serialization.Serializable
import java.io.Serializable as JavaSerializable

@Serializable
data class IndexResponse(
    val name: String = "multitenant-ktor",
    val tenantHeader: String = "X-Tenant-ID",
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
data class MovieResponse(
    val id: Long,
    val title: String,
    val tenant: String,
) : JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
data class CreateMovieRequest(
    val title: String,
) : JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
data class TenantResponse(
    val tenant: String,
    val schema: String,
) : JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
