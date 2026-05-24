plugins {
    kotlin("plugin.spring")
    alias(libs.plugins.spring.boot)
}

springBoot {
    mainClass.set("exposed.examples.spring.architecture.SpringArchitectureApplicationKt")

    buildInfo {
        properties {
            additional.put("name", "Spring Boot application architecture with Exposed")
            additional.put("java.version", JavaVersion.current())
        }
    }
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {

    implementation(libs.jetbrains.exposed.core)
    implementation(libs.jetbrains.exposed.jdbc)
    implementation(libs.jetbrains.exposed.spring.boot.starter)
    implementation(libs.hikaricp)

    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.web)

    runtimeOnly(libs.h2.v2)

    testImplementation(libs.bluetape4k.junit5)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.test) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }
}

