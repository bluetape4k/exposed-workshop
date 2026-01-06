configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.exposed_bom))

    testImplementation(project(":exposed-shared-tests"))

    testImplementation(Libs.exposed_core)
    testImplementation(Libs.exposed_jdbc)
    testImplementation(Libs.exposed_dao)
    testImplementation(Libs.exposed_java_time)
    testImplementation(Libs.bluetape4k_exposed)

    testImplementation(Libs.bluetape4k_idgenerators)
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

    // Logcaptor
    testImplementation("io.github.hakky54:logcaptor:2.10.0")
}
