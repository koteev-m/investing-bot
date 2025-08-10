package pl.bot.bot

import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.utility.BotUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import pl.bot.core.crypto.CryptoQuote
import pl.bot.core.crypto.CryptoQuoteService
import pl.bot.core.quote.Quote
import pl.bot.core.quote.QuoteService

class QuoteHandlersTest {
    private lateinit var bot: FakeTelegramBot

    @BeforeEach
    fun setup() { bot = FakeTelegramBot() }

    @AfterEach
    fun teardown() { stopKoin() }

    @Test
    fun `quote formats html with emoji`() {
        val updateJson = """
            {"update_id":1,
             "message":{"message_id":1,
                        "from":{"id":1,"is_bot":false,"first_name":"A"},
                        "chat":{"id":1,"type":"private"},
                        "date":0,
                        "text":"/quote SBER"}}
        """.trimIndent()
        val update = BotUtils.fromJson(updateJson, com.pengrad.telegrambot.model.Update::class.java)
        val service = object : QuoteService {
            override suspend fun getQuote(secid: String) = Quote(100.0, -1.0, 50)
        }
        startKoin { modules(module { single<QuoteService> { service } }) }
        quote(bot, update)
        val req = bot.lastRequest as SendMessage
        val text = req.parameters["text"] as String
        assertTrue(text.contains("📉"))
        assertTrue(req.parameters["parse_mode"] == "HTML")
    }

    @Test
    fun `cquote formats html with emoji`() {
        val updateJson = """
            {"update_id":1,
             "message":{"message_id":1,
                        "from":{"id":1,"is_bot":false,"first_name":"A"},
                        "chat":{"id":1,"type":"private"},
                        "date":0,
                        "text":"/cquote BTC"}}
        """.trimIndent()
        val update = BotUtils.fromJson(updateJson, com.pengrad.telegrambot.model.Update::class.java)
        val service = object : CryptoQuoteService {
            override suspend fun getQuote(symbol: String) = CryptoQuote(20000.0, 2.0)
        }
        startKoin { modules(module { single<CryptoQuoteService> { service } }) }
        cquote(bot, update)
        val req = bot.lastRequest as SendMessage
        val text = req.parameters["text"] as String
        assertTrue(text.contains("📈"))
        assertTrue(req.parameters["parse_mode"] == "HTML")
    }
}
