plugins {
    kotlin("plugin.spring")
    kotlin("plugin.allopen")
    kotlin("plugin.noarg")
    kotlin("plugin.jpa")
    kotlin("kapt")

    id(Plugins.spring_boot)
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

dependencies {
    implementation(Libs.bluetape4k_hibernate_reactive)
    implementation(Libs.bluetape4k_coroutines)
    implementation(Libs.bluetape4k_testcontainers)

    testImplementation(Libs.bluetape4k_crypto)
    testImplementation(Libs.bluetape4k_jackson)
    testImplementation(Libs.bluetape4k_junit5)
    testImplementation(Libs.bluetape4k_spring_tests)

    api(Libs.jakarta_annotation_api)
    api(Libs.jakarta_persistence_api)

    // Hibernate Reactive
    api(Libs.hibernate_reactive_core)
    implementation("com.ongres.scram:common:2.1") // vert.x sql client 에서 사용하는데 제외되었다.
    implementation("com.ongres.scram:client:2.1") // vert.x sql client 에서 사용하는데 제외되었다.

    // NOTE: hibernate-reactive 는 querydsl 을 사용하지 못한다. 대신 jpamodelgen 을 사용합니다.
    kapt(Libs.hibernate_jpamodelgen)
    kaptTest(Libs.hibernate_jpamodelgen)

    // Mutiny & Coroutines
    implementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Vaidators
    implementation(Libs.hibernate_validator)
    runtimeOnly(Libs.jakarta_validation_api)

    // Spring Boot Webflux
    implementation(Libs.springBoot("autoconfigure"))
    implementation(Libs.springBootStarter("validation"))
    implementation(Libs.springBootStarter("webflux"))
    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }

    // Postgres
    implementation(Libs.testcontainers_postgresql)
    implementation(Libs.vertx_pg_client)

    // Testcontainers MySQL 에서 검증을 위해 사용하기 위해 불가피하게 필요합니다
    // reactive 방식에서는 항상 verx-pg-client 를 사용합니다
    implementation(Libs.postgresql_driver)

    // MySQL
//    implementation(Libs.testcontainers_mysql)
//    implementation(Libs.vertx_mysql_client)
//    implementation(Libs.mysql_connector_j)

    // Monitoring
    implementation(Libs.micrometer_core)
    implementation(Libs.micrometer_registry_prometheus)

    // SpringDoc - OpenAPI 3.0
    implementation(Libs.springdoc_openapi_starter_webflux_ui)
}
