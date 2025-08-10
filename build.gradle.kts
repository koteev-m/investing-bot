import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    kotlin("jvm") version "1.9.22" apply false
    kotlin("plugin.serialization") version "1.9.22" apply false
}

allprojects {
    group = "pl.bot"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

    extensions.configure<KotlinJvmProjectExtension> {
        jvmToolchain(21)
    }

    dependencies {
        add("implementation", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
        add("implementation", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
        add("testImplementation", "org.jetbrains.kotlin:kotlin-test")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
