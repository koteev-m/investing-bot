package pl.bot.clients.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.api.async.RedisAsyncCommands
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext

interface Publisher {
    suspend fun publish(channel: String, message: String)
}

class RedisPublisher(redisUri: String) : Publisher {
    private val client = RedisClient.create(redisUri)
    private val connection = client.connect()
    private val commands: RedisAsyncCommands<String, String> = connection.async()

    override suspend fun publish(channel: String, message: String) {
        withContext(Dispatchers.IO) { commands.publish(channel, message).toCompletableFuture().await() }
    }
}

class InMemoryPublisher : Publisher {
    val messages = mutableMapOf<String, MutableList<String>>()
    override suspend fun publish(channel: String, message: String) {
        messages.getOrPut(channel) { mutableListOf() }.add(message)
    }
}

