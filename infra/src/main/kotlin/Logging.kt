package pl.bot.infra

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

object Logging {
    fun getLogger(forClass: Class<*>): Logger = LoggerFactory.getLogger(forClass)

    inline fun <T> withFields(vararg fields: Pair<String, String>, block: () -> T): T {
        fields.forEach { (k, v) -> MDC.put(k, v) }
        return try {
            block()
        } finally {
            fields.forEach { (k, _) -> MDC.remove(k) }
        }
    }
}
