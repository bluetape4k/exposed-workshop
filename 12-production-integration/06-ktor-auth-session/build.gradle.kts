plugins {
    application
    alias(libs.plugins.kotlin.serialization)
}

application {
    mainClass.set("exposed.examples.ktor.auth.KtorAuthSessionApplicationKt")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.call.id)
    implementation("io.ktor:ktor-server-auth:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-sessions:${libs.versions.ktor.get()}")
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation("org.springframework.security:spring-security-crypto")

    implementation(libs.jetbrains.exposed.core)
    implementation(libs.jetbrains.exposed.jdbc)
    implementation(libs.hikaricp)

    runtimeOnly(libs.h2.v2)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.bluetape4k.junit5)
    testImplementation(libs.kotlinx.coroutines.test)
}
