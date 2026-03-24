configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.exposed_bom))

    testImplementation(project(":exposed-shared-tests"))

    testImplementation(Libs.exposed_core)
    testImplementation(Libs.exposed_dao)
    testImplementation(Libs.exposed_jdbc)

    // Java 21 에서 Virtual Thread 를 사용할 때 (Java 25 에서는 jdk25 를 사용하세요)
    testRuntimeOnly(Libs.bluetape4k_virtualthread_jdk21)

    testImplementation(Libs.bluetape4k_exposed)
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

    // TODO: Virtual Threads 모듈은 코루틴을 사용하지 않습니다.
    // "코루틴 없이 Virtual Threads만으로 비동기 처리"를 명확히 하려면 아래 의존성을 제거할 수 있습니다.
    // Coroutines
    testImplementation(Libs.bluetape4k_coroutines)
    testImplementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
