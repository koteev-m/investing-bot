package pl.bot.clients.tinkoff

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import pl.bot.clients.redis.Publisher

private const val WS_URL = "wss://invest-public-api.tinkoff.ru/ws/"

/** Connection abstraction for easier testing. */
interface WsSession {
    suspend fun send(text: String)
    suspend fun receive(): String?
    suspend fun close()
}

/** Factory creating [WsSession] instances. */
interface WsConnector {
    suspend fun connect(token: String): WsSession
}

/** Real connector based on Ktor [HttpClient]. */
class KtorWsConnector(
    private val engine: HttpClientEngine = CIO.create()
) : WsConnector {
    private val client = HttpClient(engine) { install(WebSockets) }

    override suspend fun connect(token: String): WsSession {
        val session = client.webSocketSession(urlString = WS_URL) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        return object : WsSession {
            override suspend fun send(text: String) {
                session.send(Frame.Text(text))
            }

            override suspend fun receive(): String? {
                val frame = session.incoming.receiveCatching().getOrNull() ?: return null
                return (frame as? Frame.Text)?.readText()
            }

            override suspend fun close() {
                session.close()
            }
        }
    }
}

@Serializable
private data class SubscribeRequest(
    val event: String = "subscribe",
    val quotes: List<String>,
    val orderbooks: List<String>
)

@Serializable
private data class LastPriceEvent(val type: String = "last_price", val figi: String, val price: Double)

@Serializable
private data class OrderBookEvent(
    val type: String = "orderbook",
    val figi: String,
    val bids: List<List<Double>>,
    val asks: List<List<Double>>
)

class TinkoffWsClient(
    private val token: String,
    private val connector: WsConnector,
    private val publisher: Publisher,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    suspend fun run(secids: List<String>) {
        val backoff = Backoff()
        while (scope.isActive) {
            try {
                val session = connector.connect(token)
                backoff.reset()
                session.send(json.encodeToString(SubscribeRequest(quotes = secids, orderbooks = secids)))
                val heartbeat = scope.launch {
                    while (isActive) {
                        delay(10_000)
                        session.send("""{"type":"ping"}""")
                    }
                }
                while (true) {
                    val text = session.receive() ?: break
                    handleMessage(text)
                }
                heartbeat.cancel()
                session.close()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                delay(backoff.nextDelay())
            }
        }
    }

    private suspend fun handleMessage(text: String) {
        val obj = json.decodeFromString<JsonObject>(text)
        when (obj["type"]?.jsonPrimitive?.content) {
            "last_price" -> {
                val event = json.decodeFromString<LastPriceEvent>(text)
                publisher.publish("prices:${event.figi}", event.price.toString())
            }
            "orderbook" -> {
                val event = json.decodeFromString<OrderBookEvent>(text)
                publisher.publish("orderbook:${event.figi}", json.encodeToString(event))
            }
        }
    }

    private class Backoff(
        private val base: Long = 1_000L,
        private val max: Long = 60_000L
    ) {
        private var current = base
        fun reset() { current = base }
        suspend fun nextDelay(): Long {
            val d = current
            current = (current * 2).coerceAtMost(max)
            return d
        }
    }
}

