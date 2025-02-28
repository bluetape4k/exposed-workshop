plugins {
    kotlin("plugin.serialization")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.exposed_bom))

    implementation(project(":exposed-shared"))
    testImplementation(project(":exposed-shared-tests"))

    implementation(Libs.bluetape4k_exposed)
    implementation(Libs.exposed_core)
    implementation(Libs.exposed_jdbc)
    implementation(Libs.exposed_dao)

    // Id Generators
    // - Snowflake
    // - Timebased UUID
    // - Ksuids
    // Identifier 자동 생성
    implementation(Libs.bluetape4k_idgenerators)
    implementation(Libs.java_uuid_generator)

    // Coroutines
    implementation(Libs.bluetape4k_coroutines)
    implementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
