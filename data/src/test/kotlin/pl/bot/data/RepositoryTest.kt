package pl.bot.data

import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource
import org.flywaydb.core.Flyway
import org.h2.jdbcx.JdbcDataSource
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import pl.bot.data.repository.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RepositoryTest {
    private lateinit var dataSource: DataSource

    @BeforeAll
    fun setup() {
        dataSource = JdbcDataSource().apply {
            setURL("jdbc:h2:mem:test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;")
        }
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate()
        Database.connect(dataSource)
    }

    @Test
    fun `repositories basic operations`() {
        val usersRepo = UsersRepository()
        val rateLimitsRepo = RateLimitsRepository()
        val alertsRepo = AlertsRepository()
        val cryptoAlertsRepo = CryptoAlertsRepository()
        val portfoliosRepo = PortfoliosRepository()
        val positionsRepo = PositionsRepository()
        val tradesRepo = TradesRepository()
        val paymentsRepo = PaymentsRepository()
        val referralsRepo = ReferralsRepository()

        val user = usersRepo.create("Alice", "alice@example.com")
        assertEquals(user, usersRepo.findById(user.id))

        val rateLimit = rateLimitsRepo.create(user.id, "daily", LocalDate.now(), 10, 1)
        assertEquals(rateLimit, rateLimitsRepo.findById(rateLimit.id))

        val alert = alertsRepo.create(user.id, "hello", true)
        assertEquals(alert, alertsRepo.findById(alert.id))

        val cryptoAlert = cryptoAlertsRepo.create(user.id, "BTC", false)
        assertEquals(cryptoAlert, cryptoAlertsRepo.findById(cryptoAlert.id))

        val portfolio = portfoliosRepo.create(user.id, "Main")
        assertEquals(portfolio, portfoliosRepo.findById(portfolio.id))

        val position = positionsRepo.create(portfolio.id, "AAPL", 5.0)
        assertEquals(position, positionsRepo.findById(position.id))

        val tradeTime = LocalDateTime.now().withNano(0)
        val trade = tradesRepo.create(position.id, 150.0, 2.0, tradeTime)
        assertEquals(trade, tradesRepo.findById(trade.id))

        val paymentTime = LocalDateTime.now().withNano(0)
        val payment = paymentsRepo.create(user.id, 100.0, paymentTime)
        assertEquals(payment, paymentsRepo.findById(payment.id))

        val referred = usersRepo.create("Bob", "bob@example.com")
        val referral = referralsRepo.create(user.id, referred.id, "CODE")
        assertEquals(referral, referralsRepo.findById(referral.id))
    }
}
