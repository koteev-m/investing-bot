package pl.bot.clients.cache

import io.lettuce.core.RedisClient
import io.lettuce.core.api.sync.RedisCommands
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface Cache {
    suspend fun get(key: String): String?
    suspend fun set(key: String, value: String, ttlSeconds: Long)
}

class RedisCache(redisUri: String) : Cache {
    private val client = RedisClient.create(redisUri)
    private val connection = client.connect()
    private val commands: RedisCommands<String, String> = connection.sync()

    override suspend fun get(key: String): String? = withContext(Dispatchers.IO) { commands.get(key) }

    override suspend fun set(key: String, value: String, ttlSeconds: Long) {
        withContext(Dispatchers.IO) { commands.setex(key, ttlSeconds, value) }
    }
}

class InMemoryCache : Cache {
    private data class Entry(val value: String, val expiresAt: Long)
    private val map = ConcurrentHashMap<String, Entry>()
    private val mutex = Mutex()

    override suspend fun get(key: String): String? {
        val now = System.currentTimeMillis()
        val entry = map[key]
        return if (entry != null && entry.expiresAt > now) entry.value else null
    }

    override suspend fun set(key: String, value: String, ttlSeconds: Long) {
        val expiresAt = System.currentTimeMillis() + ttlSeconds * 1000
        mutex.withLock { map[key] = Entry(value, expiresAt) }
    }
}
