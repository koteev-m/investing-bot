package pl.bot.infra

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Config(
    @SerialName("TELEGRAM_TOKEN")
    val telegramToken: String,
    @SerialName("TELEGRAM_WEBHOOK_URL")
    val telegramWebhookUrl: String? = null,
    @SerialName("TELEGRAM_BOT_NAME")
    val telegramBotName: String? = null,
    @SerialName("DATABASE_URL")
    val databaseUrl: String? = null,
    @SerialName("REDIS_URL")
    val redisUrl: String? = null,
    @SerialName("SENTRY_DSN")
    val sentryDsn: String? = null,
    @SerialName("TINKOFF_TOKEN")
    val tinkoffToken: String? = null,
    @SerialName("WHALE_ALERT_KEY")
    val whaleAlertKey: String? = null,
) {
    companion object {
        fun fromEnv(): Config = Config(
            telegramToken = getEnv("TELEGRAM_TOKEN"),
            telegramWebhookUrl = System.getenv("TELEGRAM_WEBHOOK_URL"),
            telegramBotName = System.getenv("TELEGRAM_BOT_NAME"),
            databaseUrl = System.getenv("DATABASE_URL"),
            redisUrl = System.getenv("REDIS_URL"),
            sentryDsn = System.getenv("SENTRY_DSN"),
            tinkoffToken = System.getenv("TINKOFF_TOKEN"),
            whaleAlertKey = System.getenv("WHALE_ALERT_KEY"),
        )

        private fun getEnv(name: String): String =
            System.getenv(name) ?: error("Environment variable $name not found")
    }
}
