
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    testImplementation(libs.bluetape4k.io)
    testImplementation(libs.bluetape4k.jdbc)
    testImplementation(libs.bluetape4k.junit5)

    // Vertx
    testImplementation(libs.bluetape4k.vertx)
    testImplementation(libs.vertx.junit5)

    // Vertx Kotlin
    testImplementation(libs.vertx.core)
    testImplementation(libs.vertx.lang.kotlin)
    testImplementation(libs.vertx.lang.kotlin.coroutines)

    // Vertx SqlClient
    testImplementation(libs.vertx.sql.client)
    testImplementation(libs.vertx.sql.client.templates)
    testImplementation(libs.vertx.mysql.client)
    testImplementation(libs.vertx.pg.client)

    testImplementation("com.ongres.scram:scram-client:3.2") // vert.x sql client 에서 사용하는데 제외되었다.

    // Vertx Jdbc (MySQL, Postgres 를 제외한 H2 같은 것은 기존 JDBC 를 Wrapping한 것을 사용합니다)
    testImplementation(libs.vertx.jdbc.client)
    testImplementation(libs.agroal.pool)

    // MyBatis
    testImplementation(libs.mybatis.dynamic.sql)

    // Vetx SqlClient Templates 에서 Jackson Databind 를 이용한 매핑을 사용한다
    testImplementation(libs.bluetape4k.jackson2)
    testImplementation(libs.jackson.module.kotlin)
    testImplementation(libs.jackson.module.blackbird)

    testRuntimeOnly(libs.h2)
    testRuntimeOnly(libs.mysql.connector.j)

    // Testcontainers
    testImplementation(libs.bluetape4k.testcontainers)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.mysql)
    testImplementation(libs.testcontainers.postgresql)

    // Coroutines
    testImplementation(libs.bluetape4k.coroutines)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
