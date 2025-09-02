package pl.bot.core.alert

/** Repository of alert rules. */
interface AlertRepository {
    fun save(alert: Alert)
    fun all(): List<Alert>
    fun remove(alert: Alert)
}

