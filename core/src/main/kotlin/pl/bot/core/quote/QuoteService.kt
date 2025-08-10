package pl.bot.core.quote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pl.bot.clients.cache.Cache
import pl.bot.clients.moex.MoexIssClient

/** Quote data. */
data class Quote(
    val price: Double,
    val dayChangePercent: Double,
    val volume: Long,
)

interface QuoteService {
    suspend fun getQuote(secid: String): Quote?
}

class RealQuoteService(
    private val cache: Cache,
    private val moex: MoexIssClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : QuoteService {
    @Serializable
    private data class Tick(
        val price: Double,
        val dayChangePercent: Double,
        val volume: Long,
        val ts: Long,
    )

    override suspend fun getQuote(secid: String): Quote? {
        cache.get("tinkoff:quote:$secid")?.let {
            val tick = json.decodeFromString(Tick.serializer(), it)
            val now = System.currentTimeMillis() / 1000
            if (now - tick.ts <= 5) {
                return Quote(tick.price, tick.dayChangePercent, tick.volume)
            }
        }
        val candles = moex.getOHLCV(secid, "24")
        val last = candles.lastOrNull() ?: return null
        val pct = if (last.open != 0.0) ((last.close - last.open) / last.open) * 100.0 else 0.0
        return Quote(last.close, pct, last.volume)
    }
}
