plugins {
    kotlin("plugin.spring")
    id(Plugins.spring_boot)
}

springBoot {
    mainClass.set("exposed.examples.suspendedcache.SpringSuspendedCacheApplicationKt")

    buildInfo {
        properties {
            additional.put("name", "Exposed SuspendedCache Application")
            additional.put("description", "Exposed 와 SuspendedCacheRepository를 이용하여 비동기방식으로 DB 및 Redis에 접근하는 예제")
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
    implementation(Libs.bluetape4k_grpc)
    implementation(Libs.bluetape4k_testcontainers)
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

    // Spring Boot
    implementation(Libs.springBootStarter("webflux"))
    implementation(Libs.springBootStarter("cache"))
    implementation(Libs.springBootStarter("data-redis"))
    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }

    // Redis Cache
    implementation(Libs.lettuce_core)
    implementation(Libs.commons_pool2)

    // Codecs
    implementation(Libs.fory_kotlin)
    // Compressor
    implementation(Libs.lz4_java)

    // Coroutines
    implementation(Libs.bluetape4k_coroutines)
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Reactor
    implementation(Libs.reactor_netty)
    implementation(Libs.reactor_kotlin_extensions)
    testImplementation(Libs.reactor_test)
}
