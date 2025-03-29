import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.22"
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
    maven("https://maven.lavalink.dev/releases")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(
        group = "net.dv8tion",
        name = "JDA",
        version = "4.4.1_353"
    )
    implementation(
        group = "dev.arbjerg",
        name = "lavaplayer",
        version = "2.2.3"
    )
    implementation(
        group = "dev.lavalink.youtube",
        name = "common",
        version = "1.12.0"
    )
    implementation(
        group = "io.github.oshai",
        name = "kotlin-logging-jvm",
        version = "5.1.0"
    )
    implementation(
        group = "org.slf4j",
        name = "slf4j-simple",
        version = "2.0.3"
    )
    testImplementation(
        group = "org.junit.jupiter",
        name = "junit-jupiter-api",
        version = "5.8.1"
    )
    testRuntimeOnly(
        group = "org.junit.jupiter",
        name = "junit-jupiter-engine",
        version = "5.8.1"
    )
    testImplementation(
        group = "io.mockk",
        name = "mockk",
        version = "1.13.7"
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