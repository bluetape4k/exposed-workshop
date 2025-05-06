plugins {
    kotlin("plugin.spring")
    id(Plugins.spring_boot)
}


springBoot {
    mainClass.set("exposed.examples.cache.coroutines.CacheStrategyApplicationKt")

    buildInfo {
        properties {
            additional.put("name", "Exposed + Redisson Cache Strategy Application")
            additional.put("description", "Exposed + Redisson 을 활용한 다양한 캐시 전략 예제")
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
    implementation(Libs.bluetape4k_idgenerators)
    implementation(Libs.bluetape4k_redis)
    implementation(Libs.bluetape4k_testcontainers)
    testImplementation(Libs.bluetape4k_junit5)
    testImplementation(Libs.bluetape4k_spring_tests)

    // Exposed
    implementation(Libs.bluetape4k_exposed)
    implementation(Libs.bluetape4k_exposed_redisson)
    implementation(Libs.exposed_core)
    implementation(Libs.exposed_jdbc)
    implementation(Libs.exposed_dao)
    implementation(Libs.exposed_java_time)
    implementation(Libs.exposed_spring_boot_starter)

    // Compressor
    implementation(Libs.lz4_java)

    // Codecs
    implementation(Libs.fury_kotlin)

    // Near Cache
    implementation(Libs.caffeine)

    // Database Drivers
    implementation(Libs.hikaricp)

    // H2
    implementation(Libs.h2_v2)

    // Spring Boot
    implementation(Libs.springBootStarter("webflux"))
    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }

    implementation(Libs.datafaker)

    // Coroutines
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Reactor
    implementation(Libs.reactor_core)
    implementation(Libs.reactor_kotlin_extensions)
    testImplementation(Libs.reactor_test)
}
