plugins {
    kotlin("plugin.serialization")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {

    testImplementation(project(":exposed-shared-tests"))

    implementation(libs.jetbrains.exposed.core)
    implementation(libs.jetbrains.exposed.dao)
    implementation(libs.jetbrains.exposed.jdbc)
    implementation(libs.jetbrains.exposed.java.time)

    // Validator
    implementation(libs.jakarta.validation.api)
    implementation(libs.hibernate.validator)

    testImplementation(libs.exposed.core)

    testImplementation(libs.bluetape4k.junit5)

    testRuntimeOnly(libs.h2.v2)
    testRuntimeOnly(libs.mariadb.java.client)
    testRuntimeOnly(libs.mysql.connector.j)
    testRuntimeOnly(libs.postgresql.driver)
    testRuntimeOnly(libs.pgjdbc.ng)

    testImplementation(libs.bluetape4k.testcontainers)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.mariadb)
    testImplementation(libs.testcontainers.mysql)
    testImplementation(libs.testcontainers.postgresql)

    // Coroutines
    testImplementation(libs.bluetape4k.coroutines)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
