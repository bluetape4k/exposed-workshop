plugins {
    kotlin("plugin.serialization")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.exposed_bom))

    implementation(project(":exposed-shared"))
    testImplementation(project(":exposed-shared-tests"))

    implementation(Libs.bluetape4k_exposed)
    implementation(Libs.exposed_core)
    implementation(Libs.exposed_crypt)
    implementation(Libs.exposed_dao)
    implementation(Libs.exposed_java_time)
    implementation(Libs.exposed_jdbc)
    implementation(Libs.exposed_json)
    implementation(Libs.exposed_kotlin_datetime)
    implementation(Libs.exposed_migration)
    implementation(Libs.exposed_money)
    implementation(Libs.exposed_spring_boot_starter)

    implementation(Libs.bluetape4k_jdbc)
    testImplementation(Libs.bluetape4k_junit5)

    compileOnly(Libs.h2_v2)
    compileOnly(Libs.mysql_connector_j)
    compileOnly(Libs.postgresql_driver)
    compileOnly(Libs.pgjdbc_ng)

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

    // Kotlin Serialization Json
    implementation(platform(Libs.kotlinx_serialization_bom))
    implementation(Libs.kotlinx_serialization_json)

    // Java Money
    implementation(Libs.bluetape4k_money)
    implementation(Libs.javax_money_api)
    implementation(Libs.javamoney_moneta)

}
