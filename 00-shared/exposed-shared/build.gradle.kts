plugins {
    kotlin("plugin.serialization")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.exposed_bom))

    testImplementation(project(":exposed-shared-tests"))

    api(Libs.bluetape4k_exposed)
    api(Libs.exposed_core)
    api(Libs.exposed_crypt)
    api(Libs.exposed_dao)
    api(Libs.exposed_java_time)
    api(Libs.exposed_jdbc)
    api(Libs.exposed_json)
    api(Libs.exposed_kotlin_datetime)
    api(Libs.exposed_migration)
    api(Libs.exposed_money)
    api(Libs.exposed_spring_boot_starter)

    implementation(Libs.bluetape4k_jdbc)
    testImplementation(Libs.bluetape4k_junit5)

    implementation(Libs.h2_v2)
    implementation(Libs.mysql_connector_j)
    implementation(Libs.postgresql_driver)

    testImplementation(Libs.bluetape4k_testcontainers)
    testImplementation(Libs.testcontainers)
    testImplementation(Libs.testcontainers_junit_jupiter)
    testImplementation(Libs.testcontainers_mysql)
    testImplementation(Libs.testcontainers_postgresql)
    testImplementation(Libs.testcontainers_cockroachdb)

    // Identifier 자동 생성
    implementation(Libs.bluetape4k_idgenerators)
    implementation(Libs.java_uuid_generator)

    // Coroutines
    implementation(Libs.bluetape4k_coroutines)
    implementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)

    testImplementation("io.github.hakky54:logcaptor:2.10.0")

    // Kotlin Serialization Json
    implementation(platform(Libs.kotlinx_serialization_bom))
    implementation(Libs.kotlinx_serialization_json)

    // Java Money
    implementation(Libs.bluetape4k_money)
    implementation(Libs.javax_money_api)
    implementation(Libs.javamoney_moneta)

}
