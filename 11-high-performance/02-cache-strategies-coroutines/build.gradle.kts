plugins {
    alias(libs.plugins.exposed)
    kotlin("plugin.spring")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.graalvm.native)
}

exposed {
    migrations {
        tablesPackage = "exposed.examples.cache.coroutines.domain"
        databaseUrl = "jdbc:h2:mem:11-high-performance-02-cache-strategies-coroutines-migrations;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
        databaseUser = "sa"
        databasePassword = ""
    }
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

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    testImplementation(project(":exposed-shared-tests"))

    // Exposed ORM 및 DSL 참조 라이브러리
    implementation(libs.jetbrains.exposed.core)
    implementation(libs.jetbrains.exposed.jdbc)
    implementation(libs.jetbrains.exposed.dao)
    implementation(libs.jetbrains.exposed.java.time)
    implementation(libs.jetbrains.exposed.spring.boot.starter)

    // Bluetape4k 공통 테스트/유틸리티 참조 라이브러리
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc.redisson)
    implementation(libs.bluetape4k.idgenerators)
    implementation(libs.bluetape4k.redis)
    implementation(libs.bluetape4k.testcontainers)
    testImplementation(libs.bluetape4k.junit5)
    testImplementation(libs.bluetape4k.spring.boot.core)

    // 캐시 값 직렬화 코덱 참조 라이브러리
    runtimeOnly(libs.fory.kotlin)
    runtimeOnly(libs.kryo)

    // 캐시 값 압축 처리 참조 라이브러리
    runtimeOnly(libs.lz4.java)
    runtimeOnly(libs.snappy.java)
    runtimeOnly(libs.zstd.jni)

    // Near Cache 전략 검증 참조 라이브러리
    implementation(libs.caffeine)

    // 테스트 데이터베이스별 JDBC 드라이버 참조 라이브러리
    implementation(libs.hikaricp)

    // H2
    runtimeOnly(libs.h2.v2)

    // MySQL 테스트 드라이버 참조 라이브러리
    implementation(libs.testcontainers.mysql)
    runtimeOnly(libs.mysql.connector.j)

    // PostgreSQL 테스트 드라이버 참조 라이브러리
    implementation(libs.testcontainers.postgresql)
    runtimeOnly(libs.postgresql.driver)

    // Spring Boot 캐시/웹 예제 참조 라이브러리
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }

    implementation(libs.datafaker)

    // 코루틴 기반 캐시/트랜잭션 예제 참조 라이브러리
    implementation(libs.bluetape4k.coroutines)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    // Reactor 어댑터 예제 참조 라이브러리
    implementation(libs.reactor.core)
    implementation(libs.reactor.kotlin.extensions)
    testImplementation(libs.reactor.test)
}
