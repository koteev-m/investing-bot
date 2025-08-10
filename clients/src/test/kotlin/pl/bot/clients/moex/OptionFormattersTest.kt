package pl.bot.clients.moex

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OptionFormattersTest {
    @Test
    fun `formats greeks and breakevens`() {
        assertEquals("Δ 0.1234", 0.1234.formatDelta())
        assertEquals("Γ -0.5678", (-0.5678).formatGamma())
        assertEquals("Θ 0.1000", 0.1.formatTheta())
        assertEquals("Vega 0.9000", 0.9.formatVega())
        assertEquals("IV 25.00%", 0.25.formatIv())
        assertEquals("BE 100.00 / 120.00", formatBreakEven(100.0, 120.0))
    }
}

