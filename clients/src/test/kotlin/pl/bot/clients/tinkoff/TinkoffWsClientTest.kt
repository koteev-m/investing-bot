package pl.bot.clients.tinkoff

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.cancel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pl.bot.clients.redis.InMemoryPublisher

private class MockWsSession(private val messages: List<String>) : WsSession {
    private val iterator = messages.iterator()
    val sent = mutableListOf<String>()
    override suspend fun send(text: String) { sent.add(text) }
    override suspend fun receive(): String? = if (iterator.hasNext()) iterator.next() else null
    override suspend fun close() {}
}

private class MockWsConnector(private val sessions: MutableList<MockWsSession>) : WsConnector {
    override suspend fun connect(token: String): WsSession {
        if (sessions.isEmpty()) error("no sessions")
        return sessions.removeAt(0)
    }
}

class TinkoffWsClientTest {
    @Test
    fun `parses events and publishes`() = runBlocking {
        val session = MockWsSession(listOf(
            """{"type":"last_price","figi":"AAA","price":100.0}""",
            """{"type":"orderbook","figi":"AAA","bids":[[1.0,2.0]],"asks":[[3.0,4.0]]}"""
        ))
        val connector = MockWsConnector(mutableListOf(session))
        val publisher = InMemoryPublisher()
        val scope = CoroutineScope(Dispatchers.Default + Job())
        val client = TinkoffWsClient("token", connector, publisher, scope = scope)
        val job = scope.launch { client.run(listOf("AAA")) }
        delay(50)
        scope.cancel()
        job.join()
        assertEquals(listOf("100.0"), publisher.messages["prices:AAA"])
        assertEquals(1, publisher.messages["orderbook:AAA"]?.size)
        assertEquals(true, session.sent.first().contains("AAA"))
    }

    @Test
    fun `reconnects after close`() = runBlocking {
        val s1 = MockWsSession(listOf("""{"type":"last_price","figi":"AAA","price":100.0}"""))
        val s2 = MockWsSession(listOf("""{"type":"last_price","figi":"AAA","price":101.0}"""))
        val connector = MockWsConnector(mutableListOf(s1, s2))
        val publisher = InMemoryPublisher()
        val scope = CoroutineScope(Dispatchers.Default + Job())
        val client = TinkoffWsClient("token", connector, publisher, scope = scope)
        val job = scope.launch { client.run(listOf("AAA")) }
        delay(100)
        scope.cancel()
        job.join()
        assertEquals(listOf("100.0", "101.0"), publisher.messages["prices:AAA"])
        assertEquals(1, s1.sent.size)
        assertEquals(1, s2.sent.size)
    }
}

