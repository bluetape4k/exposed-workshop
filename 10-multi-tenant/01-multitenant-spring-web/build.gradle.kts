plugins {
    kotlin("plugin.spring")
    id(Plugins.spring_boot)
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

@Suppress("UnstableApiUsage")
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {


testImplementation(project(":exposed-shared-tests"))

    // bluetape4k
    implementation(Libs.bluetape4k_io)
    implementation(Libs.bluetape4k_jdbc)
    testImplementation(Libs.bluetape4k_spring_tests)

    // Exposed
    implementation(Libs.bluetape4k_exposed)
    implementation(Libs.exposed_core)
    implementation(Libs.exposed_jdbc)
    implementation(Libs.exposed_dao)
    implementation(Libs.exposed_java_time)
    implementation(Libs.exposed_migration)
    implementation(Libs.exposed_spring_boot_starter)

    // Database Drivers
    implementation(Libs.hikaricp)

    // H2
    implementation(Libs.h2_v2)

    // MySQL
    implementation(Libs.bluetape4k_testcontainers)
    implementation(Libs.testcontainers_mysql)
    implementation(Libs.mysql_connector_j)

    // PostgreSQL
    implementation(Libs.bluetape4k_testcontainers)
    implementation(Libs.testcontainers_postgresql)
    implementation(Libs.postgresql_driver)

    // Spring Boot
    implementation(Libs.springBoot("autoconfigure"))
    annotationProcessor(Libs.springBoot("autoconfigure-processor"))
    annotationProcessor(Libs.springBoot("configuration-processor"))
    runtimeOnly(Libs.springBoot("devtools"))

    implementation(Libs.springBootStarter("web"))
    implementation(Libs.springBootStarter("aop"))
    implementation(Libs.springBootStarter("actuator"))
    implementation(Libs.springBootStarter("validation"))

    testImplementation(Libs.bluetape4k_spring_tests)
    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }

    testImplementation(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)
    testImplementation(Libs.reactor_kotlin_extensions)

    // Monitoring
    implementation(Libs.micrometer_core)
    implementation(Libs.micrometer_registry_prometheus)

    // SpringDoc - OpenAPI 3.0
    implementation(Libs.springdoc_openapi_starter_webmvc_ui)

}
