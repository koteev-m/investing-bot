val ktorVersion = "2.3.8"

dependencies {
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(project(":clients"))
    implementation(project(":infra"))
    implementation(project(":metrics"))
    implementation("org.quartz-scheduler:quartz:2.3.2")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("com.github.pengrad:java-telegram-bot-api:8.3.0")
}
