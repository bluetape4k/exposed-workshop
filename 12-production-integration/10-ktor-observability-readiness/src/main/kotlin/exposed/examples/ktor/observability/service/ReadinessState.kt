package exposed.examples.ktor.observability.service

import java.util.concurrent.atomic.AtomicBoolean

internal class ReadinessState {
    private val databaseAvailable = AtomicBoolean(true)

    fun markDatabaseAvailable(available: Boolean) {
        databaseAvailable.set(available)
    }

    fun isDatabaseAvailable(): Boolean =
        databaseAvailable.get()
}
