import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.30"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "xyz.pheonic"
version = "1.0-SNAPSHOT"

sourceSets {
    named("main") {
        java.srcDir("src/main/kotlin")
    }

    named("test") {
        java.srcDir("src/test/kotlin")
    }
}

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(
        group = "net.dv8tion",
        name = "JDA",
        version = "4.4.1_353"
    )
    implementation(
        group = "com.github.walkyst",
        name = "lavaplayer-fork",
        version = "1.4.0"
    )
    implementation(
        group = "ch.qos.logback",
        name = "logback-classic",
        version = "1.2.3"
    )
    implementation(
        group = "io.github.microutils",
        name = "kotlin-logging",
        version = "1.5.4"
    )
    testImplementation(
        group = "org.junit.jupiter",
        name = "junit-jupiter",
        version = "5.4.2"
    )
    testImplementation(
        group = "io.mockk",
        name = "mockk",
        version = "1.10.4"
    )
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_11
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.withType<Jar> {
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