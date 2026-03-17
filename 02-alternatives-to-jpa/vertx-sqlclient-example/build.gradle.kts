
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    testImplementation(Libs.bluetape4k_io)
    testImplementation(Libs.bluetape4k_jdbc)
    testImplementation(Libs.bluetape4k_junit5)

    // Vertx
    testImplementation(Libs.bluetape4k_vertx)
    testImplementation(Libs.vertx_junit5)

    // Vertx Kotlin
    testImplementation(Libs.vertx_core)
    testImplementation(Libs.vertx_lang_kotlin)
    testImplementation(Libs.vertx_lang_kotlin_coroutines)

    // Vertx SqlClient
    testImplementation(Libs.vertx_sql_client)
    testImplementation(Libs.vertx_sql_client_templates)
    testImplementation(Libs.vertx_mysql_client)
    testImplementation(Libs.vertx_pg_client)

    testImplementation("com.ongres.scram:scram-client:3.2") // vert.x sql client 에서 사용하는데 제외되었다.

    // Vertx Jdbc (MySQL, Postgres 를 제외한 H2 같은 것은 기존 JDBC 를 Wrapping한 것을 사용합니다)
    testImplementation(Libs.vertx_jdbc_client)
    testImplementation(Libs.agroal_pool)

    // MyBatis
    testImplementation(Libs.mybatis_dynamic_sql)

    // Vetx SqlClient Templates 에서 Jackson Databind 를 이용한 매핑을 사용한다
    testImplementation(Libs.bluetape4k_jackson2)
    testImplementation(Libs.jackson_module_kotlin)
    testImplementation(Libs.jackson_module_blackbird)

    testRuntimeOnly(Libs.h2)
    testRuntimeOnly(Libs.mysql_connector_j)

    // Testcontainers
    testImplementation(Libs.bluetape4k_testcontainers)
    testImplementation(Libs.testcontainers)
    testImplementation(Libs.testcontainers_mysql)
    testImplementation(Libs.testcontainers_postgresql)

    // Coroutines
    testImplementation(Libs.bluetape4k_coroutines)
    testImplementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
