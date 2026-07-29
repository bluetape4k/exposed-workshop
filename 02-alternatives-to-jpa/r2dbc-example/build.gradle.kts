plugins {
    kotlin("plugin.spring")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.graalvm.native)
}

springBoot {
    mainClass.set("alternative.r2dbc.example.R2dbcApplicationKt")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(libs.bluetape4k.io)
    implementation(libs.bluetape4k.jackson2)
    testImplementation(libs.bluetape4k.junit5)

    // PostgreSQL 테스트 서버 의존성
    implementation(libs.bluetape4k.testcontainers)
    implementation(libs.testcontainers.postgresql)

    // 코루틴 의존성
    implementation(libs.bluetape4k.coroutines)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    // Reactor 의존성
    implementation(libs.reactor.core)
    implementation(libs.reactor.kotlin.extensions)
    testImplementation(libs.reactor.test)

    // R2DBC 애플리케이션 의존성
    implementation(libs.bluetape4k.spring.boot.r2dbc)
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    runtimeOnly(libs.r2dbc.postgresql)
    runtimeOnly(libs.r2dbc.h2)
    runtimeOnly(libs.r2dbc.pool)

    implementation("org.springframework.boot:spring-boot-starter-webflux")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }
}
