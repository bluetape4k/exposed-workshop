plugins {
    kotlin("plugin.serialization")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.exposed_bom))

    // Bluetape4k Exposed
    api(Libs.bluetape4k_exposed)
    implementation(Libs.bluetape4k_exposed_jackson)
    implementation(Libs.bluetape4k_exposed_fastjson2)
    implementation(Libs.bluetape4k_exposed_jasypt)

    // Exposed
    api(Libs.exposed_core)
    api(Libs.exposed_dao)
    api(Libs.exposed_jdbc)
    implementation(Libs.exposed_java_time)
    implementation(Libs.exposed_crypt)
    implementation(Libs.exposed_json)
    implementation(Libs.exposed_migration_jdbc)
    implementation(Libs.exposed_money)
    implementation(Libs.exposed_spring_boot_starter)

    implementation(Libs.bluetape4k_jdbc)
    implementation(Libs.bluetape4k_junit5)
    implementation(Libs.kluent)

    runtimeOnly(Libs.hikaricp)

    compileOnly(Libs.h2_v2)
    compileOnly(Libs.mariadb_java_client)
    compileOnly(Libs.mysql_connector_j)
    compileOnly(Libs.postgresql_driver)
    compileOnly(Libs.pgjdbc_ng)

    implementation(Libs.bluetape4k_testcontainers)
    implementation(Libs.testcontainers)
    implementation(Libs.testcontainers_mariadb)
    implementation(Libs.testcontainers_mysql)
    implementation(Libs.testcontainers_postgresql)
    implementation(Libs.testcontainers_cockroachdb)

    // Identifier 자동 생성
    implementation(Libs.bluetape4k_idgenerators)
    implementation(Libs.java_uuid_generator)

    // Coroutines
    testImplementation(Libs.bluetape4k_coroutines)
    testImplementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Kotlin Serialization Json
    testImplementation(platform(Libs.kotlinx_serialization_bom))
    testImplementation(Libs.kotlinx_serialization_json)

    // Java Money
    testImplementation(Libs.bluetape4k_money)
    testImplementation(Libs.javax_money_api)
    testImplementation(Libs.javamoney_moneta)

    // Logcaptor
    testImplementation("io.github.hakky54:logcaptor:2.10.0")

}
