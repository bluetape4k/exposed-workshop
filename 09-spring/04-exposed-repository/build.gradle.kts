plugins {
    kotlin("plugin.spring")
}

@Suppress("UnstableApiUsage")
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.exposed_bom))
    implementation(project(":exposed-shared"))
    testImplementation(project(":exposed-shared-tests"))

    // Exposed
    implementation(Libs.exposed_core)
    implementation(Libs.exposed_dao)
    implementation(Libs.exposed_spring_boot_starter)

    // Bluetape4k
    implementation(Libs.bluetape4k_io)
    implementation(Libs.bluetape4k_jdbc)
    implementation(Libs.bluetape4k_spring_core)
    testImplementation(Libs.bluetape4k_spring_tests)

    // Spring Boot
    implementation(Libs.springBootStarter("jdbc"))
    implementation(Libs.springData("commons"))
    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }

    implementation(Libs.datafaker)

    // Coroutines
    implementation(Libs.bluetape4k_coroutines)
    testImplementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)
}
