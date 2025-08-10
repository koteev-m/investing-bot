package pl.bot.data.repository

import pl.bot.data.db.*
import java.time.LocalDate
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

data class User(val id: Long, val name: String, val email: String)

data class RateLimit(
    val id: Long,
    val userId: Long,
    val quotaName: String,
    val date: LocalDate,
    val quotaLimit: Int,
    val used: Int,
)

data class Alert(val id: Long, val userId: Long, val message: String, val active: Boolean)

data class CryptoAlert(val id: Long, val userId: Long, val symbol: String, val active: Boolean)

data class Portfolio(val id: Long, val userId: Long, val name: String)

data class Position(val id: Long, val portfolioId: Long, val asset: String, val quantity: Double)

data class Trade(
    val id: Long,
    val positionId: Long,
    val price: Double,
    val quantity: Double,
    val executedAt: LocalDateTime,
)

data class Payment(val id: Long, val userId: Long, val amount: Double, val createdAt: LocalDateTime)

data class Referral(val id: Long, val userId: Long, val referredUserId: Long, val code: String)

class UsersRepository {
    fun create(name: String, email: String): User = transaction {
        val id = Users.insertAndGetId {
            it[Users.name] = name
            it[Users.email] = email
        }.value
        User(id, name, email)
    }

    fun findById(id: Long): User? = transaction {
        Users.select { Users.id eq id }.map(::toUser).singleOrNull()
    }

    private fun toUser(row: ResultRow) =
        User(row[Users.id].value, row[Users.name], row[Users.email])
}

class RateLimitsRepository {
    fun create(userId: Long, quotaName: String, date: LocalDate, quotaLimit: Int, used: Int): RateLimit = transaction {
        val id = RateLimits.insertAndGetId {
            it[RateLimits.userId] = userId
            it[RateLimits.quotaName] = quotaName
            it[RateLimits.date] = date
            it[RateLimits.quotaLimit] = quotaLimit
            it[RateLimits.used] = used
        }.value
        RateLimit(id, userId, quotaName, date, quotaLimit, used)
    }

    fun findById(id: Long): RateLimit? = transaction {
        RateLimits.select { RateLimits.id eq id }.map(::toRateLimit).singleOrNull()
    }

    private fun toRateLimit(row: ResultRow) =
        RateLimit(
            row[RateLimits.id].value,
            row[RateLimits.userId].value,
            row[RateLimits.quotaName],
            row[RateLimits.date],
            row[RateLimits.quotaLimit],
            row[RateLimits.used],
        )
}

class AlertsRepository {
    fun create(userId: Long, message: String, active: Boolean): Alert = transaction {
        val id = Alerts.insertAndGetId {
            it[Alerts.userId] = userId
            it[Alerts.message] = message
            it[Alerts.active] = active
        }.value
        Alert(id, userId, message, active)
    }

    fun findById(id: Long): Alert? = transaction {
        Alerts.select { Alerts.id eq id }.map(::toAlert).singleOrNull()
    }

    private fun toAlert(row: ResultRow) =
        Alert(row[Alerts.id].value, row[Alerts.userId].value, row[Alerts.message], row[Alerts.active])
}

class CryptoAlertsRepository {
    fun create(userId: Long, symbol: String, active: Boolean): CryptoAlert = transaction {
        val id = CryptoAlerts.insertAndGetId {
            it[CryptoAlerts.userId] = userId
            it[CryptoAlerts.symbol] = symbol
            it[CryptoAlerts.active] = active
        }.value
        CryptoAlert(id, userId, symbol, active)
    }

    fun findById(id: Long): CryptoAlert? = transaction {
        CryptoAlerts.select { CryptoAlerts.id eq id }.map(::toCryptoAlert).singleOrNull()
    }

    private fun toCryptoAlert(row: ResultRow) =
        CryptoAlert(
            row[CryptoAlerts.id].value,
            row[CryptoAlerts.userId].value,
            row[CryptoAlerts.symbol],
            row[CryptoAlerts.active],
        )
}

class PortfoliosRepository {
    fun create(userId: Long, name: String): Portfolio = transaction {
        val id = Portfolios.insertAndGetId {
            it[Portfolios.userId] = userId
            it[Portfolios.name] = name
        }.value
        Portfolio(id, userId, name)
    }

    fun findById(id: Long): Portfolio? = transaction {
        Portfolios.select { Portfolios.id eq id }.map(::toPortfolio).singleOrNull()
    }

    private fun toPortfolio(row: ResultRow) =
        Portfolio(row[Portfolios.id].value, row[Portfolios.userId].value, row[Portfolios.name])
}

class PositionsRepository {
    fun create(portfolioId: Long, asset: String, quantity: Double): Position = transaction {
        val id = Positions.insertAndGetId {
            it[Positions.portfolioId] = portfolioId
            it[Positions.asset] = asset
            it[Positions.quantity] = quantity
        }.value
        Position(id, portfolioId, asset, quantity)
    }

    fun findById(id: Long): Position? = transaction {
        Positions.select { Positions.id eq id }.map(::toPosition).singleOrNull()
    }

    private fun toPosition(row: ResultRow) =
        Position(
            row[Positions.id].value,
            row[Positions.portfolioId].value,
            row[Positions.asset],
            row[Positions.quantity],
        )
}

class TradesRepository {
    fun create(positionId: Long, price: Double, quantity: Double, executedAt: LocalDateTime): Trade = transaction {
        val id = Trades.insertAndGetId {
            it[Trades.positionId] = positionId
            it[Trades.price] = price
            it[Trades.quantity] = quantity
            it[Trades.executedAt] = executedAt
        }.value
        Trade(id, positionId, price, quantity, executedAt)
    }

    fun findById(id: Long): Trade? = transaction {
        Trades.select { Trades.id eq id }.map(::toTrade).singleOrNull()
    }

    private fun toTrade(row: ResultRow) =
        Trade(
            row[Trades.id].value,
            row[Trades.positionId].value,
            row[Trades.price],
            row[Trades.quantity],
            row[Trades.executedAt],
        )
}

class PaymentsRepository {
    fun create(userId: Long, amount: Double, createdAt: LocalDateTime): Payment = transaction {
        val id = Payments.insertAndGetId {
            it[Payments.userId] = userId
            it[Payments.amount] = amount
            it[Payments.createdAt] = createdAt
        }.value
        Payment(id, userId, amount, createdAt)
    }

    fun findById(id: Long): Payment? = transaction {
        Payments.select { Payments.id eq id }.map(::toPayment).singleOrNull()
    }

    private fun toPayment(row: ResultRow) =
        Payment(
            row[Payments.id].value,
            row[Payments.userId].value,
            row[Payments.amount],
            row[Payments.createdAt],
        )
}

class ReferralsRepository {
    fun create(userId: Long, referredUserId: Long, code: String): Referral = transaction {
        val id = Referrals.insertAndGetId {
            it[Referrals.userId] = userId
            it[Referrals.referredUserId] = referredUserId
            it[Referrals.code] = code
        }.value
        Referral(id, userId, referredUserId, code)
    }

    fun findById(id: Long): Referral? = transaction {
        Referrals.select { Referrals.id eq id }.map(::toReferral).singleOrNull()
    }

    private fun toReferral(row: ResultRow) =
        Referral(
            row[Referrals.id].value,
            row[Referrals.userId].value,
            row[Referrals.referredUserId].value,
            row[Referrals.code],
        )
}
