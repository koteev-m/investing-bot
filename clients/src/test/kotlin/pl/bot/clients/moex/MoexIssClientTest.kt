package pl.bot.clients.moex

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import pl.bot.clients.cache.InMemoryCache

class MoexIssClientTest {
    private fun fixture(name: String): String =
        this::class.java.classLoader!!.getResource("fixtures/$name")!!.readText()

    @Test
    fun `fetches data and caches responses`() = runBlocking {
        var priceCalls = 0
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            val content = when {
                path.endsWith("/securities/SBER.json") -> {
                    priceCalls++
                    fixture("last_price.json")
                }
                path.endsWith("/candles.json") -> fixture("candles.json")
                path.contains("dividends.json") -> fixture("dividends.json")
                path.contains("boards.json") -> fixture("options.json")
                else -> error("unexpected path $path")
            }
            respond(content, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val cache = InMemoryCache()
        val client = MoexIssClient(cache, engine)

        val price1 = client.getLastPrice("SBER")
        val price2 = client.getLastPrice("SBER")
        assertEquals(price1, price2)
        assertEquals(280.5, price1)
        assertEquals(1, priceCalls)

        val candles = client.getOHLCV("SBER", "60")
        assertEquals(2, candles.size)
        assertEquals(1000, candles.first().volume)

        val dividends = client.getDividendsWindow()
        assertEquals(1, dividends.size)
        assertEquals("SBER", dividends.first().secid)

        val options = client.listOptionSeries("SBER")
        assertEquals(listOf("SBER2024", "SBER2025"), options)
    }
}
