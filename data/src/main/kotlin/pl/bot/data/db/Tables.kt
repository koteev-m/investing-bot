package pl.bot.data.db

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime

object Users : LongIdTable("users") {
    val name: Column<String> = varchar("name", 255)
    val email: Column<String> = varchar("email", 255).uniqueIndex()
}

object RateLimits : LongIdTable("rate_limits") {
    val userId = reference("user_id", Users)
    val quotaName = varchar("quota_name", 255)
    val date = date("date")
    val quotaLimit = integer("quota_limit")
    val used = integer("used")
    init { index("idx_rate_limits_user_quota_date", false, userId, quotaName, date) }
}

object Alerts : LongIdTable("alerts") {
    val userId = reference("user_id", Users)
    val message = varchar("message", 255)
    val active = bool("active")
    init { index("idx_alerts_user_active", false, userId, active) }
}

object CryptoAlerts : LongIdTable("crypto_alerts") {
    val userId = reference("user_id", Users)
    val symbol = varchar("symbol", 50)
    val active = bool("active")
    init { index("idx_crypto_alerts_user_active", false, userId, active) }
}

object Portfolios : LongIdTable("portfolios") {
    val userId = reference("user_id", Users)
    val name = varchar("name", 255)
}

object Positions : LongIdTable("positions") {
    val portfolioId = reference("portfolio_id", Portfolios)
    val asset = varchar("asset", 255)
    val quantity = double("quantity")
}

object Trades : LongIdTable("trades") {
    val positionId = reference("position_id", Positions)
    val price = double("price")
    val quantity = double("quantity")
    val executedAt = datetime("executed_at")
}

object Payments : LongIdTable("payments") {
    val userId = reference("user_id", Users)
    val amount = double("amount")
    val createdAt = datetime("created_at")
}

object Referrals : LongIdTable("referrals") {
    val userId = reference("user_id", Users)
    val referredUserId = reference("referred_user_id", Users)
    val code = varchar("code", 255)
}
