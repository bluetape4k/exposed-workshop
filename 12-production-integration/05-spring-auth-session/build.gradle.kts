plugins {
    alias(libs.plugins.exposed)
    kotlin("plugin.spring")
    alias(libs.plugins.spring.boot)
}

exposed {
    migrations {
        tablesPackage = "exposed.examples.spring.auth.repository"
        databaseUrl = "jdbc:h2:mem:12-production-integration-05-spring-auth-session-migrations;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
        databaseUser = "sa"
        databasePassword = ""
    }
}

springBoot {
    mainClass.set("exposed.examples.spring.auth.SpringAuthSessionApplicationKt")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {

    implementation(libs.jetbrains.exposed.core)
    implementation(libs.jetbrains.exposed.jdbc)
    implementation(libs.hikaricp)

    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")

    runtimeOnly(libs.h2.v2)

    testImplementation(libs.bluetape4k.junit5)
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }
}
