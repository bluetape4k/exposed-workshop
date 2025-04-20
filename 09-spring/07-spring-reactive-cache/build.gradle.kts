import jdk.tools.jlink.resources.plugins

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
    implementation(Libs.fury_kotlin)

    // Compressor
    implementation(Libs.lz4_java)
}
