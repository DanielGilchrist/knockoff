plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.pitest)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        allWarningsAsErrors = true
        jvmDefault = org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode.NO_COMPATIBILITY
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    systemProperty("junit.jupiter.execution.timeout.default", "10s")
}

pitest {
    pitestVersion = libs.versions.pitest
    junit5PluginVersion = libs.versions.pitestJunit5
    targetClasses = setOf("dev.danielgilchrist.knockoff.domain.*")
    excludedClasses = setOf("*\$\$serializer")
    mutators = setOf("STRONGER", "-VOID_METHOD_CALLS")
    threads = 4
    outputFormats = setOf("HTML", "XML")
    timestampedReports = false
}

tasks.named("pitest") {
    outputs.cacheIf("Reports restored from the build cache have been seen to come from another source tree") { false }
}

data class Mutant(
    val status: String,
    val sourceFile: String,
    val className: String,
    val method: String,
    val line: Int,
    val description: String,
)

val verifyMutants by tasks.registering {
    val report = layout.buildDirectory.file("reports/pitest/mutations.xml")
    val accepted = rootProject.layout.projectDirectory.file("config/pitest/accepted-survivors.txt")
    val classes = layout.buildDirectory.dir("classes/kotlin/main")
    val sources = layout.projectDirectory.dir("src/main/kotlin")
    dependsOn(tasks.named("pitest"))

    doLast {
        val inlinedCallSites = mutableMapOf<String, Map<Int, Int>>()

        fun callSiteLine(
            className: String,
            line: Int,
        ): Int {
            val callSites =
                inlinedCallSites.getOrPut(className) {
                    val classFile = classes.get().file(className.replace('.', '/') + ".class").asFile
                    val debug =
                        classFile
                            .readText(Charsets.ISO_8859_1)
                            .substringAfter("*S KotlinDebug", "")
                            .substringAfter("*L\n", "")
                            .substringBefore("*E")
                    Regex("""(\d+)#1:(\d+)(?:,(\d+))?""")
                        .findAll(debug)
                        .flatMap { match ->
                            val (callSite, start, count) = match.destructured
                            (0 until (count.toIntOrNull() ?: 1)).map { start.toInt() + it to callSite.toInt() }
                        }.toMap()
                }
            return callSites[line] ?: line
        }

        fun isDomainSource(mutant: Mutant): Boolean {
            val packagePath = mutant.className.substringBeforeLast('.').replace('.', '/')
            return sources.file("$packagePath/${mutant.sourceFile}").asFile.exists()
        }

        fun key(mutant: Mutant): String {
            val line = callSiteLine(mutant.className, mutant.line)
            return "${mutant.status} ${mutant.className} ${mutant.method} L$line ${mutant.description}"
        }

        val document =
            javax.xml.parsers.DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(report.get().asFile)
        val elements = document.getElementsByTagName("mutation")
        val mutants =
            (0 until elements.length).map { index ->
                val element = elements.item(index) as org.w3c.dom.Element

                fun text(tag: String): String = element.getElementsByTagName(tag).item(0).textContent

                Mutant(
                    status = element.getAttribute("status"),
                    sourceFile = text("sourceFile"),
                    className = text("mutatedClass"),
                    method = text("mutatedMethod"),
                    line = text("lineNumber").toInt(),
                    description = text("description"),
                )
            }
        val failedToRun = mutants.filter { it.status in setOf("RUN_ERROR", "MEMORY_ERROR") }
        val undetected = mutants.filter { it.status in setOf("SURVIVED", "NO_COVERAGE") }
        val outsideDomain = undetected.filterNot(::isDomainSource)
        val problems =
            failedToRun.map { "  PIT could not run: $it" } +
                outsideDomain.map { "  undetected outside domain sources, must be killed: $it" }

        if (problems.isNotEmpty()) {
            throw GradleException((listOf("Mutation testing failed") + problems).joinToString("\n"))
        }

        val survivors = undetected.map(::key).groupingBy { it }.eachCount()
        val expected =
            accepted.asFile
                .readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .groupingBy { it }
                .eachCount()
        val differences =
            (survivors.keys + expected.keys)
                .sorted()
                .filter { survivors[it] != expected[it] }
                .map { "  $it: ${survivors[it] ?: 0} surviving, ${expected[it] ?: 0} accepted" }

        if (differences.isNotEmpty()) {
            throw GradleException(
                (listOf("Mutation survivors differ from config/pitest/accepted-survivors.txt") + differences)
                    .joinToString("\n"),
            )
        }
    }
}

tasks.named("check") {
    dependsOn(verifyMutants)
}
