package pl.bot.bot

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FUNCTION

// Subscription plans
enum class Plan { FREE, PRO, PREMIUM }

@Target(FUNCTION)
@Retention(RUNTIME)
annotation class RequiresPlan(val plan: Plan)

@Target(FUNCTION)
@Retention(RUNTIME)
annotation class Quota(val name: String, val dailyLimit: Int)

class RateLimiter {
    private val usage = mutableMapOf<Pair<Long, String>, Pair<java.time.LocalDate, Int>>()

    fun checkAndIncrement(userId: Long, quotaName: String, limit: Int): Boolean {
        val today = java.time.LocalDate.now()
        val key = userId to quotaName
        val (date, count) = usage[key] ?: today to 0
        val current = if (date == today) count else 0
        if (current >= limit) return false
        usage[key] = today to (current + 1)
        return true
    }
}
