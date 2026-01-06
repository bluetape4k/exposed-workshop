plugins {
    kotlin("plugin.serialization")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.exposed_bom))

    testImplementation(project(":exposed-shared-tests"))

    testImplementation(Libs.exposed_core)
    testImplementation(Libs.exposed_dao)
    testImplementation(Libs.exposed_jdbc)

    // Exposed Json 지원 라이브러리 (kotlinx.serialization 을 사용합니다)
    testImplementation(Libs.exposed_json)

    // Kotlin Serialization Json
    testImplementation(platform(Libs.kotlinx_serialization_bom))
    testImplementation(Libs.kotlinx_serialization_json)

    testImplementation(Libs.bluetape4k_exposed)
    testImplementation(Libs.bluetape4k_junit5)

    testImplementation(Libs.bluetape4k_testcontainers)
    testImplementation(Libs.testcontainers)
    testImplementation(Libs.testcontainers_mariadb)
    testImplementation(Libs.testcontainers_mysql)
    testImplementation(Libs.testcontainers_postgresql)

    // Jdbc Drivers
    testRuntimeOnly(Libs.h2_v2)
    testRuntimeOnly(Libs.mariadb_java_client)
    testRuntimeOnly(Libs.mysql_connector_j)
    testRuntimeOnly(Libs.postgresql_driver)
    testRuntimeOnly(Libs.pgjdbc_ng)

    // Coroutines
    testImplementation(Libs.bluetape4k_coroutines)
    testImplementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
