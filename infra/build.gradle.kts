dependencies {
    implementation(project(":core"))
    implementation(project(":clients"))
    implementation(project(":data"))
    api("io.insert-koin:koin-core:3.5.3")
    api("io.insert-koin:koin-ktor:3.5.3")
    implementation("org.slf4j:slf4j-api:2.0.12")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("io.lettuce:lettuce-core:6.8.0.RELEASE")
    implementation("io.sentry:sentry:8.18.0")
}
