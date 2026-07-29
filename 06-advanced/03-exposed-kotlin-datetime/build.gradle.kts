plugins {
    kotlin("plugin.serialization")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {

    testImplementation(project(":exposed-shared-tests"))

    testImplementation(libs.jetbrains.exposed.core)
    testImplementation(libs.jetbrains.exposed.dao)
    testImplementation(libs.jetbrains.exposed.jdbc)
    testImplementation(libs.jetbrains.exposed.json)
    testImplementation(libs.jetbrains.exposed.migration.jdbc)

    // java time 지원 라이브러리
    testImplementation(libs.jetbrains.exposed.kotlin.datetime)

    // Kotlin Serialization JSON 컬럼 직렬화 의존성
    testImplementation(platform(libs.kotlinx.serialization.bom))
    testImplementation(libs.kotlinx.serialization.json)

    testImplementation(libs.exposed.core)
    testImplementation(libs.bluetape4k.junit5)

    testImplementation(libs.bluetape4k.testcontainers)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.mariadb)
    testImplementation(libs.testcontainers.mysql)
    testImplementation(libs.testcontainers.postgresql)

    // 테스트 데이터베이스별 JDBC 드라이버 의존성
    testRuntimeOnly(libs.h2.v2)
    testRuntimeOnly(libs.mariadb.java.client)
    testRuntimeOnly(libs.mysql.connector.j)
    testRuntimeOnly(libs.postgresql.driver)
    testRuntimeOnly(libs.pgjdbc.ng)

    // 코루틴 기반 트랜잭션 예제 의존성
    testImplementation(libs.bluetape4k.coroutines)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
