import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    id("io.qameta.allure") version "2.12.0"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
}

group = "com.automation"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val allureVersion = "2.24.0"
val junitVersion = "5.10.1"
val playwrightVersion = "1.40.0"
val springVersion = "6.1.2"
val kotlinxSerializationVersion = "1.6.2"
val kotlinxCoroutinesVersion = "1.7.3"

dependencies {
    // Kotlin
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$kotlinxCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")

    // Spring WebClient
    implementation("org.springframework:spring-webflux:$springVersion")
    implementation("org.springframework:spring-context:$springVersion")
    implementation("io.projectreactor.netty:reactor-netty-http:1.1.14")

    // Playwright
    implementation("com.microsoft.playwright:playwright:$playwrightVersion")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    // JUnit 5
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")

    // Allure
    implementation("io.qameta.allure:allure-java-commons:$allureVersion")
    testImplementation("io.qameta.allure:allure-junit5:$allureVersion")

    // Assertions
    testImplementation("org.assertj:assertj-core:3.24.2")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
    systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

allure {
    adapter {
        autoconfigure.set(true)
        aspectjWeaver.set(false) // Отключено для совместимости с Java 24
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
