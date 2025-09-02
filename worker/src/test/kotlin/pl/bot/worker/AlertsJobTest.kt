package pl.bot.worker

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.BaseRequest
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.BaseResponse
import com.pengrad.telegrambot.utility.BotUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.bot.core.alert.Alert
import pl.bot.core.alert.Direction
import pl.bot.core.quote.Quote
import pl.bot.core.quote.QuoteService
import pl.bot.core.crypto.CryptoQuote
import pl.bot.core.crypto.CryptoQuoteService
import pl.bot.data.alert.InMemoryAlertRepository

private class FakeTelegramBot : TelegramBot("TEST") {
    val requests = mutableListOf<SendMessage>()
    override fun <T : BaseRequest<T, R>, R : BaseResponse> execute(request: BaseRequest<T, R>): R {
        if (request is SendMessage) requests += request
        val json = "{" + "\"ok\":true" + "}"
        return BotUtils.fromJson(json, request.responseType)
    }
}

private class TestQuoteService(var price: Double?) : QuoteService {
    override suspend fun getQuote(secid: String) = price?.let { Quote(it, 0.0, 0) }
}

private class TestCryptoQuoteService(var price: Double?) : CryptoQuoteService {
    override suspend fun getQuote(symbol: String) = price?.let { CryptoQuote(it, 0.0) }
}

class AlertsJobTest {
    @Test
    fun `fires once and removes for non-permanent`() {
        val repo = InMemoryAlertRepository()
        repo.save(Alert(1, 1, "SBER", false, Direction.ABOVE, 100.0, permanent = false))
        val qs = TestQuoteService(101.0)
        val bot = FakeTelegramBot()
        val job = AlertsJob(repo, qs, TestCryptoQuoteService(null), bot)
        job.execute(null)
        assertEquals(1, bot.requests.size)
        assertTrue(repo.all().isEmpty())
        qs.price = 102.0
        job.execute(null)
        assertEquals(1, bot.requests.size)
    }

    @Test
    fun `hysteresis prevents duplicate`() {
        val repo = InMemoryAlertRepository()
        repo.save(Alert(1, 1, "SBER", false, Direction.ABOVE, 100.0, hysteresisBps = 20, permanent = true))
        val qs = TestQuoteService(101.0)
        val bot = FakeTelegramBot()
        val job = AlertsJob(repo, qs, TestCryptoQuoteService(null), bot)
        job.execute(null)
        assertEquals(1, bot.requests.size)
        qs.price = 99.9 // drop but not enough to reset (needs < 99.8)
        job.execute(null)
        qs.price = 101.0
        job.execute(null)
        assertEquals(1, bot.requests.size)
    }

    @Test
    fun `permanent alert retriggers after hysteresis`() {
        val repo = InMemoryAlertRepository()
        repo.save(Alert(1, 1, "SBER", false, Direction.ABOVE, 100.0, hysteresisBps = 20, permanent = true))
        val qs = TestQuoteService(101.0)
        val bot = FakeTelegramBot()
        val job = AlertsJob(repo, qs, TestCryptoQuoteService(null), bot)
        job.execute(null)
        assertEquals(1, bot.requests.size)
        // move below hysteresis to reset
        qs.price = 99.0
        job.execute(null)
        // back above
        qs.price = 101.0
        job.execute(null)
        assertEquals(2, bot.requests.size)
    }
}

