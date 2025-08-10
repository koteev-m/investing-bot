import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    kotlin("jvm") version "1.9.22" apply false
    kotlin("plugin.serialization") version "1.9.22" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.8" apply false
    id("org.jlleitschuh.gradle.ktlint") version "13.0.0" apply false
}

allprojects {
    group = "pl.bot"
    version = "0.1.0"

    repositories { mavenCentral() }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    extensions.configure<KotlinJvmProjectExtension> { jvmToolchain(21) }
    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        ignoreFailures = true
    }
    extensions.configure<KtlintExtension> {
        ignoreFailures.set(true)
    }

    dependencies {
        add("implementation", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
        add("implementation", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
        add("testImplementation", "org.junit.jupiter:junit-jupiter:5.10.2")
        add("testImplementation", "io.mockk:mockk:1.14.5")
        add("testImplementation", "io.kotest:kotest-runner-junit5:5.8.0")
        add("testImplementation", "io.kotest:kotest-assertions-core:5.8.0")
    }

    tasks.withType<Test> { useJUnitPlatform() }

    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
            force("org.jetbrains.kotlin:kotlin-stdlib-common:1.9.22")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.22")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22")
            force("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            force("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.3")
            force("org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.7.3")
            force("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
        }
    }
}
