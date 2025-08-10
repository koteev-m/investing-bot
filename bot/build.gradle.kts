dependencies {
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(project(":clients"))
    implementation(project(":infra"))
    implementation(project(":metrics"))
    implementation("io.ktor:ktor-server-core:2.3.4")
    implementation("io.ktor:ktor-server-netty:2.3.4")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.4")
    implementation("com.github.pengrad:java-telegram-bot-api:6.9.1")
    implementation("io.sentry:sentry:7.16.0")
}
