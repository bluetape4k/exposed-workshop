plugins {
    alias(libs.plugins.exposed)
    kotlin("plugin.spring")
    alias(libs.plugins.spring.boot)
}

exposed {
    migrations {
        tablesPackage = "exposed.multitenant.security.domain"
        databaseUrl = "jdbc:h2:mem:10-multi-tenant-06-spring-security-tenant-authorization-spring-web-migrations;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
        databaseUser = "sa"
        databasePassword = ""
    }
}

springBoot {
    buildInfo()
}

dependencies {
    implementation(libs.jetbrains.exposed.core)
    implementation(libs.jetbrains.exposed.jdbc)
    implementation(libs.jetbrains.exposed.spring.boot.starter)

    implementation(libs.hikaricp)
    runtimeOnly(libs.h2.v2)

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")

    testImplementation(libs.bluetape4k.junit5)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }
}
