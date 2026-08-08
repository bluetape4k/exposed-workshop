package exposed.shared.tests

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.serialization.json.Json

/** 모든 Ktor 예제가 동일한 unknown-field tolerant JSON client를 사용하도록 합니다. */
fun ApplicationTestBuilder.createJsonClient(): HttpClient =
    createClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                },
            )
        }
    }
