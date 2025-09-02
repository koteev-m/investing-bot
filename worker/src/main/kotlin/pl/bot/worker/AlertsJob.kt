package pl.bot.worker

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendMessage
import kotlinx.coroutines.runBlocking
import org.quartz.Job
import org.quartz.JobExecutionContext
import pl.bot.core.alert.Alert
import pl.bot.core.alert.AlertRepository
import pl.bot.core.alert.Direction
import pl.bot.core.crypto.CryptoQuoteService
import pl.bot.core.quote.QuoteService

/**
 * Quartz job that checks alert rules and sends Telegram messages.
 */
class AlertsJob(
    private val repository: AlertRepository,
    private val quoteService: QuoteService,
    private val cryptoQuoteService: CryptoQuoteService,
    private val bot: TelegramBot,
) : Job {
    override fun execute(context: JobExecutionContext?) {
        repository.all().forEach { process(it) }
    }

    private fun process(alert: Alert) {
        val price = runBlocking {
            if (alert.isCrypto) {
                cryptoQuoteService.getQuote(alert.symbol)?.price
            } else {
                quoteService.getQuote(alert.symbol)?.price
            }
        } ?: return

        if (!alert.triggered) {
            val triggered = when (alert.direction) {
                Direction.ABOVE -> price >= alert.target
                Direction.BELOW -> price <= alert.target
            }
            if (triggered) {
                bot.execute(
                    SendMessage(alert.chatId, "${alert.symbol} ${symbol(alert.direction)} ${alert.target} -> $price"),
                )
                alert.triggered = true
                if (!alert.permanent) {
                    repository.remove(alert)
                }
            }
        } else if (alert.permanent) {
            val reset = when (alert.direction) {
                Direction.ABOVE -> price <= alert.target * (1 - alert.hysteresisBps / 10000.0)
                Direction.BELOW -> price >= alert.target * (1 + alert.hysteresisBps / 10000.0)
            }
            if (reset) alert.triggered = false
        }
    }

    private fun symbol(dir: Direction) = if (dir == Direction.ABOVE) ">" else "<"
}

