package pl.bot.clients.moex

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val greekFormat = DecimalFormat("0.0000").apply { decimalFormatSymbols = DecimalFormatSymbols(Locale.US) }
private val ivFormat = DecimalFormat("0.00%").apply { decimalFormatSymbols = DecimalFormatSymbols(Locale.US) }
private val priceFormat = DecimalFormat("0.00").apply { decimalFormatSymbols = DecimalFormatSymbols(Locale.US) }

fun Double.formatDelta(): String = "Δ ${greekFormat.format(this)}"
fun Double.formatGamma(): String = "Γ ${greekFormat.format(this)}"
fun Double.formatTheta(): String = "Θ ${greekFormat.format(this)}"
fun Double.formatVega(): String = "Vega ${greekFormat.format(this)}"
fun Double.formatIv(): String = "IV ${ivFormat.format(this)}"
fun formatBreakEven(low: Double, high: Double): String =
    "BE ${priceFormat.format(low)} / ${priceFormat.format(high)}"

