configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {

    testImplementation(project(":exposed-shared-tests"))

    testImplementation(libs.jetbrains.exposed.core)
    testImplementation(libs.jetbrains.exposed.dao)
    testImplementation(libs.jetbrains.exposed.jdbc)
    testImplementation(libs.jetbrains.exposed.migration.jdbc)

    testImplementation(libs.exposed.core)
    testImplementation(libs.exposed.jackson3)

    // Jackson 3 JSON 컬럼 직렬화 의존성
    testImplementation(libs.bluetape4k.jackson3)
    testImplementation(libs.jackson3.module.kotlin)
    testImplementation(libs.jackson3.module.blackbird)

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

    // 코루틴 기반 트랜잭션 예제 의존성
    testImplementation(libs.bluetape4k.coroutines)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
