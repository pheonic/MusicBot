import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    kotlin("jvm") version "1.3.41"
    id("com.github.johnrengelman.shadow") version "5.2.0"
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
        group = "net.dv8tion",
        name = "JDA",
        version = "4.0.0_59"
    )
    compile(
        group = "com.sedmelluq",
        name = "lavaplayer",
        version = "1.3.32"
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
    compile(
        group = "com.github.jengelman.gradle.plugins",
        name = "shadow",
        version = "5.2.0"
    )
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<ShadowJar>() {
    manifest {
        attributes["Main-Class"] = "xyz.pheonic.musicbot.MainKt"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
}

//val fatJar = task("fatJar", type = Jar::class) {
//    baseName = "${project.name}-fat"
//    manifest {
//        attributes["Implementation-Title"] = "xyz.pheonic.MusicBot"
//        attributes["Implementation-Version"] = version
//        attributes["Main-Class"] = "xyz.pheonic.musicbot.MainKt"
//    }
//    from(configurations.runtime.resolve().map {
//        if (it.isDirectory) it else {
//
//            zipTree(it)
//
//        }
//    })
//    with(tasks["jar"] as CopySpec)
//}

tasks {
    "build" {
        dependsOn(shadowJar)
    }
}
