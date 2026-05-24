configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {

    // Bluetape4k Exposed
    api(libs.exposed.core)
    api(libs.exposed.dao)
    api(libs.exposed.jdbc)
    implementation(libs.exposed.jackson2)
    implementation(libs.exposed.fastjson2)

    // Exposed
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

    // Coroutines
    testImplementation(libs.bluetape4k.coroutines)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)

    // Java Money
    testImplementation(libs.bluetape4k.money)
    testImplementation(libs.javax.money.api)
    testImplementation(libs.javamoney.moneta)

    // Logcaptor
    testImplementation(libs.logcaptor)

}
