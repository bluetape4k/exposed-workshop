configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {

    // Bluetape4k Exposed 의존성
    api(libs.exposed.core)
    api(libs.exposed.dao)
    api(libs.exposed.jdbc)
    implementation(libs.exposed.jackson2)
    implementation(libs.exposed.fastjson2)

    // JetBrains Exposed 의존성
    api(libs.jetbrains.exposed.core)
    api(libs.jetbrains.exposed.dao)
    api(libs.jetbrains.exposed.jdbc)
    implementation(libs.jetbrains.exposed.java.time)
    implementation(libs.jetbrains.exposed.crypt)
    implementation(libs.jetbrains.exposed.json)
    implementation(libs.jetbrains.exposed.migration.jdbc)
    implementation(libs.jetbrains.exposed.money)
    implementation(libs.jetbrains.exposed.spring.boot.starter)

    implementation(libs.bluetape4k.jdbc)
    implementation(libs.bluetape4k.junit5)

    runtimeOnly(libs.hikaricp)

    compileOnly(libs.h2.v2)
    compileOnly(libs.mariadb.java.client)
    compileOnly(libs.mysql.connector.j)
    compileOnly(libs.postgresql.driver)
    compileOnly(libs.pgjdbc.ng)

    implementation(libs.bluetape4k.testcontainers)
    implementation(libs.testcontainers)
    implementation(libs.testcontainers.mariadb)
    implementation(libs.testcontainers.mysql)
    implementation(libs.testcontainers.postgresql)
    implementation(libs.testcontainers.cockroachdb)

    // Identifier 자동 생성
    implementation(libs.bluetape4k.idgenerators)
    implementation(libs.java.uuid.generator)

    // Ktor 테스트 애플리케이션 공통 JSON client
    api(libs.ktor.server.test.host)
    api(libs.ktor.client.content.negotiation)
    api(libs.ktor.serialization.kotlinx.json)
    api(libs.kotlinx.serialization.json)

    // 코루틴 테스트 의존성
    testImplementation(libs.bluetape4k.coroutines)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)

    // Java Money 테스트 의존성
    testImplementation(libs.bluetape4k.money)
    testImplementation(libs.javax.money.api)
    testImplementation(libs.javamoney.moneta)

    // Logcaptor 테스트 의존성
    testImplementation(libs.logcaptor)

}
