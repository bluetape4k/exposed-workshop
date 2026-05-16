plugins {
    kotlin("plugin.spring")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.graalvm.native)
    alias(libs.plugins.gatling)
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

    // Exposed
    implementation(libs.jetbrains.exposed.core)
    implementation(libs.jetbrains.exposed.jdbc)
    implementation(libs.jetbrains.exposed.java.time)
    implementation(libs.jetbrains.exposed.spring.boot.starter)

    // bluetape4k
    implementation(libs.exposed.core)
    implementation(libs.bluetape4k.io)
    implementation(libs.bluetape4k.jackson2)
    implementation(libs.bluetape4k.jdbc)
    testImplementation(libs.bluetape4k.spring.boot4.core)
    implementation(libs.bluetape4k.testcontainers)

    // Database Drivers
    runtimeOnly(libs.hikaricp)

    // H2
    runtimeOnly(libs.h2.v2)

    // MySQL
    implementation(libs.testcontainers.mysql)
    runtimeOnly(libs.mysql.connector.j)

    // PostgreSQL
    implementation(libs.testcontainers.postgresql)
    runtimeOnly(libs.postgresql.driver)

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-autoconfigure-processor")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    runtimeOnly("org.springframework.boot:spring-boot-devtools")

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    testImplementation(libs.bluetape4k.spring.boot4.core)
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }

    // Coroutines
    implementation(libs.bluetape4k.coroutines)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    // Reactor
    implementation(libs.reactor.netty)
    implementation(libs.reactor.kotlin.extensions)
    testImplementation(libs.reactor.test)

    // Monitoring
    implementation(libs.micrometer.core)
    implementation(libs.micrometer.registry.prometheus)

    // SpringDoc - OpenAPI 3.0
    implementation(libs.springdoc.openapi.starter.webflux.ui)

    // Gatling
    implementation(libs.gatling.app)
    implementation(libs.gatling.core.java)
    implementation(libs.gatling.http.java)
    implementation(libs.gatling.recorder)
    implementation(libs.gatling.charts.highcharts)
    testImplementation(libs.gatling.test.framework)
}
