package exposed.examples.ktor.architecture.config

import kotlinx.serialization.json.Json

internal val ApplicationJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
