configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

tasks.test {
    // DuckDB JDBC loads a native library.
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

dependencies {
    implementation(libs.bluetape4k.core)
    implementation(libs.exposed.duckdb)
    implementation(libs.jetbrains.exposed.core)
    implementation(libs.jetbrains.exposed.jdbc)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.bluetape4k.junit5)
    testImplementation(libs.kotlinx.coroutines.test)
}
