plugins {
    alias(libs.plugins.exposed)
    alias(libs.plugins.kotlin.serialization)
}

exposed {
    migrations {
        tablesPackage = "exposed.examples.ktor.cache.coroutines.persistence"
        databaseUrl = "jdbc:h2:mem:11-high-performance-06-cache-strategies-coroutines-ktor-migrations;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
        databaseUser = "sa"
        databasePassword = ""
    }
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.jetbrains.exposed.core)
    implementation(libs.jetbrains.exposed.jdbc)
    implementation(libs.exposed.cache)
    implementation(libs.exposed.jdbc.caffeine)
    implementation(libs.exposed.ktor)
    implementation(libs.hikaricp)
    implementation(libs.kotlinx.coroutines.core)

    runtimeOnly(libs.h2.v2)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.bluetape4k.junit5)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(project(":exposed-shared-tests"))
}
