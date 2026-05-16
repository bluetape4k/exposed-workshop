plugins {
    kotlin("plugin.spring")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.graalvm.native)
}

@Suppress("UnstableApiUsage")
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(libs.jetbrains.exposed.bom))

    testImplementation(project(":exposed-shared-tests"))

    // Exposed
    implementation(libs.jetbrains.exposed.core)
    implementation(libs.jetbrains.exposed.dao)
    implementation(libs.jetbrains.exposed.jdbc)
    implementation(libs.jetbrains.exposed.spring.boot.starter)

    // Bluetape4k
    implementation(libs.exposed.core)
    implementation(libs.bluetape4k.io)
    implementation(libs.bluetape4k.jdbc)
    implementation(libs.bluetape4k.spring.boot4.core)

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }

    runtimeOnly(libs.h2.v2)
    runtimeOnly(libs.hikaricp)

    implementation(libs.datafaker)

    // Coroutines
    implementation(libs.bluetape4k.coroutines)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.debug)
    testImplementation(libs.kotlinx.coroutines.test)
}
