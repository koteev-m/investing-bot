package pl.bot.data.alert

import pl.bot.core.alert.Alert
import pl.bot.core.alert.AlertRepository

/**
 * Simple in-memory implementation of [AlertRepository].
 */
class InMemoryAlertRepository : AlertRepository {
    private val alerts = mutableListOf<Alert>()

    override fun save(alert: Alert) { alerts += alert }

    override fun all(): List<Alert> = alerts.toList()

    override fun remove(alert: Alert) { alerts.remove(alert) }
}

