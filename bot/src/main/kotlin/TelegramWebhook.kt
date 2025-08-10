package pl.bot.bot

import com.pengrad.telegrambot.utility.BotUtils
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.SendMessage
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.reflect.KFunction2
import kotlin.reflect.full.findAnnotation

fun Route.telegramWebhook(bot: TelegramBot, rateLimiter: RateLimiter = RateLimiter()) {
    post("/tg/webhook") {
        val json = call.receiveText()
        val update = BotUtils.fromJson(json, Update::class.java)
        processUpdate(bot, rateLimiter, update)
        call.respond(HttpStatusCode.OK)
    }
}

fun processUpdate(bot: TelegramBot, rateLimiter: RateLimiter, update: Update) {
    val message = update.message() ?: return
    val chatId = message.chat().id()
    val userId = message.from().id()
    val command = message.text()?.split(" ")?.firstOrNull() ?: ""

    fun exec(handler: KFunction2<TelegramBot, Update, Unit>) {
        handler.findAnnotation<Quota>()?.let { quota ->
            if (!rateLimiter.checkAndIncrement(userId, quota.name, quota.dailyLimit)) {
                bot.execute(SendMessage(chatId, "Квота исчерпана"))
                return
            }
        }
        handler.findAnnotation<RequiresPlan>()?.let { req ->
            val userPlan = userPlan(userId)
            if (userPlan.ordinal < req.plan.ordinal) {
                bot.execute(SendMessage(chatId, "Требуется план ${req.plan}"))
                return
            }
        }
        handler.call(bot, update)
    }

    when (command) {
        "/start" -> exec(::start)
        "/help" -> exec(::help)
        "/upgrade" -> exec(::upgrade)
        "/quote" -> exec(::quote)
        "/options" -> exec(::options)
        "/dividends" -> exec(::dividends)
        "/portfolio" -> exec(::portfolio)
        "/alert" -> exec(::alert)
        "/cquote" -> exec(::cquote)
        "/calert" -> exec(::calert)
        "/cgainers" -> exec(::cgainers)
        "/convert" -> exec(::convert)
        "/cfear" -> exec(::cfear)
        else -> bot.execute(SendMessage(chatId, "Неизвестная команда"))
    }
}

private fun userPlan(@Suppress("UNUSED_PARAMETER") userId: Long): Plan = Plan.FREE

@RequiresPlan(Plan.FREE)
fun start(bot: TelegramBot, update: Update) {
    bot.execute(SendMessage(update.message().chat().id(), "Добро пожаловать!"))
}

@RequiresPlan(Plan.FREE)
fun help(bot: TelegramBot, update: Update) {
    bot.execute(SendMessage(update.message().chat().id(), "Помощь"))
}

@RequiresPlan(Plan.FREE)
fun upgrade(bot: TelegramBot, update: Update) {
    bot.execute(SendMessage(update.message().chat().id(), "Доступные тарифы: PRO, PREMIUM"))
}

@RequiresPlan(Plan.FREE)
@Quota("quote", 10)
fun quote(bot: TelegramBot, update: Update) {
    bot.execute(SendMessage(update.message().chat().id(), "Quote stub"))
}

@RequiresPlan(Plan.PRO)
fun options(bot: TelegramBot, update: Update) {
    bot.execute(SendMessage(update.message().chat().id(), "Options stub"))
}

@RequiresPlan(Plan.PRO)
fun dividends(bot: TelegramBot, update: Update) {
    bot.execute(SendMessage(update.message().chat().id(), "Dividends stub"))
}

@RequiresPlan(Plan.FREE)
fun portfolio(bot: TelegramBot, update: Update) {
    val chatId = update.message().chat().id()
    val parts = update.message().text().split(" ")
    val reply = when (parts.getOrNull(1)) {
        "add" -> "Добавление в портфель (stub)"
        "show" -> "Портфель (stub)"
        else -> "Используйте /portfolio add|show"
    }
    bot.execute(SendMessage(chatId, reply))
}

@RequiresPlan(Plan.PRO)
fun alert(bot: TelegramBot, update: Update) {
    bot.execute(SendMessage(update.message().chat().id(), "Alert stub"))
}

@RequiresPlan(Plan.PRO)
fun cquote(bot: TelegramBot, update: Update) {
    bot.execute(SendMessage(update.message().chat().id(), "Crypto quote stub"))
}

@RequiresPlan(Plan.PRO)
fun calert(bot: TelegramBot, update: Update) {
    bot.execute(SendMessage(update.message().chat().id(), "Crypto alert stub"))
}

@RequiresPlan(Plan.PRO)
fun cgainers(bot: TelegramBot, update: Update) {
    bot.execute(SendMessage(update.message().chat().id(), "Crypto gainers stub"))
}

@RequiresPlan(Plan.PREMIUM)
fun convert(bot: TelegramBot, update: Update) {
    bot.execute(SendMessage(update.message().chat().id(), "Convert stub"))
}

@RequiresPlan(Plan.PRO)
fun cfear(bot: TelegramBot, update: Update) {
    bot.execute(SendMessage(update.message().chat().id(), "Crypto fear index stub"))
}
