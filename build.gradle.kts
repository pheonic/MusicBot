import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.3.31"
}

group = "xyz.pheonic"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile(
        group = "com.discord4j",
        name = "Discord4J",
        version = "2.10.1"
    )
    compile(
        group = "com.sedmelluq",
        name = "lavaplayer",
        version = "1.3.19"
    )
    compile(
        group = "ch.qos.logback",
        name = "logback-classic",
        version = "1.2.3"
    )
    compile(
        group = "io.github.microutils",
        name = "kotlin-logging",
        version = "1.5.4"
    )
    compile(
        group = "ch.qos.logback",
        name = "logback-classic",
        version = "1.2.3"
    )
    testImplementation(
        group = "org.junit.jupiter",
        name = "junit-jupiter",
        version = "5.4.2"
    )
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
}

val fatJar = task("fatJar", type = Jar::class) {
    baseName = "${project.name}-fat"
    manifest {
        attributes["Implementation-Title"] = "xyz.pheonic.MusicBot"
        attributes["Implementation-Version"] = version
        attributes["Main-Class"] = "xyz.pheonic.musicbot.MainKt"
    }
    from(configurations.runtime.map { if (it.isDirectory) it else zipTree(it) })
    with(tasks["jar"] as CopySpec)
}

tasks {
    "build" {
        dependsOn(fatJar)
    }
}
