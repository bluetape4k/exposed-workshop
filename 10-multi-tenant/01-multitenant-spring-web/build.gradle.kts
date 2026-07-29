plugins {
    alias(libs.plugins.exposed)
    kotlin("plugin.spring")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.graalvm.native)
}

exposed {
    migrations {
        tablesPackage = "exposed.multitenant.springweb"
        databaseUrl = "jdbc:h2:mem:10-multi-tenant-01-multitenant-spring-web-migrations;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
        databaseUser = "sa"
        databasePassword = ""
    }
}

springBoot {
    mainClass.set("exposed.multitenant.springweb.ExposedMultitenantApplicationKt")

    buildInfo {
        properties {
            additional.put("name", "Spring MVC with Exposed")
            additional.put("java.version", JavaVersion.current())
        }
    }
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {


    testImplementation(project(":exposed-shared-tests"))

    // Exposed ORM 및 DSL 의존성
    implementation(libs.exposed.core)
    implementation(libs.jetbrains.exposed.core)
    implementation(libs.jetbrains.exposed.jdbc)
    implementation(libs.jetbrains.exposed.dao)
    implementation(libs.jetbrains.exposed.java.time)
    implementation(libs.jetbrains.exposed.migration.jdbc)
    implementation(libs.jetbrains.exposed.spring.boot.starter)

    // Bluetape4k 공통 테스트/유틸리티 의존성
    implementation(libs.bluetape4k.io)
    implementation(libs.bluetape4k.jackson2)
    implementation(libs.bluetape4k.jdbc)
    testImplementation(libs.bluetape4k.junit5)
    testImplementation(libs.bluetape4k.spring.boot.core)

    // 테스트 데이터베이스별 JDBC 드라이버 의존성
    implementation(libs.hikaricp)

    // H2
    runtimeOnly(libs.h2.v2)

    // Testcontainers 기반 Docker 실행 의존성
    implementation(libs.bluetape4k.testcontainers)

    // MySQL 테스트 드라이버 의존성
    implementation(libs.testcontainers.mysql)
    runtimeOnly(libs.mysql.connector.j)

    // PostgreSQL 테스트 드라이버 의존성
    implementation(libs.testcontainers.postgresql)
    runtimeOnly(libs.postgresql.driver)

    // Spring Boot 멀티테넌트 웹 예제 의존성
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-autoconfigure-processor")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    runtimeOnly("org.springframework.boot:spring-boot-devtools")

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")

    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }

    implementation(libs.bluetape4k.coroutines)
    implementation(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.reactor.kotlin.extensions)

    // 관찰성 및 모니터링 의존성
    implementation(libs.micrometer.core)
    implementation(libs.micrometer.registry.prometheus)

    // SpringDoc 기반 OpenAPI 3.0 문서화 의존성
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

}
