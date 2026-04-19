import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.report.ReportMergeTask
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    base
    // jacoco
    kotlin("jvm") version Versions.kotlin

    // see: https://kotlinlang.org/docs/reference/compiler-plugins.html
    kotlin("plugin.spring") version Versions.kotlin apply false
    kotlin("plugin.allopen") version Versions.kotlin apply false
    kotlin("plugin.noarg") version Versions.kotlin apply false
    kotlin("plugin.jpa") version Versions.kotlin apply false
    kotlin("plugin.serialization") version Versions.kotlin apply false
    id("org.jetbrains.kotlinx.atomicfu") version Versions.kotlinx_atomicfu
    kotlin("kapt") version Versions.kotlin apply false

    id(Plugins.detekt) version Plugins.Versions.detekt
    id(Plugins.kover) version Plugins.Versions.kover

    id(Plugins.dependency_management) version Plugins.Versions.dependency_management
    id(Plugins.spring_boot) version Plugins.Versions.spring_boot apply false

    id(Plugins.testLogger) version Plugins.Versions.testLogger
    id(Plugins.kotlinx_benchmark) version Plugins.Versions.kotlinx_benchmark apply false
    id(Plugins.graalvm_native) version Plugins.Versions.graalvm_native apply false
}

allprojects {
    repositories {
        mavenCentral()
        google()

        // bluetape4k snapshot 버전 사용 시만 사용하세요.
//        maven {
//            name = "central-snapshots"
//            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
//        }
    }

    // bluetape4k snapshot 버전 사용 시만 사용하세요.
//    configurations.all {
//        resolutionStrategy.cacheChangingModulesFor(1, TimeUnit.DAYS)
//    }
}

