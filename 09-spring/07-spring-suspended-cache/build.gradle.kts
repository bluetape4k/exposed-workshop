plugins {
    alias(libs.plugins.exposed)
    kotlin("plugin.spring")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.graalvm.native)
}

exposed {
    migrations {
        tablesPackage = "exposed.examples.suspendedcache.domain"
        databaseUrl = "jdbc:h2:mem:09-spring-07-spring-suspended-cache-migrations;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
        databaseUser = "sa"
        databasePassword = ""
    }
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

    // Bluetape4k 공통 테스트/유틸리티 의존성
    implementation(libs.bluetape4k.io)
    implementation(libs.bluetape4k.grpc)
    implementation(libs.bluetape4k.redis)
    implementation(libs.bluetape4k.testcontainers)
    // Exposed ORM 및 DSL 의존성
    implementation(libs.exposed.core)
    implementation(libs.jetbrains.exposed.core)
    implementation(libs.jetbrains.exposed.jdbc)
    implementation(libs.jetbrains.exposed.dao)
    implementation(libs.jetbrains.exposed.java.time)
    testImplementation(libs.bluetape4k.junit5)
    implementation(libs.jetbrains.exposed.spring.boot.starter)

    // 테스트 데이터베이스별 JDBC 드라이버 의존성
    implementation(libs.hikaricp)

    // H2
    runtimeOnly(libs.h2.v2)

    // Spring Boot 통합 및 테스트 의존성
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }

    // Redis 기반 Spring Cache 예제 의존성
    runtimeOnly(libs.lettuce.core)
    runtimeOnly(libs.commons.pool2)

    // 캐시 값 직렬화 코덱 의존성
    runtimeOnly(libs.fory.kotlin)
    runtimeOnly(libs.kryo)

    // 캐시 값 압축 처리 의존성
    runtimeOnly(libs.lz4.java)
    runtimeOnly(libs.snappy.java)
    runtimeOnly(libs.zstd.jni)

    // 코루틴 기반 트랜잭션 및 비동기 흐름 의존성
    implementation(enforcedPlatform(libs.kotlinx.coroutines.bom))
    implementation(libs.bluetape4k.coroutines)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    // WebFlux/Reactor 어댑터 예제 의존성
    implementation(libs.reactor.netty)
    implementation(libs.reactor.kotlin.extensions)
    testImplementation(libs.reactor.test)
}
