import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun generateVersion(): String {
    val regex = Regex("${archivesName.get()}-(\\d+)\\.(\\d+)\\.(\\d+).(\\d+)\\.jar")
    val libsDir = layout.buildDirectory.dir("libs").get()
    val versionBase = LocalDateTime.now().format(DateTimeFormatter.ofPattern("YYYY.MM.dd"))
    val jarPrefix = "${archivesName.get()}-$versionBase"

    println("Getting version name from dir ${libsDir.asFile.absolutePath}")
    println("Archive name ${archivesName.get()}")
    println("Prefix $jarPrefix")
    println("Regex pattern: ${regex.pattern}")

    val highestNumber = libsDir.asFileTree.files
        .filter { it.name.startsWith(jarPrefix) }
        .map { regex.find(it.name) }
        .map { it?.groups?.get(4)?.value?.toInt() ?: 0 }
        .takeIf { it.isNotEmpty() }
        ?.maxBy { it } ?: 0

    println("Highest version number is $highestNumber")

    return "${versionBase}.${highestNumber + 1}"
}

plugins {
    kotlin("jvm")
}

archivesName = "parsecs-core"
group = "com.github.callmephil"
version = generateVersion()

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.9")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
