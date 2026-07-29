plugins {
    alias(libs.plugins.exposed)
    kotlin("plugin.spring")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.graalvm.native)
}

exposed {
    migrations {
        tablesPackage = "exposed.examples.springcache.domain"
        databaseUrl = "jdbc:h2:mem:09-spring-06-spring-cache-migrations;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
        databaseUser = "sa"
        databasePassword = ""
    }
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
    testImplementation(project(":exposed-shared-tests"))


    // Exposed ORM 및 DSL 의존성
    implementation(libs.jetbrains.exposed.core)
    implementation(libs.jetbrains.exposed.jdbc)
    implementation(libs.jetbrains.exposed.dao)
    implementation(libs.jetbrains.exposed.java.time)
    implementation(libs.jetbrains.exposed.migration.jdbc)
    implementation(libs.jetbrains.exposed.spring.boot.starter)

    // Bluetape4k 공통 테스트/유틸리티 의존성
    implementation(libs.exposed.jdbc)
    implementation(libs.bluetape4k.io)
    implementation(libs.bluetape4k.jdbc)
    implementation(libs.bluetape4k.redis)
    implementation(libs.bluetape4k.spring.boot.core)
    implementation(libs.bluetape4k.spring.boot.redis)
    implementation(libs.bluetape4k.testcontainers)
    testImplementation(libs.bluetape4k.junit5)

    // 테스트 데이터베이스별 JDBC 드라이버 의존성
    implementation(libs.hikaricp)

    // H2
    runtimeOnly(libs.h2.v2)

    // MySQL 테스트 드라이버 의존성
    implementation(libs.testcontainers.mysql)
    runtimeOnly(libs.mysql.connector.j)

    // PostgreSQL 테스트 드라이버 의존성
    implementation(libs.testcontainers.postgresql)
    runtimeOnly(libs.postgresql.driver)

    // Spring Boot 통합 및 테스트 의존성
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }

    // Redis 기반 Spring Cache 예제 의존성
    implementation(libs.lettuce.core)
    implementation(libs.commons.pool2)

    // 캐시 값 직렬화 코덱 의존성
    implementation(libs.fory.kotlin)
    implementation(libs.kryo)

    // 캐시 값 압축 처리 의존성
    implementation(libs.lz4.java)
    implementation(libs.snappy.java)
    implementation(libs.zstd.jni)
}