subprojects {
    apply {
        plugin<JavaLibraryPlugin>()
        // Kotlin 1.9.20 부터는 pluginId 를 지정해줘야 합니다.
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jetbrains.kotlinx.atomicfu")
        plugin(Plugins.dependency_management)
        plugin(Plugins.testLogger)
        plugin(Plugins.kover)
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    kotlin {
        jvmToolchain(21)
        compilerOptions {
            languageVersion.set(KotlinVersion.KOTLIN_2_3)
            apiVersion.set(KotlinVersion.KOTLIN_2_3)
            freeCompilerArgs = listOf(
                "-Xjsr305=strict",
                "-jvm-default=enable",
                // "-Xinline-classes",          // Kotlin 2.3 부터는 필요 없음 
                "-Xstring-concat=indy",         // since Kotlin 1.4.20 for JVM 9+
                "-Xcontext-parameters",         // since Kotlin 1.6
                "-Xannotation-default-target=param-property"
            )
            val experimentalAnnotations = listOf(
                "kotlin.RequiresOptIn",
                "kotlin.ExperimentalStdlibApi",
                "kotlin.contracts.ExperimentalContracts",
                "kotlin.experimental.ExperimentalTypeInference",
                "kotlinx.coroutines.ExperimentalCoroutinesApi",
                "kotlinx.coroutines.InternalCoroutinesApi",
                "kotlinx.coroutines.FlowPreview",
                "kotlinx.coroutines.DelicateCoroutinesApi",
            )
            freeCompilerArgs.addAll(experimentalAnnotations.map { "-opt-in=$it" })
        }
    }

    atomicfu {
        transformJvm = true
        jvmVariant = "VH"     //  FU, VH, BOTH
    }

    tasks {
        compileJava {
            options.isIncremental = true
        }

        compileKotlin {
            compilerOptions {
                incremental = true
            }
        }

        // 멀티 모듈들을 테스트 시에만 동시에 실행되지 않게 하기 위해 Mutex 를 활용합니다.
        abstract class TestMutexService: BuildService<BuildServiceParameters.None>

        val testMutex = gradle.sharedServices.registerIfAbsent(
            "test-mutex",
            TestMutexService::class
        ) {
            maxParallelUsages.set(1)
        }

        test {
            // 멀티 모듈들을 테스트 시에만 동시에 실행되지 않게 하기 위해 Mutex 를 활용합니다.
            usesService(testMutex)

            useJUnitPlatform()

            // 테스트 시 아래와 같은 예외 메시지를 제거하기 위해서
            // OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
            jvmArgs(
                "-Xshare:off",
                "-Xms2G",
                "-Xmx4G",
                "-XX:+UseG1GC",
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+EnableDynamicAgentLoading",
                "--enable-preview",
                "-Didea.io.use.nio2=true",
                "-Duser.language=en",
                "-Duser.country=US"
            )

            // Gradle 프로퍼티를 JVM 시스템 프로퍼티로 전달하여 테스트 대상 DB를 제어합니다.
            // 사용 예:
            //   ./gradlew test -PuseFastDB=true          → H2 만 테스트
            //   ./gradlew test -PuseDB=H2,POSTGRESQL      → 지정한 DB만 테스트
            project.findProperty("useFastDB")?.toString()?.let {
                systemProperty("exposed.test.useFastDB", it)
            }
            project.findProperty("useDB")?.toString()?.let {
                systemProperty("exposed.test.useDB", it)
            }

            testLogging {
                showExceptions = true
                showCauses = true
                showStackTraces = true

                events("failed")
            }
        }

        testlogger {
            theme = com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA_PARALLEL
            showFullStackTraces = true
        }

        val reportMerge by registering(ReportMergeTask::class) {
            val file = rootProject.layout.buildDirectory.asFile.get().resolve("reports/detekt/exposed.xml")
            output.set(file)
            // output.set(rootProject.buildDir.resolve("reports/detekt/exposed.xml"))
        }
        withType<Detekt>().configureEach detekt@{
            enabled = this@subprojects.name !== "exposed-tests"
            finalizedBy(reportMerge)
            reportMerge.configure {
                input.from(this@detekt.xmlReportFile)
            }
        }

        clean {
            doLast {
                delete("./.project")
                delete("./out")
                delete("./bin")
            }
        }
    }

    dependencyManagement {
        // HINT: Gradle 빌드 시, detachedConfiguration 이 많이 발생하는데, setApplyMavenExclusions(false) 를 추가하면 속도가 개선됩니다.
        // https://discuss.gradle.org/t/what-is-detachedconfiguration-i-have-a-lots-of-them-for-each-subproject-and-resolving-them-takes-95-of-build-time/31595/6
        setApplyMavenExclusions(false)

        imports {
            mavenBom(Libs.spring_boot_dependencies)
            mavenBom(Libs.bluetape4k_bom)
            mavenBom(Libs.kotlinx_coroutines_bom)
            mavenBom(Libs.kotlin_bom)
        }
        dependencies {
            dependency(Libs.jetbrains_annotations)

            // Kotlinx Coroutines (mavenBom 이 적용이 안되어서 추가로 명시했습니다)
            dependency(Libs.kotlinx_coroutines_bom)
            dependency(Libs.kotlinx_coroutines_core)
            dependency(Libs.kotlinx_coroutines_core_common)
            dependency(Libs.kotlinx_coroutines_core_jvm)
            dependency(Libs.kotlinx_coroutines_reactive)
            dependency(Libs.kotlinx_coroutines_reactor)
            dependency(Libs.kotlinx_coroutines_rx2)
            dependency(Libs.kotlinx_coroutines_rx3)
            dependency(Libs.kotlinx_coroutines_slf4j)
            dependency(Libs.kotlinx_coroutines_debug)
            dependency(Libs.kotlinx_coroutines_test)
            dependency(Libs.kotlinx_coroutines_test_jvm)

            // Apache Commons
            dependency(Libs.commons_beanutils)
            dependency(Libs.commons_collections4)
            dependency(Libs.commons_compress)
            dependency(Libs.commons_codec)
            dependency(Libs.commons_csv)
            dependency(Libs.commons_lang3)
            dependency(Libs.commons_logging)
            dependency(Libs.commons_math3)
            dependency(Libs.commons_pool2)
            dependency(Libs.commons_text)
            dependency(Libs.commons_exec)
            dependency(Libs.commons_io)

            dependency(Libs.slf4j_api)
            dependency(Libs.jcl_over_slf4j)
            dependency(Libs.jul_to_slf4j)
            dependency(Libs.log4j_over_slf4j)
            dependency(Libs.logback)
            dependency(Libs.logback_core)

            // jakarta
            dependency(Libs.jakarta_activation_api)
            dependency(Libs.jakarta_annotation_api)
            dependency(Libs.jakarta_el_api)
            dependency(Libs.jakarta_inject_api)
            dependency(Libs.jakarta_interceptor_api)
            dependency(Libs.jakarta_jms_api)
            dependency(Libs.jakarta_json_api)
            dependency(Libs.jakarta_json)
            dependency(Libs.jakarta_persistence_api)
            dependency(Libs.jakarta_servlet_api)
            dependency(Libs.jakarta_transaction_api)
            dependency(Libs.jakarta_validation_api)
            dependency(Libs.jakarta_ws_rs_api)
            dependency(Libs.jakarta_xml_bind)

            // Compressor
            dependency(Libs.snappy_java)
            dependency(Libs.lz4_java)
            dependency(Libs.zstd_jni)

            // Java Money
            dependency(Libs.javax_money_api)
            dependency(Libs.javamoney_moneta)

            dependency(Libs.findbugs)
            dependency(Libs.guava)

            dependency(Libs.kryo)
            dependency(Libs.fory_kotlin)

            // NOTE: Jackson (이상하게 mavenBom 에 적용이 안되어서 강제로 추가하였다)
            dependency(Libs.jackson_bom)
            dependency(Libs.jackson_annotations)
            dependency(Libs.jackson_core)
            dependency(Libs.jackson_databind)
            dependency(Libs.jackson_datatype_jdk8)
            dependency(Libs.jackson_datatype_jsr310)
            dependency(Libs.jackson_datatype_jsr353)
            dependency(Libs.jackson_module_kotlin)
            dependency(Libs.jackson_module_paranamer)
            dependency(Libs.jackson_module_parameter_names)
            dependency(Libs.jackson_module_blackbird)
            dependency(Libs.jackson_module_jsonSchema)

            // Hibernate
            dependency(Libs.hibernate_core)
            dependency(Libs.hibernate_jcache)
            dependency(Libs.javassist)

            // Validators
            dependency(Libs.hibernate_validator)
            dependency(Libs.hibernate_validator_annotation_processor)

            dependency(Libs.junit_bom)
            dependency(Libs.junit_jupiter)
            dependency(Libs.junit_jupiter_api)
            dependency(Libs.junit_jupiter_engine)
            dependency(Libs.junit_jupiter_migrationsupport)
            dependency(Libs.junit_jupiter_params)
            dependency(Libs.junit_platform_commons)
            dependency(Libs.junit_platform_engine)
            dependency(Libs.junit_platform_launcher)
            dependency(Libs.junit_platform_runner)
        }
    }

    dependencies {
        val api by configurations
        val testApi by configurations
        val implementation by configurations
        val testImplementation by configurations

        val compileOnly by configurations
        val testCompileOnly by configurations
        val testRuntimeOnly by configurations

        implementation(Libs.kotlin_stdlib)
        implementation(Libs.kotlin_reflect)
        testImplementation(Libs.kotlin_test)
        testImplementation(Libs.kotlin_test_junit5)

        implementation(Libs.kotlinx_coroutines_core)
        implementation(Libs.kotlinx_atomicfu)

        // 개발 시에는 logback 이 검증하기에 더 좋고, Production에서 비동기 로깅은 log4j2 가 성능이 좋다고 합니다.
        implementation(Libs.slf4j_api)
        implementation(Libs.bluetape4k_logging)
        implementation(Libs.logback)

        // JUnit 5
        testImplementation(Libs.bluetape4k_junit5)
        testImplementation(Libs.junit_jupiter)
        testRuntimeOnly(Libs.junit_platform_engine)

        testImplementation(Libs.kluent)
        testImplementation(Libs.mockk)
        testImplementation(Libs.awaitility_kotlin)

        // Property baesd test
        testImplementation(Libs.datafaker)
        testImplementation(Libs.random_beans)
    }
}

dependencies {
    subprojects
        .filter { it.name != "04-benchmark" && it.name != "exposed-shared-tests" }
        .forEach { kover(it) }
}

kover {
    reports {
        total {
            xml {
                onCheck = false
            }
            html {
                onCheck = false
            }
        }
    }
}
