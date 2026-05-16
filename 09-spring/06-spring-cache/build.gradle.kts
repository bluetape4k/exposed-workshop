plugins {
    kotlin("plugin.spring")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.graalvm.native)
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

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {

    implementation(platform(libs.jetbrains.exposed.bom))
    testImplementation(project(":exposed-shared-tests"))


    // Exposed
    implementation(libs.jetbrains.exposed.core)
    implementation(libs.jetbrains.exposed.jdbc)
    implementation(libs.jetbrains.exposed.dao)
    implementation(libs.jetbrains.exposed.java.time)
    implementation(libs.jetbrains.exposed.migration.jdbc)
    implementation(libs.jetbrains.exposed.spring.boot.starter)

    // Bluetape4k
    implementation(libs.exposed.jdbc)
    implementation(libs.bluetape4k.io)
    implementation(libs.bluetape4k.jdbc)
    implementation(libs.bluetape4k.redis)
    implementation(libs.bluetape4k.spring.boot4.core)
    implementation(libs.bluetape4k.spring.boot4.redis)
    implementation(libs.bluetape4k.testcontainers)
    testImplementation(libs.bluetape4k.junit5)

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
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }

    // Redis Cache
    implementation(libs.lettuce.core)
    implementation(libs.commons.pool2)

    // Codecs
    implementation(libs.fory.kotlin)
    implementation(libs.kryo)

    // Compressor
    implementation(libs.lz4.java)
    implementation(libs.snappy.java)
    implementation(libs.zstd.jni)
}
