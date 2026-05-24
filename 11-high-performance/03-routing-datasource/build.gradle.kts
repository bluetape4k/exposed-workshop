plugins {
    kotlin("plugin.spring")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.graalvm.native)
}

springBoot {
    mainClass.set("exposed.examples.routing.RoutingDataSourceApplicationKt")

    buildInfo {
        properties {
            additional.put("name", "Routing DataSource Example")
            additional.put("description", "테넌트 + 읽기/쓰기 분리 라우팅 DataSource 예제")
            version = "1.0.0"
            additional.put("java.version", JavaVersion.current())
        }
    }
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation(libs.jetbrains.exposed.core)
    implementation(libs.jetbrains.exposed.jdbc)

    runtimeOnly(libs.h2.v2)
    implementation(libs.hikaricp)

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation(libs.spring.boot.starter.webmvc.test)
}
