package pl.bot.clients.moex

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.long
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pl.bot.clients.cache.Cache

private const val BASE_URL = "https://iss.moex.com/iss"
private const val TTL_SECONDS = 5L

@Serializable
data class Candle(
    val open: Double,
    val close: Double,
    val high: Double,
    val low: Double,
    val volume: Long,
    val begin: String,
    val end: String,
)

@Serializable
data class Dividend(
    val secid: String,
    val registryCloseDate: String,
    val value: Double,
)

class MoexIssClient(
    private val cache: Cache,
    engine: HttpClientEngine = CIO.create(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val http = HttpClient(engine) {
        install(ContentNegotiation) { json(json) }
        install(HttpRequestRetry) {
            retryOnExceptionOrServerErrors(maxRetries = 2)
            exponentialDelay()
        }
    }

    suspend fun getLastPrice(secid: String): Double? {
        val key = "lastPrice:$secid"
        cache.get(key)?.let { return it.toDouble() }
        val url = "$BASE_URL/engines/stock/markets/shares/securities/$secid.json"
        val body = http.get(url) {
            url {
                parameters.append("iss.meta", "off")
                parameters.append("iss.only", "securities")
                parameters.append("securities.columns", "SECID,LAST")
            }
        }.bodyAsText()
        val price = parseTable(body, "securities") {
            it["LAST"]!!.jsonPrimitive.double
        }.firstOrNull()
        if (price != null) cache.set(key, price.toString(), TTL_SECONDS)
        return price
    }

    suspend fun getOHLCV(secid: String, interval: String): List<Candle> {
        val key = "ohlcv:$secid:$interval"
        cache.get(key)?.let { return json.decodeFromString(ListSerializer(Candle.serializer()), it) }
        val url = "$BASE_URL/engines/stock/markets/shares/securities/$secid/candles.json"
        val body = http.get(url) {
            url {
                parameters.append("interval", interval)
                parameters.append("iss.meta", "off")
            }
        }.bodyAsText()
        val candles = parseTable(body, "candles") {
            Candle(
                open = it["open"]!!.jsonPrimitive.double,
                close = it["close"]!!.jsonPrimitive.double,
                high = it["high"]!!.jsonPrimitive.double,
                low = it["low"]!!.jsonPrimitive.double,
                volume = it["volume"]!!.jsonPrimitive.long,
                begin = it["begin"]!!.jsonPrimitive.content,
                end = it["end"]!!.jsonPrimitive.content,
            )
        }
        cache.set(key, json.encodeToString(ListSerializer(Candle.serializer()), candles), TTL_SECONDS)
        return candles
    }

    suspend fun getDividendsWindow(days: Int = 30): List<Dividend> {
        val key = "dividends:$days"
        cache.get(key)?.let { return json.decodeFromString(ListSerializer(Dividend.serializer()), it) }
        val url = "$BASE_URL/securities/dividends.json"
        val body = http.get(url) {
            url {
                parameters.append("limit", "100")
                parameters.append("iss.meta", "off")
                parameters.append("days", days.toString())
            }
        }.bodyAsText()
        val dividends = parseTable(body, "dividends") {
            Dividend(
                secid = it["secid"]!!.jsonPrimitive.content,
                registryCloseDate = it["registryclosedate"]!!.jsonPrimitive.content,
                value = it["value"]!!.jsonPrimitive.double,
            )
        }
        cache.set(key, json.encodeToString(ListSerializer(Dividend.serializer()), dividends), TTL_SECONDS)
        return dividends
    }

    suspend fun listOptionSeries(underlying: String): List<String> {
        val key = "options:$underlying"
        cache.get(key)?.let { return json.decodeFromString(ListSerializer(String.serializer()), it) }
        val url = "$BASE_URL/engines/futures/markets/options/boards.json"
        val body = http.get(url) {
            url {
                parameters.append("underlying", underlying)
                parameters.append("iss.meta", "off")
            }
        }.bodyAsText()
        val series = parseTable(body, "boards") {
            it["SECID"]!!.jsonPrimitive.content
        }
        cache.set(key, json.encodeToString(ListSerializer(String.serializer()), series), TTL_SECONDS)
        return series
    }

    private fun <T> parseTable(body: String, section: String, mapper: (Map<String, JsonElement>) -> T): List<T> {
        val root = json.parseToJsonElement(body).jsonObject
        val table = root[section]!!.jsonObject
        val columns = table["columns"]!!.jsonArray.map { it.jsonPrimitive.content }
        val data = table["data"]!!.jsonArray
        return data.map { row ->
            val rowMap = columns.zip(row.jsonArray).associate { it.first to it.second }
            mapper(rowMap)
        }
    }
}
