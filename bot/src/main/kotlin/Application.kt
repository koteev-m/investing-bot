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
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.BotCommand
import com.pengrad.telegrambot.request.SetMyCommands
import pl.bot.infra.Config

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) { json() }
    install(CallLogging)
    install(StatusPages)
    install(MicrometerMetrics) { registry = prometheusRegistry }
    install(Koin) { modules(clientsModule, repositoriesModule, servicesModule) }
    val config = Config.fromEnv()
    val bot = TelegramBot(config.telegramToken)
    setupCommands(bot)
    routing {
        get("/healthz") { call.respondText("OK") }
        get("/readyz") { call.respondText("OK") }
        get("/metrics") { call.respondText(prometheusRegistry.scrape()) }
        telegramWebhook(bot)
    }
}

private fun setupCommands(bot: TelegramBot) {
    val commands = arrayOf(
        BotCommand("start", "Старт"),
        BotCommand("help", "Помощь"),
        BotCommand("upgrade", "Тарифы"),
        BotCommand("quote", "Котировка"),
        BotCommand("options", "Опционы"),
        BotCommand("dividends", "Дивиденды"),
        BotCommand("portfolio", "Портфель"),
        BotCommand("alert", "Алерт"),
        BotCommand("cquote", "Крипто котировка"),
        BotCommand("calert", "Крипто алерт"),
        BotCommand("cgainers", "Лидеры роста"),
        BotCommand("convert", "Конвертация"),
        BotCommand("cfear", "Индекс страха"),
    )
    bot.execute(SetMyCommands(*commands).languageCode("ru"))
}
