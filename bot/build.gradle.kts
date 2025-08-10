val ktorVersion = "2.3.8"
val telegramVersion = "8.3.0"
val sentryVersion = "8.18.0"

dependencies {
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(project(":clients"))
    implementation(project(":infra"))
    implementation(project(":metrics"))
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("com.github.pengrad:java-telegram-bot-api:$telegramVersion")
    implementation("io.sentry:sentry:$sentryVersion")
}
