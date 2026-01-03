plugins {
    kotlin("plugin.serialization")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.exposed_bom))

    testImplementation(project(":exposed-shared-tests"))

    implementation(Libs.bluetape4k_exposed)
    implementation(Libs.exposed_core)
    implementation(Libs.exposed_jdbc)
    implementation(Libs.exposed_dao)
    implementation(Libs.exposed_json)
    implementation(Libs.exposed_migration_jdbc)

    // java time 지원 라이브러리
    implementation(Libs.exposed_java_time)

    // Kotlin Serialization Json
    implementation(platform(Libs.kotlinx_serialization_bom))
    implementation(Libs.kotlinx_serialization_json)

    implementation(Libs.h2_v2)
    implementation(Libs.mysql_connector_j)
    implementation(Libs.postgresql_driver)
    implementation(Libs.pgjdbc_ng)

    testImplementation(Libs.bluetape4k_junit5)
    testImplementation(Libs.bluetape4k_testcontainers)
    testImplementation(Libs.testcontainers)
    testImplementation(Libs.testcontainers_mysql)
    testImplementation(Libs.testcontainers_postgresql)

    // Coroutines
    implementation(Libs.bluetape4k_coroutines)
    implementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
