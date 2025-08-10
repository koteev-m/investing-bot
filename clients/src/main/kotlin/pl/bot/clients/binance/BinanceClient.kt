package pl.bot.clients.binance

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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.long
import pl.bot.clients.cache.Cache

private const val BASE_URL = "https://api.binance.com"

@Serializable
data class Ticker24h(
    val symbol: String,
    val lastPrice: Double,
    val priceChangePercent: Double,
    val highPrice: Double,
    val lowPrice: Double,
    val volume: Double,
)

@Serializable
data class Order(
    val price: Double,
    val quantity: Double,
)

@Serializable
data class OrderBook(
    val bids: List<Order>,
    val asks: List<Order>,
)

@Serializable
data class Kline(
    val openTime: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val closeTime: Long,
)

class BinanceClient(
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

    suspend fun ticker24h(symbol: String): Ticker24h {
        val key = "bn:ticker:$symbol"
        cache.get(key)?.let { return json.decodeFromString(Ticker24h.serializer(), it) }
        val body = http.get("${'$'}BASE_URL/api/v3/ticker/24hr") {
            url { parameters.append("symbol", symbol) }
        }.bodyAsText()
        val obj = json.parseToJsonElement(body).jsonObject
        val ticker = Ticker24h(
            symbol = obj["symbol"]!!.jsonPrimitive.content,
            lastPrice = obj["lastPrice"]!!.jsonPrimitive.double,
            priceChangePercent = obj["priceChangePercent"]!!.jsonPrimitive.double,
            highPrice = obj["highPrice"]!!.jsonPrimitive.double,
            lowPrice = obj["lowPrice"]!!.jsonPrimitive.double,
            volume = obj["volume"]!!.jsonPrimitive.double,
        )
        cache.set(key, json.encodeToString(Ticker24h.serializer(), ticker), 5)
        return ticker
    }

    suspend fun depth(symbol: String, limit: Int = 10): OrderBook {
        val key = "bn:depth:$symbol:$limit"
        cache.get(key)?.let { return json.decodeFromString(OrderBook.serializer(), it) }
        val body = http.get("${'$'}BASE_URL/api/v3/depth") {
            url {
                parameters.append("symbol", symbol)
                parameters.append("limit", limit.toString())
            }
        }.bodyAsText()
        val root = json.parseToJsonElement(body).jsonObject
        fun mapOrders(arr: JsonArray) = arr.map { item ->
            val line = item.jsonArray
            Order(
                price = line[0].jsonPrimitive.double,
                quantity = line[1].jsonPrimitive.double,
            )
        }
        val depth = OrderBook(
            bids = mapOrders(root["bids"]!!.jsonArray),
            asks = mapOrders(root["asks"]!!.jsonArray),
        )
        cache.set(key, json.encodeToString(OrderBook.serializer(), depth), 5)
        return depth
    }

    suspend fun klines(symbol: String, interval: String, limit: Int = 500): List<Kline> {
        val key = "bn:klines:$symbol:$interval:$limit"
        cache.get(key)?.let { return json.decodeFromString(ListSerializer(Kline.serializer()), it) }
        val body = http.get("${'$'}BASE_URL/api/v3/klines") {
            url {
                parameters.append("symbol", symbol)
                parameters.append("interval", interval)
                parameters.append("limit", limit.toString())
            }
        }.bodyAsText()
        val arr = json.parseToJsonElement(body).jsonArray
        val klines = arr.map {
            val k = it.jsonArray
            Kline(
                openTime = k[0].jsonPrimitive.long,
                open = k[1].jsonPrimitive.double,
                high = k[2].jsonPrimitive.double,
                low = k[3].jsonPrimitive.double,
                close = k[4].jsonPrimitive.double,
                volume = k[5].jsonPrimitive.double,
                closeTime = k[6].jsonPrimitive.long,
            )
        }
        cache.set(key, json.encodeToString(ListSerializer(Kline.serializer()), klines), 60)
        return klines
    }
}

