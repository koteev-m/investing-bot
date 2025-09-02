package pl.bot.core.alert

/**
 * Alert rule for securities or crypto.
 */

enum class Direction { ABOVE, BELOW }

data class Alert(
    val userId: Long,
    val chatId: Long,
    val symbol: String,
    val isCrypto: Boolean,
    val direction: Direction,
    val target: Double,
    val hysteresisBps: Int = 20,
    val permanent: Boolean = false,
    var triggered: Boolean = false,
)

