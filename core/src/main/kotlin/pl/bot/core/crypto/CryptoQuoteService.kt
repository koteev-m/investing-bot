package pl.bot.core.crypto

import pl.bot.clients.coingecko.CoinGeckoClient

/** Crypto quote data. */
data class CryptoQuote(
    val price: Double,
    val change24h: Double,
)

interface CryptoQuoteService {
    suspend fun getQuote(symbol: String): CryptoQuote?
}

class RealCryptoQuoteService(
    private val cg: CoinGeckoClient,
) : CryptoQuoteService {
    override suspend fun getQuote(symbol: String): CryptoQuote? {
        val id = when (symbol.uppercase()) {
            "BTC" -> "bitcoin"
            else -> symbol.lowercase()
        }
        val map = cg.simplePrice(listOf(id), listOf("usd"), include24hChange = true)[id] ?: return null
        val price = map["usd"] ?: return null
        val change = map["usd_24h_change"] ?: 0.0
        return CryptoQuote(price, change)
    }
}
