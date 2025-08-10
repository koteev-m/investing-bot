package pl.bot.bot

import com.pengrad.telegrambot.utility.BotUtils
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.BaseRequest
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.BaseResponse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class FakeTelegramBot : TelegramBot("TEST") {
    var lastRequest: BaseRequest<*, *>? = null
    override fun <T : BaseRequest<T, R>, R : BaseResponse> execute(request: BaseRequest<T, R>): R {
        lastRequest = request
        val json = "{" + "\"ok\":true" + "}"
        return BotUtils.fromJson(json, request.responseType)
    }
}

class WebhookRoutingTest {
    @Test
    fun `parses update and routes start command`() {
        val bot = FakeTelegramBot()
        val json = """
            {"update_id":1,
             "message":{"message_id":1,
                        "from":{"id":1,"is_bot":false,"first_name":"A"},
                        "chat":{"id":1,"type":"private"},
                        "date":0,
                        "text":"/start"}}
        """.trimIndent()
        val update = BotUtils.fromJson(json, com.pengrad.telegrambot.model.Update::class.java)
        processUpdate(bot, RateLimiter(), update)
        val request = bot.lastRequest as SendMessage
        val params = request.parameters
        assertEquals("Добро пожаловать!", params["text"])
    }
}
