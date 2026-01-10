plugins {
    kotlin("kapt")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(Libs.bluetape4k_io)
    implementation(Libs.bluetape4k_jdbc)
    testImplementation(Libs.bluetape4k_junit5)

    // Vertx
    implementation(Libs.bluetape4k_vertx_core)
    implementation(Libs.bluetape4k_vertx_sqlclient)
    testImplementation(Libs.vertx_junit5)

    // Vertx Kotlin
    implementation(Libs.vertx_core)
    implementation(Libs.vertx_lang_kotlin)
    implementation(Libs.vertx_lang_kotlin_coroutines)

    // Vertx SqlClient
    implementation(Libs.vertx_sql_client)
    implementation(Libs.vertx_sql_client_templates)
    runtimeOnly(Libs.vertx_mysql_client)
    runtimeOnly(Libs.vertx_pg_client)

    implementation("com.ongres.scram:scram-client:3.2") // vert.x sql client 에서 사용하는데 제외되었다.

    // Vertx Jdbc (MySQL, Postgres 를 제외한 H2 같은 것은 기존 JDBC 를 Wrapping한 것을 사용합니다)
    runtimeOnly(Libs.vertx_jdbc_client)
    runtimeOnly(Libs.agroal_pool)

    // vertx-sql-cleint-templates 에서 @DataObject, @RowMapped 를 위해 사용
    compileOnly(Libs.vertx_codegen)
    kapt(Libs.vertx_codegen)
    kaptTest(Libs.vertx_codegen)

    // MyBatis
    implementation(Libs.mybatis_dynamic_sql)

    // Vetx SqlClient Templates 에서 Jackson Databind 를 이용한 매핑을 사용한다
    implementation(Libs.bluetape4k_jackson)
    implementation(Libs.jackson_module_kotlin)
    implementation(Libs.jackson_datatype_jdk8)
    implementation(Libs.jackson_datatype_jsr310)

    runtimeOnly(Libs.h2)
    runtimeOnly(Libs.mysql_connector_j)

    // Testcontainers
    testImplementation(Libs.bluetape4k_testcontainers)
    testImplementation(Libs.testcontainers)
    testImplementation(Libs.testcontainers_mysql)
    testImplementation(Libs.testcontainers_postgresql)

    // Coroutines
    implementation(Libs.bluetape4k_coroutines)
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)
}
