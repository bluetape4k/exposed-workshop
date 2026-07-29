plugins {
    kotlin("plugin.spring")
    kotlin("plugin.allopen")
    kotlin("plugin.noarg")
    kotlin("plugin.jpa")
    kotlin("kapt")

    alias(libs.plugins.spring.boot)
//    alias(libs.plugins.graalvm.native)
}


// JPA Entities 들을 Java와 같이 모두 override 가능하게 합니다 (Kotlin 은 기본이 final 입니다)
// 이렇게 해야 association의 proxy 가 만들어집니다.
// https://kotlinlang.org/docs/reference/compiler-plugins.html
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}

kapt {
    includeCompileClasspath = true
    correctErrorTypes = true
    showProcessorStats = true

    javacOptions {
        option("--add-modules", "java.base")
    }
}

springBoot {
    mainClass.set("alternatives.hibernate.reactive.example.HibernateReactiveApplicationKt")

    buildInfo {
        properties {
            additional.put("name", "Spring Webflux with Hibernate Reactive")
            additional.put("java.version", JavaVersion.current())
        }
    }
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencyManagement {
    dependencies {
        val vertxVersion = libs.versions.vertx.get()
        dependency("io.vertx:vertx-core:$vertxVersion")
        dependency("io.vertx:vertx-core-logging:$vertxVersion")
        dependency("io.vertx:vertx-lang-kotlin:$vertxVersion")
        dependency("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
        dependency("io.vertx:vertx-sql-client:$vertxVersion")
        dependency("io.vertx:vertx-sql-client-templates:$vertxVersion")
        dependency("io.vertx:vertx-mysql-client:$vertxVersion")
        dependency("io.vertx:vertx-pg-client:$vertxVersion")
    }
}

dependencies {
    implementation(libs.bluetape4k.testcontainers)

    implementation(libs.bluetape4k.jackson2)
    implementation(libs.bluetape4k.vertx)
    testImplementation(libs.bluetape4k.junit5)
    testImplementation(libs.bluetape4k.spring.boot.core)

    api(libs.jakarta.annotation.api)
    api(libs.jakarta.persistence.api)
    api(libs.jakarta.transaction.api)

    // Hibernate Reactive 의존성
    implementation(libs.bluetape4k.hibernate.reactive)
    implementation(libs.hibernate.reactive.core)
    implementation("com.ongres.scram:scram-client:3.2") // vert.x sql client 에서 사용하는데 제외되었다.

    // NOTE: hibernate-reactive 는 querydsl 을 사용하지 못한다. 대신 jpamodelgen 을 사용합니다.
    kapt(libs.hibernate.jpamodelgen)
    kaptTest(libs.hibernate.jpamodelgen)

    // Mutiny 및 코루틴 의존성
    implementation(libs.bluetape4k.coroutines)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    // Validation 의존성
    implementation(libs.hibernate.validator)
    implementation(libs.jakarta.validation.api)

    // Spring Boot WebFlux 애플리케이션 의존성
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }

    // PostgreSQL reactive client와 Testcontainers 의존성
    implementation(libs.testcontainers.postgresql)
    implementation(libs.vertx.pg.client)

    // Testcontainers MySQL 에서 검증을 위해 사용하기 위해 불가피하게 필요합니다
    // reactive 방식에서는 항상 verx-pg-client 를 사용합니다
    runtimeOnly(libs.postgresql.driver)

    // 모니터링 의존성
    implementation(libs.micrometer.core)
    implementation(libs.micrometer.registry.prometheus)

    // SpringDoc OpenAPI 3.0 UI 의존성
    implementation(libs.springdoc.openapi.starter.webflux.ui)
}
