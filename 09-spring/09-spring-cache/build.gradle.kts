plugins {
    kotlin("plugin.spring")
    id(Plugins.spring_boot)
}


springBoot {
    mainClass.set("exposed.examples.springcache.SpringCacheApplicationKt")

    buildInfo {
        properties {
            additional.put("name", "Exposed Cache Application")
            additional.put("description", "Exposed 와 Spring Boot Cache 를 활용한 분산 캐시 예제")
            version = "1.0.0"
            additional.put("java.version", JavaVersion.current())
        }
    }
}

@Suppress("UnstableApiUsage")
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {

    implementation(platform(Libs.exposed_bom))
    testImplementation(project(":exposed-shared-tests"))

    // bluetape4k
    implementation(Libs.bluetape4k_redis)
    implementation(Libs.bluetape4k_testcontainers)
    implementation(Libs.bluetape4k_jdbc)
    testImplementation(Libs.bluetape4k_spring_tests)
    testImplementation(Libs.bluetape4k_junit5)


    // Exposed
    implementation(Libs.bluetape4k_exposed)
    implementation(Libs.exposed_core)
    implementation(Libs.exposed_jdbc)
    implementation(Libs.exposed_dao)
    implementation(Libs.exposed_java_time)
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

    implementation(Libs.springBootStarter("webflux"))
    implementation(Libs.springBootStarter("cache"))
    implementation(Libs.springBootStarter("data-redis"))
    implementation(Libs.springBootStarter("aop"))

    testImplementation(Libs.bluetape4k_spring_tests)
    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }

    // Redis Cache
    implementation(Libs.lettuce_core)
    implementation(Libs.commons_pool2)

    // Codecs
    implementation(Libs.kryo)
    implementation(Libs.fury_kotlin)

    // Compressor
    implementation(Libs.commons_compress)
    implementation(Libs.lz4_java)
    implementation(Libs.snappy_java)
    implementation(Libs.zstd_jni)

    // DataFaker
    implementation(Libs.datafaker)

    // Coroutines
    implementation(Libs.bluetape4k_coroutines)
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Reactor
    implementation(Libs.reactor_netty)
    implementation(Libs.reactor_kotlin_extensions)
    testImplementation(Libs.reactor_test)

    // SpringDoc - OpenAPI 3.0
    implementation(Libs.springdoc_openapi_starter_webflux_ui)

    implementation(Libs.hibernate_validator)
}
