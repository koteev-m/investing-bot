package pl.bot.clients.feargreed

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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import pl.bot.clients.cache.Cache

private const val BASE_URL = "https://api.alternative.me/fng/"
private const val TTL_SECONDS = 60L

class FearGreedClient(
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

    suspend fun current(): Int {
        val key = "fng:current"
        cache.get(key)?.let { return it.toInt() }
        val body = http.get(BASE_URL) {
            url { parameters.append("limit", "1") }
        }.bodyAsText()
        val value = json.parseToJsonElement(body)
            .jsonObject["data"]!!.jsonArray.first().jsonObject["value"]!!.jsonPrimitive.int
        cache.set(key, value.toString(), TTL_SECONDS)
        return value
        
    }
}

