package pl.bot.bot

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.metrics.micrometer.*
import org.koin.ktor.plugin.Koin
import pl.bot.infra.clientsModule
import pl.bot.infra.repositoriesModule
import pl.bot.infra.servicesModule
import pl.bot.metrics.prometheusRegistry

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) { json() }
    install(CallLogging)
    install(StatusPages)
    install(MicrometerMetrics) { registry = prometheusRegistry }
    install(Koin) { modules(clientsModule, repositoriesModule, servicesModule) }
    routing {
        get("/healthz") { call.respondText("OK") }
        get("/readyz") { call.respondText("OK") }
        get("/metrics") { call.respondText(prometheusRegistry.scrape()) }
    }
}
