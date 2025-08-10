package pl.bot.clients.moex

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pl.bot.clients.cache.InMemoryCache

class MoexOptionCalcClientTest {
    private fun fixture(name: String): String =
        this::class.java.classLoader!!.getResource("fixtures/$name")!!.readText()

    @Test
    fun `fetches option calc data and caches`() = runBlocking {
        var seriesCalls = 0
        var chainCalls = 0
        var greeksCalls = 0
        var smileCalls = 0
        var pnlCalls = 0
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            val content = when {
                path.endsWith("/series.json") -> { seriesCalls++; fixture("calc_series.json") }
                path.endsWith("/chain.json") -> { chainCalls++; fixture("option_chain.json") }
                path.contains("/greeks/") -> { greeksCalls++; fixture("greeks.json") }
                path.endsWith("/iv-smile.json") -> { smileCalls++; fixture("iv_smile.json") }
                path.endsWith("/strategy-pnl.json") -> { pnlCalls++; fixture("strategy_pnl.json") }
                else -> error("unexpected path $path")
            }
            respond(content, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val cache = InMemoryCache()
        val client = MoexOptionCalcClient(cache, engine)

        val series1 = client.listOptionSeries("SBER")
        val series2 = client.listOptionSeries("SBER")
        assertEquals(series1, series2)
        assertEquals(listOf("SBER2024"), series1)
        assertEquals(1, seriesCalls)

        val chain1 = client.optionChain("SBER", "2024-12-20")
        val chain2 = client.optionChain("SBER", "2024-12-20")
        assertEquals(chain1.size, 2)
        assertEquals(chain1[0].strike, 100.0)
        assertEquals(1, chainCalls)

        val greeks1 = client.calcGreeks("SBER123")
        val greeks2 = client.calcGreeks("SBER123")
        assertEquals(greeks1, greeks2)
        assertEquals(0.1, greeks1!!.delta)
        assertEquals(1, greeksCalls)

        val smile1 = client.ivSmile("SBER", "2024-12-20")
        val smile2 = client.ivSmile("SBER", "2024-12-20")
        assertEquals(smile1.size, 2)
        assertEquals(1, smileCalls)

        val pnl1 = client.strategyPnl("spec")
        val pnl2 = client.strategyPnl("spec")
        assertEquals(pnl1.breakevenLow, 95.0)
        assertEquals(1, pnlCalls)
    }
}

