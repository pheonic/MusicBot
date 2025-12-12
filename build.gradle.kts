import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    kotlin("jvm") version "2.2.20"
    id("com.gradleup.shadow") version "9.3.0"
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
    maven("https://jitpack.io")
    maven("https://maven.lavalink.dev/releases")
}

dependencies {
    implementation("net.dv8tion:JDA:6.1.3")
    implementation("dev.arbjerg:lavaplayer:2.2.4")
    implementation("dev.lavalink.youtube:common:1.16.0")
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("org.slf4j:slf4j-simple:2.0.3")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation("io.mockk:mockk:1.13.7")
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_17
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