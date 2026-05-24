configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {

    testImplementation(project(":exposed-shared-tests"))

    testImplementation(libs.jetbrains.exposed.core)
    testImplementation(libs.jetbrains.exposed.dao)
    testImplementation(libs.jetbrains.exposed.jdbc)

    // Java 21 에서 Virtual Thread 를 사용할 때 (Java 25 에서는 jdk25 를 사용하세요)
    testRuntimeOnly(libs.bluetape4k.virtualthread.jdk21)

    testImplementation(libs.exposed.core)
    testImplementation(libs.bluetape4k.junit5)

    testRuntimeOnly(libs.h2.v2)
    testRuntimeOnly(libs.mariadb.java.client)
    testRuntimeOnly(libs.mysql.connector.j)
    testRuntimeOnly(libs.postgresql.driver)
    testRuntimeOnly(libs.pgjdbc.ng)

    testImplementation(libs.bluetape4k.testcontainers)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.mariadb)
    testImplementation(libs.testcontainers.mysql)
    testImplementation(libs.testcontainers.postgresql)

    // TODO: Virtual Threads 모듈은 코루틴을 사용하지 않습니다.
    // "코루틴 없이 Virtual Threads만으로 비동기 처리"를 명확히 하려면 아래 의존성을 제거할 수 있습니다.
    // Coroutines
    testImplementation(libs.bluetape4k.coroutines)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
