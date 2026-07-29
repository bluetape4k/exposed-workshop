plugins {
    alias(libs.plugins.exposed)
    kotlin("plugin.spring")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.graalvm.native)
}

exposed {
    migrations {
        tablesPackage = "exposed.examples.spring.transaction"
        databaseUrl = "jdbc:h2:mem:09-spring-03-spring-transaction-migrations;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
        databaseUser = "sa"
        databasePassword = ""
    }
}

@Suppress("UnstableApiUsage")
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {

    testImplementation(project(":exposed-shared-tests"))

    // Exposed ORM 및 DSL 의존성
    implementation(libs.jetbrains.exposed.core)
    implementation(libs.jetbrains.exposed.dao)
    implementation(libs.jetbrains.exposed.jdbc)
    implementation(libs.jetbrains.exposed.spring.boot.starter)

    // Bluetape4k 공통 테스트/유틸리티 의존성
    implementation(libs.exposed.core)
    implementation(libs.bluetape4k.io)
    implementation(libs.bluetape4k.jdbc)
    implementation(libs.bluetape4k.spring.boot.core)

    // Spring Boot 통합 및 테스트 의존성
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }

    runtimeOnly(libs.h2.v2)
    runtimeOnly(libs.hikaricp)

    implementation(libs.datafaker)

    // 코루틴 기반 트랜잭션 및 비동기 흐름 의존성
    implementation(libs.bluetape4k.coroutines)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.debug)
    testImplementation(libs.kotlinx.coroutines.test)
}
