plugins {
    alias(libs.plugins.exposed)
    kotlin("plugin.spring")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.graalvm.native)
}

exposed {
    migrations {
        tablesPackage = "exposed.examples.cache.domain"
        databaseUrl = "jdbc:h2:mem:11-high-performance-01-cache-strategies-migrations;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
        databaseUser = "sa"
        databasePassword = ""
    }
}


springBoot {
    mainClass.set("exposed.examples.cache.CacheStrategyApplicationKt")

    buildInfo {
        properties {
            additional.put("name", "Exposed + Redisson Cache Strategy Application")
            additional.put("description", "Exposed + Redisson 을 활용한 다양한 캐시 전략 예제")
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

    // Exposed
    implementation(libs.jetbrains.exposed.core)
    implementation(libs.jetbrains.exposed.jdbc)
    implementation(libs.jetbrains.exposed.dao)
    implementation(libs.jetbrains.exposed.java.time)
    implementation(libs.jetbrains.exposed.spring.boot.starter)

    // bluetape4k
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc.redisson)
    implementation(libs.bluetape4k.idgenerators)
    implementation(libs.bluetape4k.redis)
    implementation(libs.bluetape4k.testcontainers)
    testImplementation(libs.bluetape4k.junit5)
    testImplementation(libs.bluetape4k.spring.boot.core)

    // Codecs
    runtimeOnly(libs.fory.kotlin)
    runtimeOnly(libs.kryo)

    // Compressor
    runtimeOnly(libs.lz4.java)
    runtimeOnly(libs.snappy.java)
    runtimeOnly(libs.zstd.jni)

    // Near Cache
    implementation(libs.caffeine)

    // Database Drivers
    implementation(libs.hikaricp)

    // H2
    runtimeOnly(libs.h2.v2)

    // MySQL
    implementation(libs.testcontainers.mysql)
    runtimeOnly(libs.mysql.connector.j)

    // PostgreSQL
    implementation(libs.testcontainers.postgresql)
    runtimeOnly(libs.postgresql.driver)

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }

    implementation(libs.datafaker)

    implementation(libs.bluetape4k.coroutines)
    implementation(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    implementation(libs.reactor.kotlin.extensions)
}
