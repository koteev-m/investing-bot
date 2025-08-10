package pl.bot.clients.moex

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pl.bot.clients.cache.Cache

private const val BASE_URL = "https://iss.moex.com/iss/options"
private const val TTL_SHORT = 10L
private const val TTL_LONG = 60L

@Serializable
data class OptionChainItem(
    val strike: Double,
    val call: String,
    val put: String,
)

@Serializable
data class Greeks(
    val delta: Double,
    val gamma: Double,
    val theta: Double,
    val vega: Double,
    val iv: Double,
)

@Serializable
data class IvPoint(
    val strike: Double,
    val iv: Double,
)

@Serializable
data class PnlPoint(
    val price: Double,
    val pnl: Double,
)

@Serializable
data class StrategyPnl(
    val breakevenLow: Double,
    val breakevenHigh: Double,
    val points: List<PnlPoint>,
)

class MoexOptionCalcClient(
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

    suspend fun listOptionSeries(underlying: String): List<String> {
        val key = "calc:series:$underlying"
        cache.get(key)?.let { return json.decodeFromString(ListSerializer(String.serializer()), it) }
        val url = "$BASE_URL/series.json"
        val body = http.get(url) {
            url {
                parameters.append("underlying", underlying)
                parameters.append("iss.meta", "off")
            }
        }.bodyAsText()
        val series = parseTable(body, "series") {
            it["SECID"]!!.jsonPrimitive.content
        }
        cache.set(key, json.encodeToString(ListSerializer(String.serializer()), series), TTL_LONG)
        return series
    }

    suspend fun optionChain(underlying: String, expiry: String): List<OptionChainItem> {
        val key = "calc:chain:$underlying:$expiry"
        cache.get(key)?.let { return json.decodeFromString(ListSerializer(OptionChainItem.serializer()), it) }
        val url = "$BASE_URL/chain.json"
        val body = http.get(url) {
            url {
                parameters.append("underlying", underlying)
                parameters.append("expiry", expiry)
                parameters.append("iss.meta", "off")
            }
        }.bodyAsText()
        val chain = parseTable(body, "chain") {
            OptionChainItem(
                strike = it["STRIKE"]!!.jsonPrimitive.double,
                call = it["CALL"]!!.jsonPrimitive.content,
                put = it["PUT"]!!.jsonPrimitive.content,
            )
        }
        cache.set(key, json.encodeToString(ListSerializer(OptionChainItem.serializer()), chain), TTL_LONG)
        return chain
    }

    suspend fun calcGreeks(optionCode: String): Greeks? {
        val key = "calc:greeks:$optionCode"
        cache.get(key)?.let { return json.decodeFromString(Greeks.serializer(), it) }
        val url = "$BASE_URL/greeks/$optionCode.json"
        val body = http.get(url) {
            url { parameters.append("iss.meta", "off") }
        }.bodyAsText()
        val greeks = parseTable(body, "greeks") {
            Greeks(
                delta = it["delta"]!!.jsonPrimitive.double,
                gamma = it["gamma"]!!.jsonPrimitive.double,
                theta = it["theta"]!!.jsonPrimitive.double,
                vega = it["vega"]!!.jsonPrimitive.double,
                iv = it["iv"]!!.jsonPrimitive.double,
            )
        }.firstOrNull()
        if (greeks != null) cache.set(key, json.encodeToString(Greeks.serializer(), greeks), TTL_SHORT)
        return greeks
    }

    suspend fun ivSmile(underlying: String, expiry: String): List<IvPoint> {
        val key = "calc:smile:$underlying:$expiry"
        cache.get(key)?.let { return json.decodeFromString(ListSerializer(IvPoint.serializer()), it) }
        val url = "$BASE_URL/iv-smile.json"
        val body = http.get(url) {
            url {
                parameters.append("underlying", underlying)
                parameters.append("expiry", expiry)
                parameters.append("iss.meta", "off")
            }
        }.bodyAsText()
        val points = parseTable(body, "smile") {
            IvPoint(
                strike = it["strike"]!!.jsonPrimitive.double,
                iv = it["iv"]!!.jsonPrimitive.double,
            )
        }
        cache.set(key, json.encodeToString(ListSerializer(IvPoint.serializer()), points), TTL_SHORT)
        return points
    }

    suspend fun strategyPnl(spec: String): StrategyPnl {
        val key = "calc:pnl:$spec"
        cache.get(key)?.let { return json.decodeFromString(StrategyPnl.serializer(), it) }
        val url = "$BASE_URL/strategy-pnl.json"
        val body = http.get(url) {
            url {
                parameters.append("spec", spec)
                parameters.append("iss.meta", "off")
            }
        }.bodyAsText()
        val points = parseTable(body, "pnl") {
            PnlPoint(
                price = it["price"]!!.jsonPrimitive.double,
                pnl = it["pnl"]!!.jsonPrimitive.double,
            )
        }
        val be = parseTable(body, "breakeven") {
            it["low"]!!.jsonPrimitive.double to it["high"]!!.jsonPrimitive.double
        }.first()
        val result = StrategyPnl(be.first, be.second, points)
        cache.set(key, json.encodeToString(StrategyPnl.serializer(), result), TTL_SHORT)
        return result
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

