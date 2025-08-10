package pl.bot.clients.whalealert

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.long
import pl.bot.clients.cache.Cache

private const val BASE_URL = "https://api.whale-alert.io/v1"
private const val TTL_SECONDS = 60L
private const val MIN_INTERVAL_MS = 20_000L

@Serializable
data class Transfer(
    val blockchain: String,
    val symbol: String,
    val hash: String,
    val from: String?,
    val to: String?,
    val amount: Double,
    @kotlinx.serialization.SerialName("amount_usd") val amountUsd: Double,
    val timestamp: Long,
)

class WhaleAlertClient(
    private val apiKey: String,
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

    private val mutex = Mutex()
    private var lastRequest = 0L

    private suspend fun throttle() {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val wait = lastRequest + MIN_INTERVAL_MS - now
            if (wait > 0) delay(wait)
            lastRequest = System.currentTimeMillis()
        }
    }

    suspend fun getTransfers(minUsd: Int, assets: String? = null): List<Transfer> {
        val key = "whale:$minUsd:$assets"
        cache.get(key)?.let { return json.decodeFromString(ListSerializer(Transfer.serializer()), it) }
        throttle()
        val body = http.get("${'$'}BASE_URL/transactions") {
            url {
                parameters.append("api_key", apiKey)
                parameters.append("min_value", minUsd.toString())
                parameters.append("start", ((System.currentTimeMillis()/1000) - 3600).toString())
                if (assets != null) parameters.append("currency", assets)
            }
        }.bodyAsText()
        val root = json.parseToJsonElement(body).jsonObject
        val txs = root["transactions"]?.jsonArray?.map { item ->
            val obj = item.jsonObject
            Transfer(
                blockchain = obj["blockchain"]!!.jsonPrimitive.content,
                symbol = obj["symbol"]!!.jsonPrimitive.content,
                hash = obj["hash"]!!.jsonPrimitive.content,
                from = obj["from"]?.jsonObject?.get("address")?.jsonPrimitive?.content,
                to = obj["to"]?.jsonObject?.get("address")?.jsonPrimitive?.content,
                amount = obj["amount"]!!.jsonPrimitive.double,
                amountUsd = obj["amount_usd"]!!.jsonPrimitive.double,
                timestamp = obj["timestamp"]!!.jsonPrimitive.long,
            )
        } ?: emptyList()
        cache.set(key, json.encodeToString(ListSerializer(Transfer.serializer()), txs), TTL_SECONDS)
        return txs
    }
}

