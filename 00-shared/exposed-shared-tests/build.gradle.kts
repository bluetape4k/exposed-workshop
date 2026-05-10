configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(libs.exposed.bom))

    // Bluetape4k Exposed
    api(libs.bluetape4k.exposed.core)
    api(libs.bluetape4k.exposed.jdbc)
    implementation(libs.bluetape4k.exposed.jackson2)
    implementation(libs.bluetape4k.exposed.fastjson2)

    // Exposed
    api(libs.exposed.core)
    api(libs.exposed.dao)
    api(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.exposed.crypt)
    implementation(libs.exposed.json)
    implementation(libs.exposed.migration.jdbc)
    implementation(libs.exposed.money)
    implementation(libs.exposed.spring.boot.starter)

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
