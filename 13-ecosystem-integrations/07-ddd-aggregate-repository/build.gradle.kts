configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(libs.bluetape4k.core)
    implementation(libs.exposed.core)
    implementation(libs.jetbrains.exposed.core)
    implementation(libs.jetbrains.exposed.jdbc)

    runtimeOnly(libs.h2.v2)

    testImplementation(libs.bluetape4k.junit5)
}
