package exposed.examples.ktor.observability.config

import kotlinx.serialization.json.Json

internal val ApplicationJson = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}
