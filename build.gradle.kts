import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") version "3.2.3"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
}

group = "me.kht"
version = "0.0.3"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    implementation("com.mikuac:shiro:2.1.8")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("org.jsoup:jsoup:1.17.2")

    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")

    // https://mvnrepository.com/artifact/com.microsoft.playwright/playwright
    implementation("com.microsoft.playwright:playwright:1.41.2")

    // https://mvnrepository.com/artifact/com.squareup.moshi/moshi-kotlin
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.withType<BootJar> {
    archiveFileName.set("AyanamiBot.jar")
}

tasks.create("buildDockerImage") {
    dependsOn("build")
    group = "build"

    doLast {
        val result = exec {
            commandLine = listOf("docker", "build", "-t","kht/ayanamibot:$version",".")
        }
        if (result.exitValue != 0) {
            throw RuntimeException("Failed to build docker image")
        }
    }
}