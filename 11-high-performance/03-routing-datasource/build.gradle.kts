plugins {
    kotlin("plugin.spring")
    id(Plugins.spring_boot)
    id(Plugins.graalvm_native)
}

springBoot {
    mainClass.set("exposed.examples.routing.RoutingDataSourceApplicationKt")

    buildInfo {
        properties {
            additional.put("name", "Routing DataSource Example")
            additional.put("description", "테넌트 + 읽기/쓰기 분리 라우팅 DataSource 예제")
            version = "1.0.0"
            additional.put("java.version", JavaVersion.current())
        }
    }
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(Libs.springBootStarter("jdbc"))
    implementation(Libs.springBootStarter("web"))
    implementation(Libs.springBootStarter("actuator"))

    implementation(platform(Libs.exposed_bom))
    implementation(Libs.exposed_core)
    implementation(Libs.exposed_jdbc)

    runtimeOnly(Libs.h2_v2)
    implementation(Libs.hikaricp)

    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
}
