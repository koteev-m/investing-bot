package pl.bot.bot

import com.pengrad.telegrambot.utility.BotUtils
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup
import com.pengrad.telegrambot.model.request.KeyboardButton
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.reflect.KFunction2
import kotlin.reflect.full.findAnnotation
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext
import pl.bot.core.quote.QuoteService
import pl.bot.core.crypto.CryptoQuoteService
import pl.bot.core.alert.Alert
import pl.bot.core.alert.AlertRepository
import pl.bot.core.alert.Direction

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
                val msg = SendMessage(chatId, "Квота исчерпана")
                    .replyMarkup(
                        ReplyKeyboardMarkup(KeyboardButton("/upgrade"))
                            .oneTimeKeyboard(true)
                            .resizeKeyboard(true),
                    )
                bot.execute(msg)
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
@Quota("quote", 5)
fun quote(bot: TelegramBot, update: Update) {
    val service = GlobalContext.get().get<QuoteService>()
    val chatId = update.message().chat().id()
    val parts = update.message().text().split(" ")
    val secid = parts.getOrNull(1)?.uppercase()
    if (secid == null) {
        bot.execute(SendMessage(chatId, "Используйте /quote TICKER"))
        return
    }
    val q = runBlocking { service.getQuote(secid) }
    if (q == null) {
        bot.execute(SendMessage(chatId, "Не найдена котировка"))
        return
    }
    val emoji = if (q.dayChangePercent >= 0) "📈" else "📉"
    val sign = if (q.dayChangePercent >= 0) "+" else ""
    val text = "<b>${secid}</b> %.2f ₽ (%s%.2f%%) %s\nОбъём: %d".format(
        q.price,
        sign,
        q.dayChangePercent,
        emoji,
        q.volume,
    )
    bot.execute(
        SendMessage(chatId, text).parseMode(ParseMode.HTML),
    )
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

@RequiresPlan(Plan.FREE)
fun alert(bot: TelegramBot, update: Update) {
    val chatId = update.message().chat().id()
    val userId = update.message().from().id()
    val parts = update.message().text().split(" ")
    val symbol = parts.getOrNull(1)?.uppercase()
    val op = parts.getOrNull(2)
    val price = parts.getOrNull(3)?.toDoubleOrNull()
    if (symbol == null || price == null || op !in setOf(">", "<")) {
        bot.execute(SendMessage(chatId, "Используйте /alert TICKER > PRICE"))
        return
    }
    val dir = if (op == ">") Direction.ABOVE else Direction.BELOW
    val repo = GlobalContext.get().get<AlertRepository>()
    val permanent = userPlan(userId).ordinal >= Plan.PRO.ordinal
    repo.save(Alert(userId, chatId, symbol, false, dir, price, permanent = permanent))
    bot.execute(SendMessage(chatId, "Алерт сохранён"))
}

@RequiresPlan(Plan.FREE)
@Quota("cquote", 5)
fun cquote(bot: TelegramBot, update: Update) {
    val service = GlobalContext.get().get<CryptoQuoteService>()
    val chatId = update.message().chat().id()
    val parts = update.message().text().split(" ")
    val symbol = parts.getOrNull(1)?.uppercase()
    if (symbol == null) {
        bot.execute(SendMessage(chatId, "Используйте /cquote SYMBOL"))
        return
    }
    val q = runBlocking { service.getQuote(symbol) }
    if (q == null) {
        bot.execute(SendMessage(chatId, "Не найдена котировка"))
        return
    }
    val emoji = if (q.change24h >= 0) "📈" else "📉"
    val sign = if (q.change24h >= 0) "+" else ""
    val text = "<b>${symbol}</b> %.2f USD (%s%.2f%%) %s".format(
        q.price,
        sign,
        q.change24h,
        emoji,
    )
    bot.execute(
        SendMessage(chatId, text).parseMode(ParseMode.HTML),
    )
}

@RequiresPlan(Plan.FREE)
fun calert(bot: TelegramBot, update: Update) {
    val chatId = update.message().chat().id()
    val userId = update.message().from().id()
    val parts = update.message().text().split(" ")
    val symbol = parts.getOrNull(1)?.uppercase()
    val op = parts.getOrNull(2)
    val price = parts.getOrNull(3)?.toDoubleOrNull()
    if (symbol == null || price == null || op !in setOf(">", "<")) {
        bot.execute(SendMessage(chatId, "Используйте /calert SYMBOL > PRICE"))
        return
    }
    val dir = if (op == ">") Direction.ABOVE else Direction.BELOW
    val repo = GlobalContext.get().get<AlertRepository>()
    val permanent = userPlan(userId).ordinal >= Plan.PRO.ordinal
    repo.save(Alert(userId, chatId, symbol, true, dir, price, permanent = permanent))
    bot.execute(SendMessage(chatId, "Алерт сохранён"))
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
