plugins {
    kotlin("plugin.serialization")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.exposed_bom))

    implementation(project(":exposed-shared-tests"))

    implementation(Libs.bluetape4k_exposed)
    implementation(Libs.exposed_core)
    testImplementation(Libs.exposed_jdbc)
    testImplementation(Libs.exposed_dao)
    testImplementation(Libs.exposed_json)
    testImplementation(Libs.exposed_migration_jdbc)

    // java time 지원 라이브러리
    testImplementation(Libs.exposed_kotlin_datetime)

    // Kotlin Serialization Json
    testImplementation(platform(Libs.kotlinx_serialization_bom))
    testImplementation(Libs.kotlinx_serialization_json)

    testImplementation(Libs.h2_v2)
    testImplementation(Libs.mysql_connector_j)
    testImplementation(Libs.postgresql_driver)
    testImplementation(Libs.pgjdbc_ng)

    testImplementation(Libs.bluetape4k_junit5)
    testImplementation(Libs.bluetape4k_testcontainers)
    testImplementation(Libs.testcontainers)
    testImplementation(Libs.testcontainers_mysql)
    testImplementation(Libs.testcontainers_postgresql)

    // Coroutines
    testImplementation(Libs.bluetape4k_coroutines)
    testImplementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
