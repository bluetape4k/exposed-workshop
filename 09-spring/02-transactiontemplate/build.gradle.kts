plugins {
    kotlin("plugin.spring")
}

@Suppress("UnstableApiUsage")
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.exposed_bom))

    testImplementation(project(":exposed-shared-tests"))

    // Exposed
    implementation(Libs.bluetape4k_exposed)
    implementation(Libs.exposed_core)
    implementation(Libs.exposed_dao)
    implementation(Libs.exposed_spring_boot_starter)

    implementation(Libs.h2_v2)
    implementation(Libs.hikaricp)

    // Bluetape4k
    implementation(Libs.bluetape4k_io)

    // Spring Boot
    implementation(Libs.springBootStarter("jdbc"))
    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }

    // 랜덤 데이터를 생성
    implementation(Libs.datafaker)

}
