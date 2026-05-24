package exposed.examples.ktor.routingdatasource.routing

import io.ktor.http.HttpMethod
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.util.AttributeKey
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

enum class DataSourceRole {
    READ,
    WRITE,
    ;

    companion object {
        const val HEADER_NAME: String = "X-Data-Source"

        fun fromHeader(value: String?): DataSourceRole? =
            value
                ?.trim()
                ?.uppercase()
                ?.let { role -> entries.firstOrNull { it.name == role } }
    }
}

private val DataSourceRoleAttributeKey = AttributeKey<DataSourceRole>("DataSourceRole")

val RoutingPlugin = createApplicationPlugin(name = "RoutingDataSourcePlugin") {
    onCall { call ->
        if (call.request.path() in publicPaths) {
            return@onCall
        }

        val header = call.request.header(DataSourceRole.HEADER_NAME)
        val role = if (header == null) {
            defaultRole(call.request.httpMethod)
        } else {
            DataSourceRole.fromHeader(header)
                ?: throw IllegalArgumentException("Unknown datasource role: $header")
        }
        call.attributes.put(DataSourceRoleAttributeKey, role)
    }
}

object RoutingContext {
    private val current = ThreadLocal<DataSourceRole?>()

    fun currentRole(): DataSourceRole =
        current.get() ?: throw IllegalStateException("Data source role is not bound")

    fun asContextElement(role: DataSourceRole): RoutingContextElement =
        RoutingContextElement(role)

    internal fun bind(role: DataSourceRole?): DataSourceRole? {
        val previous = current.get()
        if (role == null) {
            current.remove()
        } else {
            current.set(role)
        }
        return previous
    }
}

class RoutingContextElement(
    private val role: DataSourceRole,
) : ThreadContextElement<DataSourceRole?> {
    companion object Key: CoroutineContext.Key<RoutingContextElement>

    override val key: CoroutineContext.Key<RoutingContextElement> = Key

    override fun updateThreadContext(context: CoroutineContext): DataSourceRole? =
        RoutingContext.bind(role)

    override fun restoreThreadContext(context: CoroutineContext, oldState: DataSourceRole?) {
        RoutingContext.bind(oldState)
    }
}

suspend fun ApplicationCall.withRoutingContext(block: suspend () -> Unit) {
    val role = attributes[DataSourceRoleAttributeKey]
    withContext(RoutingContext.asContextElement(role)) {
        block()
    }
}

private fun defaultRole(method: HttpMethod): DataSourceRole =
    if (method == HttpMethod.Get) DataSourceRole.READ else DataSourceRole.WRITE

private val publicPaths = setOf("/", "/health", "/routing/stats")
