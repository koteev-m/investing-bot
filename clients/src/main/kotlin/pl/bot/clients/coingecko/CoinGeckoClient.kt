package pl.bot.clients.coingecko

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import pl.bot.clients.cache.Cache

private const val BASE_URL = "https://api.coingecko.com/api/v3"
private const val TTL_SECONDS = 60L

@Serializable
data class MarketCoin(
    val id: String,
    val symbol: String,
    val name: String,
    @kotlinx.serialization.SerialName("current_price") val currentPrice: Double,
    @kotlinx.serialization.SerialName("price_change_percentage_24h") val priceChangePercentage24h: Double,
)

class CoinGeckoClient(
    private val cache: Cache,
    engine: HttpClientEngine = CIO.create(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val http = HttpClient(engine) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 5_000
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 5_000
        }
        install(HttpRequestRetry) {
            retryOnExceptionOrServerErrors(maxRetries = 2)
            exponentialDelay()
        }
    }

    suspend fun simplePrice(ids: List<String>, vs: List<String> = listOf("usd", "rub")): Map<String, Map<String, Double>> {
        val key = "cg:simple:${ids.joinToString(",")}:${vs.joinToString(",")}" 
        cache.get(key)?.let {
            return json.decodeFromString(MapSerializer(String.serializer(), MapSerializer(String.serializer(), Double.serializer())), it)
        }
        val body = http.get("${'$'}BASE_URL/simple/price") {
            url {
                parameters.append("ids", ids.joinToString(","))
                parameters.append("vs_currencies", vs.joinToString(","))
            }
        }.bodyAsText()
        val result = json.decodeFromString(MapSerializer(String.serializer(), MapSerializer(String.serializer(), Double.serializer())), body)
        cache.set(key, body, TTL_SECONDS)
        return result
    }

    suspend fun marketsTopGainers(limit: Int = 10): List<MarketCoin> {
        val key = "cg:top:$limit"
        cache.get(key)?.let {
            return json.decodeFromString(ListSerializer(MarketCoin.serializer()), it)
        }
        val body = http.get("${'$'}BASE_URL/coins/markets") {
            url {
                parameters.append("vs_currency", "usd")
                parameters.append("order", "market_cap_desc")
                parameters.append("per_page", "250")
                parameters.append("page", "1")
                parameters.append("price_change_percentage", "24h")
            }
        }.bodyAsText()
        val markets = json.decodeFromString(ListSerializer(MarketCoin.serializer()), body)
            .sortedByDescending { it.priceChangePercentage24h }
            .take(limit)
        cache.set(key, json.encodeToString(ListSerializer(MarketCoin.serializer()), markets), TTL_SECONDS)
        return markets
    }
}

