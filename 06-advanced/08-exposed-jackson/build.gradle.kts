configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.exposed_bom))
    implementation(platform(Libs.bluetape4k_bom))

    implementation(Libs.exposed_core)
    implementation(Libs.exposed_jdbc)
    implementation(Libs.exposed_dao)

    implementation(Libs.bluetape4k_exposed_jackson)
    testImplementation(Libs.bluetape4k_exposed_tests)
    testImplementation(Libs.bluetape4k_junit5)

    // Coroutines
    implementation(Libs.bluetape4k_coroutines)
    implementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)

    api(Libs.hikaricp)

    api(Libs.h2_v2)
    api(Libs.mariadb_java_client)
    api(Libs.mysql_connector_j)
    api(Libs.postgresql_driver)
    api(Libs.pgjdbc_ng)

    api(Libs.bluetape4k_testcontainers)
    api(Libs.testcontainers)
    api(Libs.testcontainers_junit_jupiter)
    api(Libs.testcontainers_mariadb)
    api(Libs.testcontainers_mysql)
    api(Libs.testcontainers_postgresql)
    api(Libs.testcontainers_cockroachdb)
}
