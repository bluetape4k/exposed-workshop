plugins {
    kotlin("plugin.serialization")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(libs.jetbrains.exposed.bom))

    testImplementation(project(":exposed-shared-tests"))

    testImplementation(libs.jetbrains.exposed.core)
    testImplementation(libs.jetbrains.exposed.dao)
    testImplementation(libs.jetbrains.exposed.jdbc)
    testImplementation(libs.jetbrains.exposed.json)
    testImplementation(libs.jetbrains.exposed.migration.jdbc)

    // java time 지원 라이브러리
    testImplementation(libs.jetbrains.exposed.kotlin.datetime)

    // Kotlin Serialization Json
    testImplementation(platform(libs.kotlinx.serialization.bom))
    testImplementation(libs.kotlinx.serialization.json)

    testImplementation(libs.exposed.core)
    testImplementation(libs.bluetape4k.junit5)

    testImplementation(libs.bluetape4k.testcontainers)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.mariadb)
    testImplementation(libs.testcontainers.mysql)
    testImplementation(libs.testcontainers.postgresql)

    // Jdbc Drivers
    testRuntimeOnly(libs.h2.v2)
    testRuntimeOnly(libs.mariadb.java.client)
    testRuntimeOnly(libs.mysql.connector.j)
    testRuntimeOnly(libs.postgresql.driver)
    testRuntimeOnly(libs.pgjdbc.ng)

    // Coroutines
    testImplementation(libs.bluetape4k.coroutines)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
