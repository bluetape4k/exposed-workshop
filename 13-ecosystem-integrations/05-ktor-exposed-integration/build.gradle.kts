plugins {
    alias(libs.plugins.exposed)
    application
    alias(libs.plugins.kotlin.serialization)
}

exposed {
    migrations {
        tablesPackage = "exposed.examples.ktor.exposedintegration"
        databaseUrl = "jdbc:h2:mem:13-ecosystem-integrations-05-ktor-exposed-integration-migrations;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
        databaseUser = "sa"
        databasePassword = ""
    }
}

application {
    mainClass.set("exposed.examples.ktor.exposedintegration.KtorExposedIntegrationApplicationKt")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(libs.bluetape4k.ktor.core)
    implementation(libs.exposed.ktor)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.jetbrains.exposed.core)
    implementation(libs.jetbrains.exposed.jdbc)
    implementation(libs.jetbrains.exposed.r2dbc)
    implementation(libs.hikaricp)
    implementation(libs.r2dbc.pool)

    runtimeOnly(libs.h2.v2)
    runtimeOnly(libs.r2dbc.h2)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.bluetape4k.junit5)
    testImplementation(libs.bluetape4k.ktor.testing)
    testImplementation(libs.kotlinx.coroutines.test)
}
