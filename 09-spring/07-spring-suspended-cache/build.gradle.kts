plugins {
    kotlin("plugin.spring")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.graalvm.native)
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

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {

    testImplementation(project(":exposed-shared-tests"))

    // bluetape4k
    implementation(libs.bluetape4k.io)
    implementation(libs.bluetape4k.grpc)
    implementation(libs.bluetape4k.redis)
    implementation(libs.bluetape4k.testcontainers)
    // Exposed
    implementation(libs.bluetape4k.exposed)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.java.time)
    testImplementation(libs.bluetape4k.junit5)
    implementation(libs.exposed.spring.boot.starter)

    // Database Drivers
    implementation(libs.hikaricp)

    // H2
    runtimeOnly(libs.h2.v2)

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }

    // Redis Cache
    runtimeOnly(libs.lettuce.core)
    runtimeOnly(libs.commons.pool2)

    // Codecs
    runtimeOnly(libs.fory.kotlin)
    runtimeOnly(libs.kryo)

    // Compressor
    runtimeOnly(libs.lz4.java)
    runtimeOnly(libs.snappy.java)
    runtimeOnly(libs.zstd.jni)

    // Coroutines
    implementation(enforcedPlatform(libs.kotlinx.coroutines.bom))
    implementation(libs.bluetape4k.coroutines)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    // Reactor
    implementation(libs.reactor.netty)
    implementation(libs.reactor.kotlin.extensions)
    testImplementation(libs.reactor.test)
}
