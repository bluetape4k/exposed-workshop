import groovy.json.JsonSlurper
import java.time.Instant

plugins {
    kotlin("plugin.allopen")
    id(Plugins.kotlinx_benchmark)
}

dependencies {
    implementation(project(":03-routing-datasource"))

    implementation(Libs.caffeine)
    implementation(Libs.kotlinx_benchmark_runtime)
    implementation(Libs.kotlinx_benchmark_runtime_jvm)
    implementation(Libs.jmh_core)

    // Exposed
    implementation(platform(Libs.exposed_bom))
    implementation(Libs.exposed_core)
    implementation(Libs.exposed_jdbc)
    implementation(Libs.exposed_dao)
    implementation(Libs.exposed_java_time)

    // JPA / Hibernate
    implementation(Libs.jakarta_persistence_api)
    implementation(Libs.hibernate_core)

    // Database
    implementation(Libs.h2_v2)
    implementation(Libs.hikaricp)
    implementation(Libs.postgresql_driver)

    // Testcontainers
    implementation(Libs.bluetape4k_testcontainers)
    implementation(Libs.testcontainers_postgresql)
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
}

benchmark {
    targets {
        register("main")
    }

    configurations {
        named("main") {
            warmups = 5
            iterations = 10
            iterationTime = 1
            iterationTimeUnit = "s"
            mode = "avgt"
            outputTimeUnit = "us"
            reportFormat = "json"
        }

        register("smoke") {
            warmups = 2
            iterations = 3
            iterationTime = 300
            iterationTimeUnit = "ms"
            mode = "avgt"
            outputTimeUnit = "us"
            reportFormat = "json"
        }
    }
}

abstract class BenchmarkMarkdownReportTask: DefaultTask() {

    @get:Input
    abstract val profile: Property<String>

    @get:InputDirectory
    abstract val reportsRoot: DirectoryProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun writeMarkdown() {
        @Suppress("UNCHECKED_CAST")
        fun Any?.asStringAnyMap(): Map<String, Any?>? = this as? Map<String, Any?>

        val reportsDir = reportsRoot.get().asFile
        val jsonFile = reportsDir.walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .maxByOrNull { it.lastModified() }
            ?: error("Benchmark JSON report not found under ${reportsDir.absolutePath}. Run the benchmark task first.")

        val rows = (JsonSlurper().parse(jsonFile) as? List<*>)?.mapNotNull { element ->
            element.asStringAnyMap()
        }.orEmpty()

        val markdown = buildString {
            appendLine("# Benchmark Report")
            appendLine()
            appendLine("- Profile: `${profile.get()}`")
            appendLine("- Generated At: `${Instant.now()}`")
            appendLine("- Source JSON: `${jsonFile.absolutePath}`")
            appendLine()
            appendLine("| Benchmark | Mode | Params | Score | Error | Unit |")
            appendLine("| --- | --- | --- | ---: | ---: | --- |")

            rows.sortedBy { it["benchmark"].toString() }.forEach { row ->
                val params = row["params"].asStringAnyMap()
                val metric = row["primaryMetric"].asStringAnyMap()
                val benchmarkName = row["benchmark"].toString().substringAfterLast('.')
                val mode = row["mode"]?.toString().orEmpty()
                val paramsText = params?.entries
                    ?.sortedBy { it.key }
                    ?.joinToString("<br/>") { (key, value) -> "$key=$value" }
                    ?.ifBlank { "-" }
                    ?: "-"
                val score = (metric?.get("score") as? Number)?.let { "%.3f".format(it.toDouble()) } ?: "-"
                val error = (metric?.get("scoreError") as? Number)?.let { "%.3f".format(it.toDouble()) } ?: "-"
                val unit = metric?.get("scoreUnit")?.toString().orEmpty().ifBlank { "-" }

                appendLine("| $benchmarkName | $mode | $paramsText | $score | $error | $unit |")
            }
        }

        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(markdown)
        }

        logger.lifecycle("Wrote benchmark markdown report to ${outputFile.get().asFile}")
    }
}

val benchmarkProfile = providers.gradleProperty("benchmarkProfile").orElse("main")

tasks.register<BenchmarkMarkdownReportTask>("benchmarkMarkdown") {
    group = "benchmark"
    description = "Run benchmarks and write the latest JSON result as Markdown."
    profile.set(benchmarkProfile)
    reportsRoot.set(layout.buildDirectory.dir(benchmarkProfile.map { "reports/benchmarks/$it" }))
    outputFile.set(layout.buildDirectory.file(benchmarkProfile.map { "reports/benchmarks/$it/benchmark-report.md" }))

    dependsOn(benchmarkProfile.map { profileName ->
        when (profileName) {
            "main" -> "benchmark"
            "smoke" -> "smokeBenchmark"
            else -> error("Unsupported benchmarkProfile=$profileName. Use main or smoke.")
        }
    })
}
