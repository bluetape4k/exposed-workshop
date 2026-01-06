configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    testImplementation(platform(Libs.exposed_bom))
    testImplementation(platform(Libs.bluetape4k_bom))

    testImplementation(project(":exposed-shared-tests"))

    testImplementation(Libs.exposed_core)
    testImplementation(Libs.exposed_dao)
    testImplementation(Libs.exposed_jdbc)

    testImplementation(Libs.bluetape4k_exposed)
    testImplementation(Libs.bluetape4k_exposed_jackson)

    testImplementation(Libs.bluetape4k_junit5)

    testRuntimeOnly(Libs.h2_v2)
    testRuntimeOnly(Libs.mariadb_java_client)
    testRuntimeOnly(Libs.mysql_connector_j)
    testRuntimeOnly(Libs.postgresql_driver)
    testRuntimeOnly(Libs.pgjdbc_ng)

    testImplementation(Libs.bluetape4k_testcontainers)
    testImplementation(Libs.testcontainers)
    testImplementation(Libs.testcontainers_mariadb)
    testImplementation(Libs.testcontainers_mysql)
    testImplementation(Libs.testcontainers_postgresql)

    // Coroutines
    testImplementation(Libs.bluetape4k_coroutines)
    testImplementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
