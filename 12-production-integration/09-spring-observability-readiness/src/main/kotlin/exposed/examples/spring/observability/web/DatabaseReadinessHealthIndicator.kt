package exposed.examples.spring.observability.web

import exposed.examples.spring.observability.repository.DiagnosticsRepository
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Component

@Component("databaseReadiness")
internal class DatabaseReadinessHealthIndicator(
    private val state: DiagnosticsState,
    private val repository: DiagnosticsRepository,
) : HealthIndicator {

    override fun health(): Health =
        when {
            !state.isDatabaseAvailable() ->
                Health.down()
                    .withDetail("database", "degraded by example state")
                    .build()

            else -> databaseHealth()
        }

    private fun databaseHealth(): Health =
        try {
            if (repository.ping()) {
                Health.up()
                    .withDetail("database", "reachable")
                    .build()
            } else {
                Health.down()
                    .withDetail("database", "unreachable")
                    .build()
            }
        } catch (e: Exception) {
            Health.down(e)
                .withDetail("database", "unreachable")
                .build()
        }
}
