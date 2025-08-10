plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "pl-bot"

include(
    "core",
    "data",
    "clients",
    "bot",
    "worker",
    "metrics",
    "infra"
)
