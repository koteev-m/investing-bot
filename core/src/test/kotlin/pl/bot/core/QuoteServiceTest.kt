package pl.bot.core

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pl.bot.clients.cache.InMemoryCache
import pl.bot.clients.moex.Candle
import pl.bot.clients.moex.MoexIssClient
import pl.bot.core.quote.RealQuoteService

class QuoteServiceTest {
    @Test
    fun `uses tinkoff tick when fresh`() = runBlocking {
        val cache = InMemoryCache()
        val moex = mockk<MoexIssClient>(relaxed = true)
        val service = RealQuoteService(cache, moex)
        val ts = System.currentTimeMillis() / 1000
        val json = "{" +
            "\"price\":100.0,\"dayChangePercent\":1.0,\"volume\":10,\"ts\":$ts" +
            "}"
        cache.set("tinkoff:quote:SBER", json, 60)
        val quote = service.getQuote("SBER")!!
        assertEquals(100.0, quote.price)
        coVerify(exactly = 0) { moex.getOHLCV(any(), any()) }
    }

    @Test
    fun `falls back to moex when no tick`() = runBlocking {
        val cache = InMemoryCache()
        val moex = mockk<MoexIssClient>()
        val candles = listOf(
            Candle(open = 10.0, close = 11.0, high = 0.0, low = 0.0, volume = 5, begin = "", end = ""),
        )
        coEvery { moex.getOHLCV("SBER", "24") } returns candles
        val service = RealQuoteService(cache, moex)
        val quote = service.getQuote("SBER")!!
        assertEquals(11.0, quote.price)
        assertEquals(10.0, quote.dayChangePercent)
        assertEquals(5, quote.volume)
        coVerify(exactly = 1) { moex.getOHLCV("SBER", "24") }
    }
}
