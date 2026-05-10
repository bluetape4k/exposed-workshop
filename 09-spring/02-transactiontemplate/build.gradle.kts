plugins {
    kotlin("plugin.spring")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.graalvm.native)
}

@Suppress("UnstableApiUsage")
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(libs.exposed.bom))

    testImplementation(project(":exposed-shared-tests"))

    // Exposed
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.spring.boot.starter)

    // Bluetape4k
    implementation(libs.bluetape4k.exposed.core)
    implementation(libs.bluetape4k.exposed.dao)
    implementation(libs.bluetape4k.io)
    testImplementation(libs.bluetape4k.junit5)

    runtimeOnly(libs.h2.v2)
    runtimeOnly(libs.hikaricp)

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }

    // 랜덤 데이터를 생성
    implementation(libs.datafaker)
}
