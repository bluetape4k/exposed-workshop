configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

val includeRedisIntegration =
    providers.gradleProperty("includeRedisIntegration")
        .map(String::toBoolean)
        .orElse(false)

dependencies {
    implementation(libs.exposed.jdbc.lettuce)
    implementation(libs.jetbrains.exposed.core)
    implementation(libs.jetbrains.exposed.jdbc)
    implementation(libs.bluetape4k.coroutines)
    implementation(libs.kotlinx.coroutines.core)

    runtimeOnly(libs.h2.v2)

    testImplementation(project(":exposed-shared-tests"))
    testImplementation(libs.bluetape4k.junit5)
    testImplementation(libs.bluetape4k.testcontainers)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.testcontainers)
    testImplementation(libs.mockk)

    testRuntimeOnly(libs.lettuce.core)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform {
        if (includeRedisIntegration.get()) {
            includeTags("redis")
        } else {
            excludeTags("redis")
        }
    }
}
