plugins {
    alias(libs.plugins.exposed)
    kotlin("plugin.spring")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.graalvm.native)
    alias(libs.plugins.gatling)
}

exposed {
    migrations {
        tablesPackage = "exposed.workshop.springwebflux"
        databaseUrl = "jdbc:h2:mem:01-spring-boot-spring-webflux-exposed-migrations;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
        databaseUser = "sa"
        databasePassword = ""
    }
}

springBoot {
    mainClass.set("exposed.workshop.springwebflux.SpringWebfluxApplicationKt")

    buildInfo {
        properties {
            additional.put("name", "Webflux + Exposed Application")
            additional.put("description", "Webflux + Exposed Application")
            version = "1.0.0"
            additional.put("java.version", JavaVersion.current())
        }
    }
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    testImplementation(project(":exposed-shared-tests"))

    // JetBrains Exposed 의존성
    implementation(libs.jetbrains.exposed.core)
    implementation(libs.jetbrains.exposed.jdbc)
    implementation(libs.jetbrains.exposed.java.time)
    implementation(libs.jetbrains.exposed.spring.boot.starter)

    // bluetape4k 공통 의존성
    implementation(libs.exposed.core)
    implementation(libs.bluetape4k.io)
    implementation(libs.bluetape4k.jackson2)
    implementation(libs.bluetape4k.jdbc)
    testImplementation(libs.bluetape4k.spring.boot.core)
    testRuntimeOnly(libs.bluetape4k.virtualthread.jdk25)
    implementation(libs.bluetape4k.testcontainers)

    // 데이터베이스 드라이버
    runtimeOnly(libs.hikaricp)

    // H2
    runtimeOnly(libs.h2.v2)

    // MySQL 드라이버와 Testcontainers 의존성
    implementation(libs.testcontainers.mysql)
    runtimeOnly(libs.mysql.connector.j)

    // PostgreSQL 드라이버와 Testcontainers 의존성
    implementation(libs.testcontainers.postgresql)
    runtimeOnly(libs.postgresql.driver)

    // Spring Boot WebFlux 애플리케이션 의존성
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-autoconfigure-processor")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    runtimeOnly("org.springframework.boot:spring-boot-devtools")

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    testImplementation(libs.bluetape4k.spring.boot.core)
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }

    // 코루틴 의존성
    implementation(libs.bluetape4k.coroutines)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    // Reactor 의존성
    implementation(libs.reactor.netty)
    implementation(libs.reactor.kotlin.extensions)
    testImplementation(libs.reactor.test)

    // 모니터링 의존성
    implementation(libs.micrometer.core)
    implementation(libs.micrometer.registry.prometheus)

    // SpringDoc OpenAPI 3.0 UI 의존성
    implementation(libs.springdoc.openapi.starter.webflux.ui)

    // Gatling 부하 테스트 의존성
    implementation(libs.gatling.app)
    implementation(libs.gatling.core.java)
    implementation(libs.gatling.http.java)
    implementation(libs.gatling.recorder)
    implementation(libs.gatling.charts.highcharts)
    testImplementation(libs.gatling.test.framework)
}
